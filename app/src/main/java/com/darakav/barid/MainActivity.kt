package com.darakav.barid

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.darakav.barid.ui.Snapshot
import com.darakav.barid.ui.components.Backdrop
import com.darakav.barid.ui.components.LocalHazeState
import com.darakav.barid.ui.components.glass
import com.darakav.barid.ui.screens.AboutScreen
import com.darakav.barid.ui.screens.ActivityScreen
import com.darakav.barid.ui.screens.ConnectScreen
import com.darakav.barid.ui.screens.ConfirmServer
import com.darakav.barid.ui.screens.HomeScreen
import com.darakav.barid.ui.screens.ServerScreen
import com.darakav.barid.ui.screens.sampleMessage
import com.darakav.barid.ui.screens.SettingsScreen
import com.darakav.barid.ui.screens.WelcomeScreen
import com.darakav.barid.ui.theme.Barid
import com.darakav.barid.ui.theme.BaridTheme
import com.darakav.barid.ui.theme.Palette
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen { WELCOME, CONNECT, SERVER, HOME, SETTINGS, ABOUT, ACTIVITY }

/** One activity, Compose screens. Messages are forwarded by [SmsReceiver] and [ForwardWorker], not here. */
class MainActivity : AppCompatActivity() {
    private val settings by lazy { Settings(this) }
    private var snapshot by mutableStateOf(Snapshot.Empty)
    private var deepLink by mutableStateOf<Pairing?>(null)
    private var deepLinkInvalid by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        snapshot = Snapshot.load(this)
        if (savedInstanceState == null) handle(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    refresh()
                    delay(2_000)
                }
            }
        }
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "darakav" && data.scheme != "barid") return
        val pairing = Pairing.parse(data.toString())
        if (pairing == null) deepLinkInvalid = true else deepLink = pairing
    }

    private suspend fun refresh() {
        snapshot = withContext(Dispatchers.IO) { Snapshot.load(applicationContext) }
    }

    @Composable
    private fun App() {
        var themeMode by remember { mutableStateOf(settings.themeMode) }
        var palette by remember { mutableStateOf(Palette.of(settings.palette)) }
        var solid by remember { mutableStateOf(settings.reduceTransparency) }
        val language = remember { AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',').substringBefore('-') }
        var stackNames by rememberSaveable { mutableStateOf(if (snapshot.pairing == null) "WELCOME" else "HOME") }
        val stack = stackNames.split(',').map { Screen.valueOf(it) }
        fun go(screen: Screen) { stackNames = screen.name }
        fun push(screen: Screen) { stackNames = "$stackNames,${screen.name}" }
        fun pop() { if (stack.size > 1) stackNames = stack.dropLast(1).joinToString(",") { it.name } }
        val screen = stack.last()

        var message by remember { mutableStateOf<String?>(null) }
        var importing by remember { mutableStateOf(false) }
        var sending by remember { mutableStateOf(false) }
        var testing by remember { mutableStateOf(false) }
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
        val scope = rememberCoroutineScope()
        val haptics = LocalHapticFeedback.current

        fun test(afterPairing: Boolean = false) {
            val pairing = settings.pairing ?: return
            testing = true
            scope.launch {
                val sample = sampleMessage(getString(R.string.server_test_message))
                val result = withContext(Dispatchers.IO) { Forwarder.check(pairing, sample) }
                if (result == Forwarder.Result.Rejected) settings.rejected = true
                message = getString(
                    when (result) {
                        Forwarder.Result.Ok -> if (afterPairing) R.string.paired_ok else R.string.test_ok
                        Forwarder.Result.Rejected -> R.string.test_rejected
                        else -> R.string.test_failed
                    },
                )
                testing = false
                refresh()
            }
        }

        // Delivers from the app as well as in the background, since some phones (HyperOS, MIUI) hold
        // background work back for a long time. Quiet unless the owner asked.
        fun sendNow(report: Boolean) {
            if (sending) return
            sending = true
            scope.launch {
                val outcome = Delivery.run(applicationContext)
                if (report) message = when (outcome) {
                    is Delivery.Outcome.Done ->
                        if (outcome.sent > 0) resources.getQuantityString(R.plurals.sent_now, outcome.sent, outcome.sent) else getString(R.string.nothing_waiting)
                    Delivery.Outcome.Rejected -> getString(R.string.test_rejected)
                    is Delivery.Outcome.Failed -> getString(R.string.send_failed)
                }
                if (outcome is Delivery.Outcome.Failed) ForwardWorker.schedule(applicationContext)
                sending = false
                refresh()
            }
        }
        // While the app is open, anything waiting is sent right away (and retried each minute).
        LaunchedEffect(Unit) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    val waiting = withContext(Dispatchers.IO) { MessageQueue(applicationContext).size() }
                    val due = System.currentTimeMillis() - settings.lastAttemptAt > 60_000 || settings.lastError == null
                    if (waiting > 0 && !settings.rejected && due) sendNow(report = false)
                    delay(5_000)
                }
            }
        }

        val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted && !shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS)) openAppSettings()
            scope.launch { refresh() }
        }
        fun runImport() {
            importing = true
            scope.launch {
                val count = withContext(Dispatchers.IO) { runCatching { Backfill.run(applicationContext) }.getOrDefault(0) }
                message = if (count > 0) resources.getQuantityString(R.plurals.import_done, count, count) else getString(R.string.import_none)
                importing = false
                refresh()
                if (count > 0) sendNow(report = false)
            }
        }
        val readSmsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) runImport() else message = getString(R.string.import_needs_permission)
        }

        // A code for any server other than Darakav's is confirmed first: a QR code alone must never
        // redirect bank messages somewhere the owner did not choose.
        var pending by remember { mutableStateOf<Pairing?>(null) }

        fun paired(pairing: Pairing, confirmed: Boolean = false) {
            if (!confirmed && !pairing.trusted) { pending = pairing; return }
            pending = null
            // Repeats are remembered per server: a different server may receive everything again.
            if (settings.pairing?.url != pairing.url) Seen(applicationContext).clear()
            settings.pairing = pairing
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            ForwardWorker.schedule(applicationContext)
            go(Screen.HOME)
            scope.launch {
                refresh()
                if (!Snapshot.granted(applicationContext, Manifest.permission.RECEIVE_SMS)) smsPermission.launch(Manifest.permission.RECEIVE_SMS)
            }
            test(afterPairing = true)
        }
        LaunchedEffect(deepLink) { deepLink?.let { deepLink = null; paired(it) } }
        LaunchedEffect(deepLinkInvalid) { if (deepLinkInvalid) { deepLinkInvalid = false; message = getString(R.string.invalid_code) } }

        BackHandler(enabled = stack.size > 1) { pop() }

        BaridTheme(themeMode, palette, solid) {
            val dark = Barid.colors.dark
            val lightBars = !dark && screen != Screen.CONNECT
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
            val haze = remember { HazeState() }
            CompositionLocalProvider(LocalHazeState provides if (Barid.colors.solid) null else haze) {
                Box(Modifier.fillMaxSize()) {
                    Backdrop(haze)
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = { (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 }) togetherWith fadeOut(tween(140)) },
                        label = "screen",
                    ) { target ->
                        when (target) {
                            Screen.WELCOME -> WelcomeScreen(onConnect = { push(Screen.CONNECT) })
                            Screen.CONNECT -> ConnectScreen(
                                canGoBack = stack.size > 1, confirming = pending != null,
                                onBack = { pop() }, onOtherServer = { push(Screen.SERVER) }, onPaired = { paired(it) },
                            )
                            Screen.SERVER -> ServerScreen(snapshot.pairing, onBack = { pop() }, onConnect = { paired(it, confirmed = true) })
                            Screen.HOME -> HomeScreen(
                                state = snapshot, now = now, xiaomi = isXiaomi(), importing = importing, testing = testing, sending = sending,
                                onSendNow = { sendNow(report = true) },
                                onSettings = { push(Screen.SETTINGS) },
                                onSeeAll = { push(Screen.ACTIVITY) },
                                onAllowSms = { smsPermission.launch(Manifest.permission.RECEIVE_SMS) },
                                onBattery = { requestBatteryExemption() },
                                onAutostart = { openAutostart() },
                                onReconnect = { push(Screen.CONNECT) },
                                onImport = {
                                    if (Snapshot.granted(applicationContext, Manifest.permission.READ_SMS)) runImport()
                                    else readSmsPermission.launch(Manifest.permission.READ_SMS)
                                },
                                onTest = { test() },
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                state = snapshot, themeMode = themeMode, palette = palette, reduceTransparency = solid, language = language,
                                onBack = { pop() },
                                onThemeMode = { themeMode = it; settings.themeMode = it },
                                onPalette = { palette = it; settings.palette = it.id },
                                onReduceTransparency = { solid = it; settings.reduceTransparency = it },
                                onLanguage = { tag ->
                                    AppCompatDelegate.setApplicationLocales(
                                        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                                    )
                                },
                                onAbout = { push(Screen.ABOUT) },
                                onConnectOther = { push(Screen.CONNECT) },
                                onEditServer = { push(Screen.SERVER) },
                                onDisconnect = {
                                    settings.pairing = null
                                    MessageQueue(applicationContext).clear()
                                    ActivityLog(applicationContext).clear()
                                    Seen(applicationContext).clear()
                                    ForwardWorker.cancel(applicationContext)
                                    go(Screen.WELCOME)
                                    scope.launch { refresh() }
                                },
                            )
                            Screen.ABOUT -> AboutScreen(onBack = { pop() })
                            Screen.ACTIVITY -> ActivityScreen(snapshot.recent, onBack = { pop() })
                        }
                    }
                    Toast(message, Modifier.align(Alignment.BottomCenter)) { message = null }
                    pending?.let { ConfirmServer(it, onConfirm = { paired(it, confirmed = true) }, onCancel = { pending = null }) }
                }
            }
        }
    }

    @Composable
    private fun Toast(text: String?, modifier: Modifier, onDone: () -> Unit) {
        LaunchedEffect(text) { if (text != null) { delay(3_500); onDone() } }
        AnimatedVisibility(
            visible = text != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth(),
        ) {
            val c = Barid.colors
            Text(
                text.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = c.text,
                modifier = Modifier.glass(c, LocalHazeState.current, RoundedCornerShape(16.dp), strong = true)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
            )
        }
    }

    private fun isXiaomi() = Build.MANUFACTURER.lowercase().let { it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") }

    /** Asks Android to stop pausing Barid; without it some phones stop forwarding in the background. */
    @SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        val direct = Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        try {
            startActivity(direct)
        } catch (_: ActivityNotFoundException) {
            runCatching { startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
    }

    /** Xiaomi's own Autostart list, which also stops background apps; falls back to the app's settings. */
    private fun openAutostart() {
        val miui = Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
        try {
            startActivity(miui)
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        runCatching { startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) }
    }
}
