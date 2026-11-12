package com.darakav.barid

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Keeps Barid up to date from its GitHub releases (README "Updates"). A background check every 12 hours
 * downloads a newer release's APK, checks that it is Barid, newer and signed with the installed app's
 * key, and installs it: silently on Android 12+ once the owner allowed Barid to install apps, otherwise
 * with a notification and a Home card. Where the session install is refused (some Xiaomi HyperOS
 * builds replace Android's installer), the APK opens in the phone's installer screen instead, as when
 * it is opened from a file manager. Off when a store (F-Droid, Google Play…) installed Barid: the
 * store updates it. Only GitHub is contacted; nothing about the phone or its messages is sent.
 */
object Updates {
    const val LATEST = "https://api.github.com/repos/ali-mirsalari/barid/releases/latest"
    const val ACTION_INSTALL = "com.darakav.barid.INSTALL_UPDATE"
    private const val CHANNEL = "updates"
    private const val NOTIFICATION = 1

    /** App stores and update managers that keep Barid current themselves. */
    private val STORES = setOf(
        "org.fdroid.fdroid", "org.fdroid.basic", "com.looker.droidify", "com.machiav3lli.fdroid", "app.flicky",
        "dev.imranr.obtainium", "com.aurora.store", "com.android.vending", "com.farsitel.bazaar", "ir.mservices.market",
    )

    /** Set while an activity is visible, so the install confirmation can open directly. */
    @Volatile var foreground = false

    data class Release(val version: String, val apkUrl: String, val size: Long)

    sealed interface Outcome {
        data object UpToDate : Outcome
        data class Ready(val version: String) : Outcome
        data object Failed : Outcome
        data object Off : Outcome
    }

    /** GitHub's latest-release JSON: the version from a "v1.2.3" tag and its "barid-1.2.3.apk" asset. */
    fun parse(json: String): Release? {
        val o = JSONObject(json)
        if (o.optBoolean("draft") || o.optBoolean("prerelease")) return null
        val version = o.optString("tag_name").removePrefix("v")
        if (!Regex("\\d+(\\.\\d+){1,3}").matches(version)) return null
        val assets = o.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val url = a.optString("browser_download_url")
            if (a.optString("name") == "barid-$version.apk" && url.startsWith("https://")) return Release(version, url, a.optLong("size"))
        }
        return null
    }

    /** True when [candidate] ("1.0.10") is a later version than [current] ("1.0.9"). */
    fun newer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** The store that installed Barid, if one did; null for an APK installed by hand. */
    fun store(context: Context): String? {
        val pm = context.packageManager
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(context.packageName).installingPackageName
            else @Suppress("DEPRECATION") pm.getInstallerPackageName(context.packageName)
        }.getOrNull()
        return installer?.takeIf { it in STORES }
    }

    fun enabled(context: Context) = Settings(context).autoUpdate && store(context) == null

    /** The owner allowed Barid to install apps ("Install unknown apps"), needed for silent updates. */
    fun installAllowed(context: Context) = context.packageManager.canRequestPackageInstalls()

    fun notificationsAllowed(context: Context) =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun file(context: Context, version: String) = File(File(context.filesDir, "updates"), "barid-$version.apk")

    /**
     * Checks GitHub, downloads a newer release and starts its installation. [install] false only
     * prepares it (the owner taps Install).
     */
    fun run(context: Context, install: Boolean = true): Outcome {
        val app = context.applicationContext
        if (store(app) != null) return Outcome.Off
        val settings = Settings(app)
        val current = BuildConfig.VERSION_NAME
        // A finished update: forget the old file and notice.
        settings.updateReady?.let { if (!newer(it, current)) clear(app) }
        val release = runCatching { parse(get(LATEST)) }.getOrNull()
        settings.updateCheckedAt = System.currentTimeMillis()
        if (release == null) return Outcome.Failed.also { settings.updateError = "check" }
        if (!newer(release.version, current)) return Outcome.UpToDate.also { clear(app); settings.updateError = null }
        val apk = file(app, release.version)
        if (!(apk.exists() && verified(app, apk))) {
            apk.parentFile?.listFiles()?.forEach { it.delete() }
            val ok = runCatching { download(release.apkUrl, apk, release.size) }.isSuccess && verified(app, apk)
            if (!ok) return Outcome.Failed.also { apk.delete(); settings.updateError = "download" }
        }
        settings.updateReady = release.version
        settings.updateError = null
        if (install) install(app)
        return Outcome.Ready(release.version)
    }

    /**
     * Starts installing the downloaded update; Android asks the owner only when it must. After a refused
     * session install, the owner using the app gets the installer screen ([openInstaller]); in the
     * background the notification waits for them.
     */
    fun install(context: Context): Boolean {
        val app = context.applicationContext
        val settings = Settings(app)
        val version = settings.updateReady ?: return false
        val apk = file(app, version)
        if (!apk.exists() || !verified(app, apk)) return false
        if (settings.updateFallback) {
            if (!foreground) { notifyReady(app); return true }
            return openInstaller(app, apk)
        }
        if (!installSession(app, apk)) {
            settings.updateFallback = true
            return if (foreground) openInstaller(app, apk) else true.also { notifyReady(app) }
        }
        return true
    }

    /** The phone's own installer screen for the downloaded APK, which every Android build allows. */
    private fun openInstaller(context: Context, apk: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.isSuccess

    /** Opens the installer screen after a refused session install, from the activity the owner is using. */
    fun installFallback(context: Context): Boolean {
        val app = context.applicationContext
        val apk = Settings(app).updateReady?.let { file(app, it) } ?: return false
        return apk.exists() && verified(app, apk) && openInstaller(context, apk)
    }

    private fun installSession(app: Context, apk: File): Boolean {
        val installer = app.packageManager.packageInstaller
        installer.mySessions.forEach { runCatching { installer.abandonSession(it.sessionId) } }
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.packageName)
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        return runCatching {
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                session.openWrite("barid.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val status = Intent(app, UpdateReceiver::class.java).setAction(UpdateReceiver.STATUS)
                // Mutable: the installer adds the status and the confirmation intent.
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                session.commit(PendingIntent.getBroadcast(app, id, status, flags).intentSender)
            }
            true
        }.getOrDefault(false)
    }

    private fun clear(context: Context) {
        val settings = Settings(context)
        settings.updateReady = null
        settings.updateFallback = false
        File(context.filesDir, "updates").listFiles()?.forEach { it.delete() }
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION)
    }

    /** The APK is Barid, newer than this build and signed with the installed app's key. */
    private fun verified(context: Context, apk: File): Boolean {
        val pm = context.packageManager
        val archive = archiveInfo(pm, apk.path) ?: return false
        if (archive.packageName != context.packageName) return false
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        if (code <= BuildConfig.VERSION_CODE) return false
        val installed = signers(installedInfo(pm, context.packageName))
        val offered = signers(archive)
        return installed.isNotEmpty() && installed == offered
    }

    @SuppressLint("PackageManagerGetSignatures") // compared as a whole set with the installed app's signers
    private fun archiveInfo(pm: PackageManager, path: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= 28) pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
        else @Suppress("DEPRECATION") pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES)

    @SuppressLint("PackageManagerGetSignatures")
    private fun installedInfo(pm: PackageManager, name: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= 28) pm.getPackageInfo(name, PackageManager.GET_SIGNING_CERTIFICATES)
        else @Suppress("DEPRECATION") pm.getPackageInfo(name, PackageManager.GET_SIGNATURES)

    private fun signers(info: PackageInfo?): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            val signing = info?.signingInfo ?: return emptySet()
            if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory
        } else @Suppress("DEPRECATION") info?.signatures
        return signatures.orEmpty().mapNotNull { it?.toByteArray() }.map { sha256(it) }.toSet()
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 20_000
        readTimeout = 60_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Barid/${BuildConfig.VERSION_NAME}")
    }

    private fun get(url: String): String {
        val c = open(url)
        c.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun download(url: String, dest: File, size: Long) {
        dest.parentFile?.mkdirs()
        val part = File(dest.path + ".part")
        val c = open(url)
        try {
            if (c.responseCode != 200 || !c.url.protocol.equals("https", true)) error("HTTP ${c.responseCode}")
            c.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
        } finally {
            c.disconnect()
        }
        if (size > 0 && part.length() != size) { part.delete(); error("size") }
        if (!part.renameTo(dest)) error("rename")
    }

    /** "Barid 1.2.3 is ready": opens the app, which starts the installation. */
    fun notifyReady(context: Context) {
        val version = Settings(context).updateReady ?: return
        if (!notificationsAllowed(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.update_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = Intent(context, MainActivity::class.java).setAction(ACTION_INSTALL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val tap = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.update_ready_title, version))
            .setContentText(context.getString(R.string.update_ready_notification))
            .setContentIntent(tap)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        runCatching { manager.notify(NOTIFICATION, notification) }
    }
}

/** Installation results: opens Android's confirmation when the owner is using Barid, else notifies. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != STATUS) return
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null && Updates.foreground) {
                    runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                } else Updates.notifyReady(context)
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            // Refused or cancelled (some HyperOS builds return either for their own installer): from
            // now on the installer screen is used, which every phone allows.
            else -> {
                Settings(context).updateFallback = true
                if (Updates.foreground) Updates.installFallback(context) else Updates.notifyReady(context)
            }
        }
    }

    companion object {
        const val STATUS = "com.darakav.barid.UPDATE_STATUS"
    }
}

/** The 12-hourly update check ([Updates.run]); scheduled at every app start, survives reboots. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!Updates.enabled(applicationContext)) return Result.success()
        Updates.run(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "barid-updates"

        fun schedule(context: Context) {
            val manager = WorkManager.getInstance(context)
            if (!Updates.enabled(context)) { manager.cancelUniqueWork(NAME); return }
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            manager.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
