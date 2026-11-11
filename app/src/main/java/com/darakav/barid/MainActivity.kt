package com.darakav.barid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.concurrent.Executors

/** Pairing (QR code, setup code or darakav:// link) and the forwarding status. */
class MainActivity : AppCompatActivity() {
    private lateinit var settings: Settings
    private val io = Executors.newSingleThreadExecutor()

    private val scan = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { pair(it) }
    }
    private val receivePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { render() }
    private val readPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) importRecent() else toast(R.string.import_needs_permission)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = Settings(this)
        findViewById<Button>(R.id.scan).setOnClickListener {
            scan.launch(
                ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt(getString(R.string.scan_prompt)).setBeepEnabled(false).setOrientationLocked(false),
            )
        }
        findViewById<Button>(R.id.connect).setOnClickListener {
            pair(findViewById<EditText>(R.id.code).text.toString())
        }
        findViewById<Button>(R.id.allow_sms).setOnClickListener { receivePermission.launch(Manifest.permission.RECEIVE_SMS) }
        findViewById<Button>(R.id.test).setOnClickListener { test() }
        findViewById<Button>(R.id.import_recent).setOnClickListener {
            if (granted(Manifest.permission.READ_SMS)) importRecent() else readPermission.launch(Manifest.permission.READ_SMS)
        }
        findViewById<Button>(R.id.battery).setOnClickListener {
            startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        findViewById<Button>(R.id.disconnect).setOnClickListener { confirmDisconnect() }
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun handle(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme == "darakav") pair(data.toString())
    }

    private fun pair(input: String) {
        val pairing = Pairing.parse(input)
        if (pairing == null) {
            toast(R.string.invalid_code)
            return
        }
        val connect = {
            MessageQueue(this).clear()
            settings.pairing = pairing
            findViewById<EditText>(R.id.code).setText("")
            render()
            if (!granted(Manifest.permission.RECEIVE_SMS)) receivePermission.launch(Manifest.permission.RECEIVE_SMS)
            test()
        }
        val current = settings.pairing
        if (current != null && current.key != pairing.key) {
            AlertDialog.Builder(this).setTitle(R.string.replace_title).setMessage(R.string.replace_message)
                .setPositiveButton(R.string.replace_action) { _, _ -> connect() }
                .setNegativeButton(android.R.string.cancel, null).show()
        } else connect()
    }

    private fun test() {
        val pairing = settings.pairing ?: return
        status(getString(R.string.testing))
        io.execute {
            val result = Forwarder.check(pairing)
            runOnUiThread {
                when (result) {
                    Forwarder.Result.Ok -> { settings.rejected = false; settings.lastError = null; toast(R.string.test_ok); ForwardWorker.schedule(this) }
                    Forwarder.Result.Rejected -> { settings.rejected = true; toast(R.string.test_rejected) }
                    else -> toast(R.string.test_failed)
                }
                render()
            }
        }
    }

    private fun importRecent() {
        io.execute {
            val count = Backfill.run(this)
            runOnUiThread {
                toast(getString(R.string.import_done, count))
                render()
            }
        }
    }

    private fun confirmDisconnect() {
        AlertDialog.Builder(this).setTitle(R.string.disconnect_title).setMessage(R.string.disconnect_message)
            .setPositiveButton(R.string.disconnect) { _, _ ->
                settings.pairing = null
                MessageQueue(this).clear()
                render()
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun render() {
        val pairing = settings.pairing
        findViewById<View>(R.id.pair_group).visibility = if (pairing == null) View.VISIBLE else View.GONE
        findViewById<View>(R.id.status_group).visibility = if (pairing == null) View.GONE else View.VISIBLE
        if (pairing == null) return
        findViewById<TextView>(R.id.device).text = getString(R.string.device_line, pairing.label, Uri.parse(pairing.url).host ?: pairing.url)
        findViewById<TextView>(R.id.insecure).visibility = if (pairing.encrypted) View.GONE else View.VISIBLE
        val smsAllowed = granted(Manifest.permission.RECEIVE_SMS)
        findViewById<View>(R.id.allow_sms).visibility = if (smsAllowed) View.GONE else View.VISIBLE
        val battery = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
        findViewById<View>(R.id.battery).visibility = if (battery) View.GONE else View.VISIBLE
        val pending = MessageQueue(this).size()
        val last = settings.lastSentAt
        val lines = buildList {
            add(
                when {
                    settings.rejected -> getString(R.string.state_rejected)
                    !smsAllowed -> getString(R.string.state_no_permission)
                    else -> getString(R.string.state_active)
                },
            )
            add(
                if (last > 0) getString(R.string.last_sent, DateUtils.getRelativeTimeSpanString(last), settings.sentCount)
                else getString(R.string.nothing_sent),
            )
            if (pending > 0) add(getString(R.string.pending, pending))
            settings.lastError?.let { add(getString(R.string.last_problem, it)) }
        }
        status(lines.joinToString("\n"))
    }

    private fun status(text: String) {
        findViewById<TextView>(R.id.status).text = text
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun toast(id: Int) = toast(getString(id))
    private fun toast(text: String) = android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }
}
