package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NutriLensViewModel
import com.example.ui.ScreenDestination
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PantryScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.NutriLensTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: NutriLensViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create system notification channel
        com.example.notification.SafetyNotificationManager.createNotificationChannel(applicationContext)

        // Handle barcode intent if launched from notification tap
        intent?.getStringExtra("EXTRA_BARCODE")?.let { barcode ->
            if (barcode.isNotBlank()) {
                viewModel.loadProduct(barcode)
            }
        }

        setContent {
            NutriLensTheme {
                // Request Notification Permission on Android 13+
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) { }
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                NutriLensApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("EXTRA_BARCODE")?.let { barcode ->
            if (barcode.isNotBlank()) {
                viewModel.loadProduct(barcode)
            }
        }
    }
}

@Composable
fun NutriLensApp(viewModel: NutriLensViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val historyProducts by viewModel.historyList.collectAsStateWithLifecycle()
    val recentHistoryProducts by viewModel.recentHistoryList.collectAsStateWithLifecycle()
    val pantryList by viewModel.pantryList.collectAsStateWithLifecycle()

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1400)
        showSplash = false
    }

    // Handle system back navigation to return to Scanner screen if on secondary tabs or detail
    BackHandler(enabled = uiState.currentDestination !is ScreenDestination.Scanner) {
        viewModel.navigateBack()
    }

    // Auto-prompt trigger (7+ scans, 30-day cooldown)
    LaunchedEffect(userProfile.shouldTriggerAutoPrompt) {
        if (userProfile.shouldTriggerAutoPrompt) {
            viewModel.setDonationDialogVisible(true)
        }
    }

    val isDetailScreen = uiState.currentDestination is ScreenDestination.ProductDetail

    val navigationRailContent = @Composable {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            header = {
                Spacer(modifier = Modifier.height(12.dp))
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        ) {
            NavigationRailItem(
                selected = uiState.currentDestination is ScreenDestination.Scanner,
                onClick = { viewModel.navigateTo(ScreenDestination.Scanner) },
                icon = {
                    Icon(
                        imageVector = if (uiState.currentDestination is ScreenDestination.Scanner) {
                            Icons.Filled.QrCodeScanner
                        } else {
                            Icons.Outlined.QrCodeScanner
                        },
                        contentDescription = stringResource(R.string.nav_scan)
                    )
                },
                label = { Text(stringResource(R.string.nav_scan)) },
                modifier = Modifier.testTag("nav_scanner_tab")
            )

            NavigationRailItem(
                selected = uiState.currentDestination is ScreenDestination.History,
                onClick = { viewModel.navigateTo(ScreenDestination.History) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (historyProducts.isNotEmpty()) {
                                Badge {
                                    Text(if (historyProducts.size > 99) "99+" else historyProducts.size.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.currentDestination is ScreenDestination.History) {
                                Icons.Filled.History
                            } else {
                                Icons.Outlined.History
                            },
                            contentDescription = stringResource(R.string.nav_history)
                        )
                    }
                },
                label = { Text(stringResource(R.string.nav_history)) },
                modifier = Modifier.testTag("nav_history_tab")
            )

            NavigationRailItem(
                selected = uiState.currentDestination is ScreenDestination.Dashboard,
                onClick = { viewModel.navigateTo(ScreenDestination.Dashboard) },
                icon = {
                    Icon(
                        imageVector = if (uiState.currentDestination is ScreenDestination.Dashboard) {
                            Icons.Filled.Analytics
                        } else {
                            Icons.Outlined.Analytics
                        },
                        contentDescription = stringResource(R.string.nav_dashboard)
                    )
                },
                label = { Text(stringResource(R.string.nav_dashboard)) },
                modifier = Modifier.testTag("nav_dashboard_tab")
            )

            NavigationRailItem(
                selected = uiState.currentDestination is ScreenDestination.Pantry,
                onClick = { viewModel.navigateTo(ScreenDestination.Pantry) },
                icon = {
                    Icon(
                        imageVector = if (uiState.currentDestination is ScreenDestination.Pantry) {
                            Icons.Filled.Kitchen
                        } else {
                            Icons.Outlined.Kitchen
                        },
                        contentDescription = stringResource(R.string.nav_pantry)
                    )
                },
                label = { Text(stringResource(R.string.nav_pantry)) },
                modifier = Modifier.testTag("nav_pantry_tab")
            )

            NavigationRailItem(
                selected = uiState.currentDestination is ScreenDestination.Profile,
                onClick = { viewModel.navigateTo(ScreenDestination.Profile) },
                icon = {
                    Icon(
                        imageVector = if (uiState.currentDestination is ScreenDestination.Profile) {
                            Icons.Filled.HealthAndSafety
                        } else {
                            Icons.Outlined.HealthAndSafety
                        },
                        contentDescription = stringResource(R.string.nav_profile)
                    )
                },
                label = { Text(stringResource(R.string.nav_profile)) },
                modifier = Modifier.testTag("nav_profile_tab")
            )
        }
    }

    val bottomBarContent = @Composable {
        if (!isDetailScreen) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = uiState.currentDestination is ScreenDestination.Scanner,
                    onClick = { viewModel.navigateTo(ScreenDestination.Scanner) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentDestination is ScreenDestination.Scanner) {
                                Icons.Filled.QrCodeScanner
                            } else {
                                Icons.Outlined.QrCodeScanner
                            },
                            contentDescription = stringResource(R.string.nav_scan)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_scan)) },
                    modifier = Modifier.testTag("nav_scanner_tab")
                )

                NavigationBarItem(
                    selected = uiState.currentDestination is ScreenDestination.History,
                    onClick = { viewModel.navigateTo(ScreenDestination.History) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (historyProducts.isNotEmpty()) {
                                    Badge {
                                        Text(if (historyProducts.size > 99) "99+" else historyProducts.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.currentDestination is ScreenDestination.History) {
                                Icons.Filled.History
                            } else {
                                Icons.Outlined.History
                            },
                            contentDescription = stringResource(R.string.nav_history)
                        )
                    }
                },
                label = { Text(stringResource(R.string.nav_history)) },
                modifier = Modifier.testTag("nav_history_tab")
            )

            NavigationBarItem(
                selected = uiState.currentDestination is ScreenDestination.Dashboard,
                onClick = { viewModel.navigateTo(ScreenDestination.Dashboard) },
                icon = {
                    Icon(
                        imageVector = if (uiState.currentDestination is ScreenDestination.Dashboard) {
                            Icons.Filled.Analytics
                        } else {
                            Icons.Outlined.Analytics
                        },
                        contentDescription = stringResource(R.string.nav_dashboard)
                    )
                },
                label = { Text(stringResource(R.string.nav_dashboard)) },
                modifier = Modifier.testTag("nav_dashboard_tab")
            )

            NavigationBarItem(
                selected = uiState.currentDestination is ScreenDestination.Pantry,
                onClick = { viewModel.navigateTo(ScreenDestination.Pantry) },
                icon = {
                    Icon(
                        imageVector = if (uiState.currentDestination is ScreenDestination.Pantry) {
                            Icons.Filled.Kitchen
                        } else {
                            Icons.Outlined.Kitchen
                        },
                        contentDescription = stringResource(R.string.nav_pantry)
                    )
                },
                label = { Text(stringResource(R.string.nav_pantry)) },
                modifier = Modifier.testTag("nav_pantry_tab")
            )

            NavigationBarItem(
                selected = uiState.currentDestination is ScreenDestination.Profile,
                onClick = { viewModel.navigateTo(ScreenDestination.Profile) },
                icon = {
                    Icon(
                        imageVector = if (uiState.currentDestination is ScreenDestination.Profile) {
                            Icons.Filled.HealthAndSafety
                        } else {
                            Icons.Outlined.HealthAndSafety
                        },
                        contentDescription = stringResource(R.string.nav_profile)
                    )
                },
                label = { Text(stringResource(R.string.nav_profile)) },
                modifier = Modifier.testTag("nav_profile_tab")
            )
        }
    }
}

    val mainScreensContent = @Composable {
        AnimatedContent(
            targetState = uiState.currentDestination,
            transitionSpec = {
                if (targetState is ScreenDestination.ProductDetail) {
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeIn(tween(250)))
                        .togetherWith(slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { width -> -width / 4 } + fadeOut(tween(200)))
                } else if (initialState is ScreenDestination.ProductDetail) {
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> -width / 4 } + fadeIn(tween(250)))
                        .togetherWith(slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeOut(tween(200)))
                } else {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(tween(180)))
                }
            },
            label = "screen_navigation_transition"
        ) { destination ->
            when (destination) {
                is ScreenDestination.Scanner -> {
                    ScannerScreen(
                        isTorchEnabled = uiState.isTorchEnabled,
                        onToggleTorch = { viewModel.toggleTorch() },
                        onBarcodeScanned = { barcode -> viewModel.scanBarcode(barcode) },
                        onNavigateToHistory = { viewModel.navigateTo(ScreenDestination.History) },
                        historyCount = historyProducts.size,
                        onOpenDonationDialog = { viewModel.setDonationDialogVisible(true) },
                        shouldShowHomeBanner = userProfile.shouldShowHomeBanner,
                        onDismissBanner = { viewModel.dismissHomeBanner() },
                        isSupporter = userProfile.isSupporter,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.ProductDetail -> {
                    ProductDetailScreen(
                        product = uiState.selectedProduct,
                        warnings = uiState.productWarnings,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onBack = { viewModel.navigateBack() },
                        onToggleFavorite = { product -> viewModel.toggleFavorite(product) },
                        onRefresh = {
                            uiState.selectedProduct?.barcode?.let { barcode ->
                                viewModel.loadProduct(barcode, forceRefresh = true)
                            }
                        },
                        onOpenDonationDialog = { viewModel.setDonationDialogVisible(true) },
                        isSupporter = userProfile.isSupporter,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.History -> {
                    HistoryScreen(
                        products = recentHistoryProducts,
                        currentFilter = uiState.historyFilter,
                        searchQuery = uiState.searchQuery,
                        onFilterChange = { filter -> viewModel.setHistoryFilter(filter) },
                        onSearchChange = { query -> viewModel.setSearchQuery(query) },
                        onProductClick = { barcode -> viewModel.loadProduct(barcode) },
                        onToggleFavorite = { product -> viewModel.toggleFavorite(product) },
                        onDeleteProduct = { barcode -> viewModel.deleteProduct(barcode) },
                        onClearAll = { viewModel.clearHistory() },
                        onSeedSamples = { viewModel.seedSampleScans() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.Dashboard -> {
                    DashboardScreen(
                        products = historyProducts,
                        onSelectProductForScan = { viewModel.navigateTo(ScreenDestination.Scanner) },
                        onProductClick = { barcode -> viewModel.loadProduct(barcode) },
                        onSeedSamples = { viewModel.seedSampleScans() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.Profile -> {
                    ProfileScreen(
                        userProfile = userProfile,
                        onToggleAllergen = { allergen, monitored ->
                            viewModel.toggleAllergen(allergen, monitored)
                        },
                        onSetVegan = { viewModel.setVegan(it) },
                        onSetVegetarian = { viewModel.setVegetarian(it) },
                        onSetGlutenFree = { viewModel.setGlutenFree(it) },
                        onAddCustomKeyword = { viewModel.addCustomKeyword(it) },
                        onRemoveCustomKeyword = { viewModel.removeCustomKeyword(it) },
                        onSetAvoidPalmOil = { viewModel.setAvoidPalmOil(it) },
                        onSetAvoidNova4 = { viewModel.setAvoidNova4(it) },
                        onSetAvoidHighRiskAdditives = { viewModel.setAvoidHighRiskAdditives(it) },
                        onResetToDefaults = { viewModel.resetToDefaults() },
                        onOpenDonationDialog = { viewModel.setDonationDialogVisible(true) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.Pantry -> {
                    PantryScreen(
                        pantryItems = pantryList,
                        onToggleConsumed = { barcode, consumed -> viewModel.togglePantryConsumed(barcode, consumed) },
                        onUpdateExpiration = { barcode, date -> viewModel.updatePantryExpiration(barcode, date) },
                        onDeleteItem = { barcode -> viewModel.deletePantryItem(barcode) },
                        onClearPantry = { viewModel.clearPantry() },
                        onSeedPantry = { viewModel.seedSamplePantry() },
                        onProductClick = { barcode -> viewModel.loadProduct(barcode) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp
            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (!isDetailScreen) {
                        navigationRailContent()
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        mainScreensContent()
                    }
                }
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = bottomBarContent
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        mainScreensContent()
                    }
                }
            }
        }

        if (uiState.showDonationDialog) {
            com.example.ui.components.DonationDialog(
                isSupporter = userProfile.isSupporter,
                onDismiss = { viewModel.dismissAutoPromptDialog() },
                onConfirmSupporter = { viewModel.markAsSupporter(true) }
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(450, easing = FastOutSlowInEasing)) +
                   scaleOut(targetScale = 1.08f, animationSpec = tween(450, easing = FastOutSlowInEasing))
        ) {
            NutriLensSplashScreen(
                onDismiss = { showSplash = false }
            )
        }
    } // Close outer Box
} // Close NutriLensApp

@Composable
fun NutriLensSplashScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val introScale = remember { Animatable(0.72f) }
    val introAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            introScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            introAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(400)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha_1"
    )
    val laserYFraction by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF022C22),
                        Color(0xFF064E3B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .scale(introScale.value)
                .alpha(introAlpha.value)
        ) {
            // High-Tech Scanner Core with Concentric Glow Waves
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                // Expanding Ripple Halo 1
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFF34D399).copy(alpha = ringAlpha1))
                )

                // Expanding Ripple Halo 2
                Box(
                    modifier = Modifier
                        .size(122.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.22f))
                )

                // Center Icon Disc
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = stringResource(R.string.app_name),
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )

                    // Animated horizontal laser sweep
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(fraction = laserYFraction)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .height(3.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.White,
                                                Color(0xFF6EE7B7),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFFA7F3D0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.splash_technologies),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD1FAE5),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

