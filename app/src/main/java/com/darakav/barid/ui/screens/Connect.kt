package com.darakav.barid.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.darakav.barid.Pairing
import com.darakav.barid.R
import com.darakav.barid.ui.Snapshot
import com.darakav.barid.ui.components.BaridIcons
import com.darakav.barid.ui.components.PrimaryButton
import com.darakav.barid.ui.theme.Barid
import com.darakav.barid.ui.theme.Vazirmatn
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

private val SheetText = Color(0xFFE7EBF3)
private val SheetMuted = Color(0xFFAAB4C5)

/** Dark glass over the camera (the camera feed itself cannot be blurred). */
private fun Modifier.darkGlass(shape: RoundedCornerShape) = this.clip(shape)
    .background(Color(0xFF141C2A).copy(alpha = .72f))
    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .14f), Color.White.copy(alpha = .06f))), shape)

/**
 * Pairing: a full-bleed camera that reads the QR code shown by the server, with pasting a setup code
 * and (later) other servers in a sheet within thumb reach.
 */
@Composable
fun ConnectScreen(canGoBack: Boolean, onBack: () -> Unit, onPaired: (Pairing) -> Unit) {
    val context = LocalContext.current
    var cameraAllowed by remember { mutableStateOf(Snapshot.granted(context, Manifest.permission.CAMERA)) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraAllowed = it
        askedOnce = true
    }
    DisposableEffect(Unit) {
        if (!cameraAllowed && !askedOnce) cameraPermission.launch(Manifest.permission.CAMERA)
        onDispose { }
    }
    var code by rememberSaveable { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val paired by rememberUpdatedState(onPaired)
    fun tryPair(text: String, fromScan: Boolean): Boolean {
        if (done) return true
        val pairing = Pairing.parse(text)
        if (pairing == null) {
            if (!fromScan) invalid = true
            return false
        }
        done = true
        paired(pairing)
        return true
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF060A10))) {
        if (cameraAllowed) {
            QrCamera(Modifier.fillMaxSize()) { scanned -> tryPair(scanned, fromScan = true) }
            ScanFrame(Modifier.align(Alignment.Center).padding(bottom = 140.dp))
        } else {
            CameraOff(
                Modifier.align(Alignment.Center).padding(bottom = 180.dp).padding(horizontal = 32.dp),
                onAllow = {
                    if (!askedOnce) cameraPermission.launch(Manifest.permission.CAMERA)
                    else context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                },
            )
        }
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (canGoBack) {
                Box(
                    Modifier.size(42.dp).darkGlass(RoundedCornerShape(13.dp)).clickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(BaridIcons.Back, stringResource(R.string.cd_back), Modifier.size(20.dp), tint = SheetText) }
            }
            Text(
                stringResource(R.string.connect_hint),
                style = MaterialTheme.typography.bodySmall, color = SheetText, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().darkGlass(RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 11.dp),
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().imePadding()
                .padding(10.dp).darkGlass(RoundedCornerShape(26.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = .25f)))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = .06f))
                    .border(1.dp, if (invalid) Color(0xFFF2A5A0) else Color.White.copy(alpha = .1f), RoundedCornerShape(13.dp))
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
                    if (code.isEmpty()) Text(stringResource(R.string.paste_code_hint), style = MaterialTheme.typography.bodyMedium, color = SheetMuted)
                    BasicTextField(
                        value = code,
                        onValueChange = { code = it.trim(); invalid = false; if (Pairing.parse(code) != null) tryPair(code, false) },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = Vazirmatn, color = SheetText, textDirection = TextDirection.Ltr),
                        cursorBrush = SolidColor(Barid.colors.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) {
                        val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip
                        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty().trim()
                        code = text
                        tryPair(text, false)
                    }.padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(BaridIcons.Paste, null, Modifier.size(16.dp), tint = SheetText)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_paste), style = MaterialTheme.typography.labelMedium, color = SheetText)
                    }
                }
            }
            if (invalid) Text(stringResource(R.string.invalid_code), style = MaterialTheme.typography.bodySmall, color = Color(0xFFF2A5A0))
            if (code.isNotEmpty() && !invalid) {
                PrimaryButton(stringResource(R.string.action_connect), { tryPair(code, false) }, Modifier.fillMaxWidth())
            }
            SheetOption(BaridIcons.Server, stringResource(R.string.other_server_title), stringResource(R.string.other_server_body), stringResource(R.string.coming_soon))
        }
    }
}

@Composable
private fun SheetOption(icon: ImageVector, title: String, body: String, badge: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(17.dp), tint = SheetText)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = SheetText.copy(alpha = .7f))
            Text(body, style = MaterialTheme.typography.bodySmall, color = SheetMuted)
        }
        Text(
            badge, style = MaterialTheme.typography.labelSmall, color = SheetMuted,
            modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = .08f)).padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CameraOff(modifier: Modifier, onAllow: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
            Icon(BaridIcons.CameraOff, null, Modifier.size(26.dp), tint = SheetText)
        }
        Text(stringResource(R.string.camera_off_title), style = MaterialTheme.typography.titleMedium, color = SheetText)
        Text(stringResource(R.string.camera_off_body), style = MaterialTheme.typography.bodySmall, color = SheetMuted, textAlign = TextAlign.Center)
        PrimaryButton(stringResource(R.string.action_allow_camera), onAllow)
    }
}

/** Corner brackets with a slow scanning line in the accent color. */
@Composable
private fun ScanFrame(modifier: Modifier) {
    val accent = Barid.colors.accent
    val line by rememberInfiniteTransition(label = "scan").animateFloat(
        .08f, .92f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "scan-line",
    )
    Canvas(modifier.size(240.dp)) {
        val stroke = 4.dp.toPx()
        val len = 42.dp.toPx()
        val r = 18.dp.toPx()
        val w = size.width
        val h = size.height
        val style = Stroke(stroke, cap = StrokeCap.Round)
        fun corner(x: Float, y: Float, sx: Float, sy: Float) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(x, y + sy * len)
                lineTo(x, y + sy * r)
                quadraticTo(x, y, x + sx * r, y)
                lineTo(x + sx * len, y)
            }
            drawPath(path, Color.White, style = style)
        }
        corner(0f, 0f, 1f, 1f); corner(w, 0f, -1f, 1f); corner(0f, h, 1f, -1f); corner(w, h, -1f, -1f)
        val y = h * line
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, accent, Color.Transparent), 16.dp.toPx(), w - 16.dp.toPx()),
            topLeft = Offset(16.dp.toPx(), y - 1.dp.toPx()), size = Size(w - 32.dp.toPx(), 2.dp.toPx()),
        )
    }
}

/** CameraX preview whose frames are decoded by ZXing (QR only); no Google Play services involved. */
@Composable
private fun QrCamera(modifier: Modifier, onCode: (String) -> Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onCode)
    val view = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(owner) {
        val executor = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            val p = runCatching { future.get() }.getOrNull() ?: return@addListener
            provider = p
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            var stopped = false
            analysis.setAnalyzer(executor, QrAnalyzer { text ->
                view.post { if (!stopped && latest(text)) stopped = true }
            })
            runCatching {
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            executor.shutdown()
        }
    }
    AndroidView({ view }, modifier)
}

private class QrAnalyzer(private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val source = PlanarYUVLuminanceSource(data, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
            val result = runCatching { reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))) }.getOrNull()
            if (result != null) onCode(result.text)
        } finally {
            reader.reset()
            image.close()
        }
    }
}

