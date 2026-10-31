package com.example.scanner

import android.annotation.SuppressLint
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerView(
    isTorchEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
    var lastScannedTimestamp by remember { mutableStateOf(0L) }

    val haptic = LocalHapticFeedback.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(isTorchEnabled, camera) {
        camera?.cameraControl?.enableTorch(isTorchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
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

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

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

                        val scanner = BarcodeScanning.getClient(options)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(
                                scanner = scanner,
                                imageProxy = imageProxy,
                                onBarcodeFound = { rawBarcode ->
                                    val now = System.currentTimeMillis()
                                    // Debounce scans (minimum 1.5 seconds)
                                    if (rawBarcode != lastScannedBarcode || now - lastScannedTimestamp > 1500) {
                                        lastScannedBarcode = rawBarcode
                                        lastScannedTimestamp = now
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onBarcodeDetected(rawBarcode)
                                    }
                                }
                            )
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

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
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder reticle overlay & scanning animation
        ScannerOverlay()
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeFound: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
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
    } else {
        imageProxy.close()
    }
}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_overlay_anim")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
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

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val boxWidth = maxWidth * 0.78f
        val boxHeight = 230.dp

        val density = LocalDensity.current
        val widthPx = with(density) { boxWidth.toPx() }
        val heightPx = with(density) { boxHeight.toPx() }

        // Semi-transparent darkened background with rounded cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val left = (canvasWidth - widthPx) / 2f
            val top = (canvasHeight - heightPx) / 2f

            val cutoutRect = RoundRect(
                rect = Rect(Offset(left, top), Size(widthPx, heightPx)),
                cornerRadius = CornerRadius(24f, 24f)
            )

            val backgroundPath = Path().apply {
                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
            }
            val cutoutPath = Path().apply {
                addRoundRect(cutoutRect)
            }

            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.62f))
            }
        }

        // Viewfinder frame and high-tech corner brackets
        Box(
            modifier = Modifier
                .size(boxWidth, boxHeight)
                .border(
                    width = 1.dp,
                    color = Color(0xFF34D399).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            // High-tech Corner Brackets Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bracketLen = 28.dp.toPx()
                val strokeW = 3.5.dp.toPx()
                val cornerRadius = 16.dp.toPx()
                val bracketColor = Color(0xFF34D399).copy(alpha = bracketPulseAlpha)

                // Top-Left
                drawPath(
                    path = Path().apply {
                        moveTo(0f, bracketLen)
                        lineTo(0f, cornerRadius)
                        quadraticTo(0f, 0f, cornerRadius, 0f)
                        lineTo(bracketLen, 0f)
                    },
                    color = bracketColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round
                    )
                )

                // Top-Right
                drawPath(
                    path = Path().apply {
                        moveTo(size.width - bracketLen, 0f)
                        lineTo(size.width - cornerRadius, 0f)
                        quadraticTo(size.width, 0f, size.width, cornerRadius)
                        lineTo(size.width, bracketLen)
                    },
                    color = bracketColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round
                    )
                )

                // Bottom-Left
                drawPath(
                    path = Path().apply {
                        moveTo(0f, size.height - bracketLen)
                        lineTo(0f, size.height - cornerRadius)
                        quadraticTo(0f, size.height, cornerRadius, size.height)
                        lineTo(bracketLen, size.height)
                    },
                    color = bracketColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round
                    )
                )

                // Bottom-Right
                drawPath(
                    path = Path().apply {
                        moveTo(size.width - bracketLen, size.height)
                        lineTo(size.width - cornerRadius, size.height)
                        quadraticTo(size.width, size.height, size.width, size.height - cornerRadius)
                        lineTo(size.width, size.height - bracketLen)
                    },
                    color = bracketColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round
                    )
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
                                Color(0xFF10B981).copy(alpha = 0.28f),
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
