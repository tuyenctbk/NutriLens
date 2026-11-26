package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.scanner.BarcodeScannerView

@Composable
fun ScannerScreen(
    isTorchEnabled: Boolean,
    onToggleTorch: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onOpenIngredientAnalyzer: () -> Unit = {},
    activeProfileName: String = "",
    historyCount: Int = 0,
    onOpenDonationDialog: () -> Unit = {},
    shouldShowHomeBanner: Boolean = false,
    onDismissBanner: () -> Unit = {},
    isSupporter: Boolean = false,
    isSplashVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findScannerActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isPermanentlyDenied by remember { mutableStateOf(false) }
    var cameraPromptHandled by remember { mutableStateOf(hasCameraPermission) }
    var notifPromptRequested by remember { mutableStateOf(false) }

    // Re-check camera permission when returning to foreground (e.g. from system App Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = granted
                if (granted) {
                    isPermanentlyDenied = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        cameraPromptHandled = true
        if (!granted) {
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            isPermanentlyDenied = !shouldShowRationale
        } else {
            isPermanentlyDenied = false
        }
    }

    // Sequentially request CAMERA after splash finishes, then POST_NOTIFICATIONS on Android 13+
    LaunchedEffect(isSplashVisible, cameraPromptHandled) {
        if (!isSplashVisible) {
            if (!hasCameraPermission && !cameraPromptHandled) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else if (cameraPromptHandled && !notifPromptRequested && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifPromptRequested = true
                val hasNotif = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            BarcodeScannerView(
                isTorchEnabled = isTorchEnabled,
                onBarcodeDetected = { barcode ->
                    onBarcodeScanned(barcode)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission request screen / fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPermanentlyDenied) Icons.Default.Settings else Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.camera_required_title),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.camera_required_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPermanentlyDenied) {
                                stringResource(R.string.camera_permission_permanently_denied)
                            } else {
                                stringResource(R.string.camera_required_desc)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        if (isPermanentlyDenied) {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    runCatching { context.startActivity(intent) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_settings_permission_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.open_settings_button))
                            }
                        } else {
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grant_camera_permission_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.enable_camera_button))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = { showManualInputDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("enter_manual_barcode_button")
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.enter_manual_button))
                        }
                    }
                }
            }
        }

        // Top Bar overlay with frosted dark glass pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.62f),
                contentColor = Color.White,
                modifier = Modifier.border(
                    width = 0.75.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF4EFAA4),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.scanner_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isSupporter) {
                                "$activeProfileName • ${stringResource(R.string.donation_active_supporter)}"
                            } else {
                                activeProfileName
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSupporter) Color(0xFFFF80AB) else Color(0xFF86EFAC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Ingredient Label Analyzer button
                IconButton(
                    onClick = onOpenIngredientAnalyzer,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("scanner_label_analyzer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = stringResource(R.string.label_analyzer_title),
                        tint = Color(0xFF4EFAA4)
                    )
                }

                // Scan History Shortcut button
                IconButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("scanner_history_shortcut_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (historyCount > 0) {
                                Badge(containerColor = Color(0xFF4EFAA4)) {
                                    Text(
                                        text = if (historyCount > 99) "99+" else historyCount.toString(),
                                        color = Color(0xFF003825),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.open_history_cd),
                            tint = Color.White
                        )
                    }
                }

                // Manual input button
                IconButton(
                    onClick = { showManualInputDialog = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("manual_barcode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = stringResource(R.string.manual_barcode_cd),
                        tint = Color.White
                    )
                }

                // Support / Donation button
                IconButton(
                    onClick = onOpenDonationDialog,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("donation_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.donation_title),
                        tint = if (isSupporter) Color(0xFFFF4081) else Color.White
                    )
                }

                // Torch toggle button with animated warm glow
                if (hasCameraPermission) {
                    val torchBg by animateColorAsState(
                        targetValue = if (isTorchEnabled) Color(0xFFFFD54F).copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.62f),
                        animationSpec = tween(220),
                        label = "torch_bg"
                    )
                    val torchBorder by animateColorAsState(
                        targetValue = if (isTorchEnabled) Color(0xFFFFD54F).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.16f),
                        animationSpec = tween(220),
                        label = "torch_border"
                    )
                    IconButton(
                        onClick = onToggleTorch,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(torchBg)
                            .border(0.75.dp, torchBorder, CircleShape)
                            .testTag("torch_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = stringResource(R.string.torch_toggle),
                            tint = if (isTorchEnabled) Color(0xFFFFD54F) else Color.White
                        )
                    }
                }
            }
        }

        // Animated Compact Home Screen Donation Banner
        AnimatedVisibility(
            visible = shouldShowHomeBanner,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 66.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_donation_banner"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.92f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF4081),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_donation_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onOpenDonationDialog,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(stringResource(R.string.support_dev_button), fontSize = 11.sp, color = Color.White)
                    }
                    IconButton(onClick = onDismissBanner) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Bottom Controls: Scanner Hint + Direct Utility Actions
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f), Color.Black)
                    )
                )
                .padding(bottom = 24.dp, top = 28.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = stringResource(R.string.scanner_hint),
                        color = Color.White.copy(alpha = 0.94f),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showManualInputDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF0E2F22).copy(alpha = 0.92f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_bottom_manual_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = Color(0xFF4EFAA4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.enter_manual_button),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenIngredientAnalyzer,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF0E2F22).copy(alpha = 0.92f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_bottom_label_analyzer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = Color(0xFF4EFAA4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.label_analyzer_title),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Manual Barcode Dialog
    if (showManualInputDialog) {
        val trimmedBarcode = manualBarcodeText.trim()
        AlertDialog(
            onDismissRequest = {
                showManualInputDialog = false
                manualBarcodeText = ""
            },
            title = { Text(stringResource(R.string.manual_entry_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.manual_entry_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualBarcodeText,
                        onValueChange = { input ->
                            manualBarcodeText = input.filter { it.isLetterOrDigit() || it == '-' }
                        },
                        label = { Text(stringResource(R.string.manual_entry_label)) },
                        placeholder = { Text(stringResource(R.string.manual_entry_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (trimmedBarcode.isNotBlank()) {
                                    showManualInputDialog = false
                                    manualBarcodeText = ""
                                    onBarcodeScanned(trimmedBarcode)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_barcode_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = trimmedBarcode.isNotBlank(),
                    onClick = {
                        if (trimmedBarcode.isNotBlank()) {
                            showManualInputDialog = false
                            manualBarcodeText = ""
                            onBarcodeScanned(trimmedBarcode)
                        }
                    },
                    modifier = Modifier.testTag("submit_manual_barcode_button")
                ) {
                    Text(stringResource(R.string.lookup_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showManualInputDialog = false
                        manualBarcodeText = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

private tailrec fun Context.findScannerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findScannerActivity()
    else -> null
}
