package com.darakav.barid

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Keeps the last crash on the phone (never sent anywhere) so the owner can copy it from the app. */
class BaridApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { CrashReport.save(this, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }
}

object CrashReport {
    private fun file(context: Context) = File(context.applicationContext.filesDir, "last-crash.txt")

    fun save(context: Context, thread: Thread, error: Throwable) {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        file(context).writeText(
            "Barid ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · " +
                "${Build.MANUFACTURER} ${Build.MODEL} · thread ${thread.name} · ${System.currentTimeMillis()}\n\n" + trace.take(12_000),
        )
    }

    fun read(context: Context): String? = file(context).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        file(context).delete()
    }
}
