package com.example.scanner

import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@Composable
fun BarcodeScannerView(
    isTorchEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var scanFlashActive by remember { mutableStateOf(false) }

    val latestOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val lastScannedBarcode = remember { AtomicReference<String?>(null) }
    val lastScannedTimestamp = remember { AtomicLong(0L) }

    val haptic = LocalHapticFeedback.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    val isDisposed = remember { AtomicBoolean(false) }

    LaunchedEffect(isTorchEnabled, camera) {
        val activeCamera = camera ?: return@LaunchedEffect
        if (activeCamera.cameraInfo.hasFlashUnit()) {
            runCatching { activeCamera.cameraControl.enableTorch(isTorchEnabled) }
        }
    }

    LaunchedEffect(scanFlashActive) {
        if (scanFlashActive) {
            delay(280)
            scanFlashActive = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isDisposed.set(true)
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                Log.w("NutriLensScanner", "Failed to unbind camera provider", e)
            }
            // Close ML Kit scanner on the analysis executor after any in-flight frame finishes
            runCatching {
                cameraExecutor.execute {
                    try {
                        barcodeScanner.close()
                    } catch (e: Exception) {
                        Log.w("NutriLensScanner", "Failed to close barcode scanner", e)
                    }
                }
            }
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val mainExecutor = ContextCompat.getMainExecutor(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    if (isDisposed.get() || cameraExecutor.isShutdown) return@addListener
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderRef = cameraProvider

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (isDisposed.get()) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            processImageProxy(
                                scanner = barcodeScanner,
                                imageProxy = imageProxy,
                                onBarcodeFound = { rawBarcode ->
                                    if (isDisposed.get()) return@processImageProxy
                                    val now = System.currentTimeMillis()
                                    val prevCode = lastScannedBarcode.get()
                                    val prevTime = lastScannedTimestamp.get()
                                    // Debounce duplicate scans (minimum 1.5 seconds)
                                    if (rawBarcode != prevCode || now - prevTime > 1500L) {
                                        lastScannedBarcode.set(rawBarcode)
                                        lastScannedTimestamp.set(now)
                                        mainExecutor.execute {
                                            if (isDisposed.get()) return@execute
                                            scanFlashActive = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            latestOnBarcodeDetected(rawBarcode)
                                        }
                                    }
                                }
                            )
                        }

                        val cameraSelector = when {
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                            else -> CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("NutriLensScanner", "Use case binding failed", e)
                    }
                }, mainExecutor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder reticle overlay & scanning animation
        ScannerOverlay(isFlashActive = scanFlashActive)
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeFound: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        try {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val raw = barcode.rawValue
                        if (!raw.isNullOrBlank()) {
                            onBarcodeFound(raw)
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("NutriLensScanner", "Barcode scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            Log.e("NutriLensScanner", "Failed to submit frame to ML Kit scanner", e)
            imageProxy.close()
        }
    } else {
        imageProxy.close()
    }
}

@Composable
fun ScannerOverlay(
    isFlashActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_overlay_anim")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )
    val bracketPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracket_pulse"
    )
    val flashAlpha by animateFloatAsState(
        targetValue = if (isFlashActive) 0.35f else 0f,
        animationSpec = tween(220),
        label = "scan_flash_alpha"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val boxWidth = (maxWidth * 0.78f).coerceAtMost(360.dp)
        val boxHeight = 230.dp

        val density = LocalDensity.current
        val widthPx = with(density) { boxWidth.toPx() }
        val heightPx = with(density) { boxHeight.toPx() }
        val cornerPx = with(density) { 22.dp.toPx() }

        val cutoutPath = remember { Path() }
        val bracketPath = remember { Path() }

        // Semi-transparent darkened background with rounded cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val left = (canvasWidth - widthPx) / 2f
            val top = (canvasHeight - heightPx) / 2f

            val cutoutRect = RoundRect(
                rect = Rect(Offset(left, top), Size(widthPx, heightPx)),
                cornerRadius = CornerRadius(cornerPx, cornerPx)
            )

            cutoutPath.reset()
            cutoutPath.addRoundRect(cutoutRect)

            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.62f))
            }
        }

        // Viewfinder frame and precision corner brackets
        Box(
            modifier = Modifier
                .size(boxWidth, boxHeight)
                .background(
                    color = Color(0xFF34D399).copy(alpha = flashAlpha),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = if (isFlashActive) 2.dp else 1.dp,
                    color = Color(0xFF34D399).copy(alpha = if (isFlashActive) 0.9f else 0.28f),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            // High-precision Corner Brackets Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bracketLen = 30.dp.toPx()
                val strokeW = 3.5.dp.toPx()
                val cornerRadius = 18.dp.toPx()
                val bracketColor = if (isFlashActive) {
                    Color.White
                } else {
                    Color(0xFF34D399).copy(alpha = bracketPulseAlpha)
                }
                val strokeStyle = Stroke(width = strokeW, cap = StrokeCap.Round)

                bracketPath.reset()
                // Top-Left
                bracketPath.moveTo(0f, bracketLen)
                bracketPath.lineTo(0f, cornerRadius)
                bracketPath.quadraticTo(0f, 0f, cornerRadius, 0f)
                bracketPath.lineTo(bracketLen, 0f)

                // Top-Right
                bracketPath.moveTo(size.width - bracketLen, 0f)
                bracketPath.lineTo(size.width - cornerRadius, 0f)
                bracketPath.quadraticTo(size.width, 0f, size.width, cornerRadius)
                bracketPath.lineTo(size.width, bracketLen)

                // Bottom-Left
                bracketPath.moveTo(0f, size.height - bracketLen)
                bracketPath.lineTo(0f, size.height - cornerRadius)
                bracketPath.quadraticTo(0f, size.height, cornerRadius, size.height)
                bracketPath.lineTo(bracketLen, size.height)

                // Bottom-Right
                bracketPath.moveTo(size.width - bracketLen, size.height)
                bracketPath.lineTo(size.width - cornerRadius, size.height)
                bracketPath.quadraticTo(size.width, size.height, size.width, size.height - cornerRadius)
                bracketPath.lineTo(size.width, size.height - bracketLen)

                drawPath(
                    path = bracketPath,
                    color = bracketColor,
                    style = strokeStyle
                )
            }

            // Animated Laser Sweep Beam with Glowing Aura
            val laserY = boxHeight * laserProgress

            // Trailing aura
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .offset(y = laserY - 14.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF10B981).copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Sharp Core Laser line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .offset(y = laserY)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF34D399),
                                Color.White,
                                Color(0xFF34D399),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
