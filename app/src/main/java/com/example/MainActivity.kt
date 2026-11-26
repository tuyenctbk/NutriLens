package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.BillingEvent
import com.example.notification.SafetyNotificationManager
import com.example.ui.NutriLensViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.DonationDialog
import com.example.ui.components.localizedProfileName
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.IngredientAnalyzerScreen
import com.example.ui.screens.PantryScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SupportDevelopmentScreen
import com.example.ui.theme.NutriLensTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: NutriLensViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create system notification channels (Dietary Safety & Pantry Expiration)
        SafetyNotificationManager.createNotificationChannel(applicationContext)

        handleLaunchIntent(intent)

        // Collect ViewModel-scoped Google Play Billing requests strictly while RESUMED
        // so BillingClient.launchBillingFlow always receives a live, valid Activity session.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.billingLaunchRequests.collect { request ->
                    viewModel.executeGooglePlayBillingLaunch(this@MainActivity, request)
                }
            }
        }

        // Surface Billing feedback as Toast when user triggers purchase outside SupportDevelopmentScreen
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.billingEvents.collect { event ->
                    if (viewModel.uiState.value.currentDestination is ScreenDestination.SupportDevelopment) {
                        return@collect
                    }
                    val message = when (event) {
                        is BillingEvent.PurchaseSucceeded -> {
                            val label = event.tier?.let { getString(it.labelRes) }
                                ?: getString(R.string.donation_active_supporter)
                            getString(R.string.donation_processed_toast, label)
                        }
                        is BillingEvent.PurchasePending -> getString(R.string.billing_purchase_pending)
                        is BillingEvent.PurchaseCancelled -> getString(R.string.billing_purchase_cancelled)
                        is BillingEvent.RestoreResult -> {
                            if (event.restored) getString(R.string.billing_restore_success)
                            else getString(R.string.billing_restore_none)
                        }
                        is BillingEvent.BillingMessage -> {
                            if (event.formatArg != null) getString(event.messageRes, event.formatArg)
                            else getString(event.messageRes)
                        }
                        is BillingEvent.BillingError -> getString(R.string.billing_generic_error, event.debugMessage)
                    }
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }

        setContent {
            NutriLensTheme {
                NutriLensApp(
                    viewModel = viewModel,
                    hostActivity = this@MainActivity
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onHostActivityResumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        val barcode = intent?.getStringExtra("EXTRA_BARCODE")
        if (!barcode.isNullOrBlank()) {
            viewModel.loadProduct(barcode)
            return
        }
        val openTab = intent?.getStringExtra(SafetyNotificationManager.EXTRA_OPEN_TAB)
        if (openTab == SafetyNotificationManager.TAB_PANTRY) {
            viewModel.navigateTo(ScreenDestination.Pantry)
        }
    }
}

@Composable
fun NutriLensApp(
    viewModel: NutriLensViewModel,
    hostActivity: android.app.Activity? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val historyProducts by viewModel.historyList.collectAsStateWithLifecycle()
    val recentHistoryProducts by viewModel.recentHistoryList.collectAsStateWithLifecycle()
    val pantryList by viewModel.pantryList.collectAsStateWithLifecycle()
    val activeProfileDisplayName = localizedProfileName(userProfile.activeProfileId, userProfile.activeProfileName)

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1350)
        showSplash = false
    }

    // Handle system back navigation to return to Scanner screen if on secondary tabs or detail
    BackHandler(enabled = uiState.currentDestination !is ScreenDestination.Scanner) {
        viewModel.navigateBack()
    }

    val isDetailScreen = uiState.currentDestination is ScreenDestination.ProductDetail ||
        uiState.currentDestination is ScreenDestination.Compare ||
        uiState.currentDestination is ScreenDestination.IngredientAnalyzer ||
        uiState.currentDestination is ScreenDestination.SupportDevelopment

    val navigationRailContent = @Composable {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            header = {
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon_fg),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 2.dp,
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
                val targetIsSub = targetState is ScreenDestination.ProductDetail ||
                    targetState is ScreenDestination.Compare ||
                    targetState is ScreenDestination.IngredientAnalyzer ||
                    targetState is ScreenDestination.SupportDevelopment
                val initialIsSub = initialState is ScreenDestination.ProductDetail ||
                    initialState is ScreenDestination.Compare ||
                    initialState is ScreenDestination.IngredientAnalyzer ||
                    initialState is ScreenDestination.SupportDevelopment

                if (targetIsSub && !initialIsSub) {
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeIn(tween(240)))
                        .togetherWith(slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { width -> -width / 4 } + fadeOut(tween(180)))
                } else if (initialIsSub && !targetIsSub) {
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> -width / 4 } + fadeIn(tween(240)))
                        .togetherWith(slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeOut(tween(180)))
                } else {
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(tween(160)))
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
                        onOpenIngredientAnalyzer = { viewModel.openIngredientAnalyzer() },
                        activeProfileName = activeProfileDisplayName,
                        historyCount = historyProducts.size,
                        onOpenDonationDialog = { viewModel.openSupportDevelopment() },
                        shouldShowHomeBanner = userProfile.shouldShowHomeBanner,
                        onDismissBanner = { viewModel.dismissHomeBanner() },
                        isSupporter = userProfile.isSupporter,
                        isSplashVisible = showSplash,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.ProductDetail -> {
                    ProductDetailScreen(
                        product = uiState.selectedProduct,
                        warnings = uiState.productWarnings,
                        betterAlternatives = uiState.betterAlternatives,
                        isLoadingAlternatives = uiState.isLoadingAlternatives,
                        activeProfileName = activeProfileDisplayName,
                        failedBarcode = destination.barcode,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onBack = { viewModel.navigateBack() },
                        onToggleFavorite = { product -> viewModel.toggleFavorite(product) },
                        onRefresh = {
                            val targetBarcode = destination.barcode.ifBlank {
                                uiState.selectedProduct?.barcode.orEmpty()
                            }
                            if (targetBarcode.isNotBlank()) {
                                viewModel.loadProduct(targetBarcode, forceRefresh = true)
                            }
                        },
                        onCompareClick = { leftCode, rightCode ->
                            viewModel.openCompare(leftCode, rightCode)
                        },
                        onAlternativeClick = { altBarcode ->
                            viewModel.loadProduct(altBarcode)
                        },
                        onOpenIngredientAnalyzer = { barcode ->
                            viewModel.openIngredientAnalyzer(barcode)
                        },
                        onOpenDonationDialog = { viewModel.setDonationDialogVisible(true) },
                        showPostScanSupport = userProfile.shouldShowPostScanSupport,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.Compare -> {
                    CompareScreen(
                        leftProduct = uiState.compareLeftProduct,
                        rightProduct = uiState.compareRightProduct,
                        availableProducts = historyProducts,
                        userProfile = userProfile,
                        onSelectProduct = { isLeft, product ->
                            viewModel.selectCompareProduct(isLeft, product)
                        },
                        onSwapProducts = { viewModel.swapCompareProducts() },
                        onInspectProduct = { barcode -> viewModel.loadProduct(barcode) },
                        onBack = { viewModel.navigateBack() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.IngredientAnalyzer -> {
                    IngredientAnalyzerScreen(
                        initialBarcode = destination.initialBarcode,
                        userProfile = userProfile,
                        onSaveProduct = { product -> viewModel.saveAnalyzedIngredientProduct(product) },
                        onBack = { viewModel.navigateBack() },
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
                        onCompareProduct = { barcode -> viewModel.openCompare(barcode) },
                        onToggleFavorite = { product -> viewModel.toggleFavorite(product) },
                        onDeleteProduct = { barcode -> viewModel.deleteProduct(barcode) },
                        onClearAll = { viewModel.clearHistory() },
                        onScanProduct = { viewModel.navigateTo(ScreenDestination.Scanner) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.Dashboard -> {
                    DashboardScreen(
                        products = historyProducts,
                        onSelectProductForScan = { viewModel.navigateTo(ScreenDestination.Scanner) },
                        onProductClick = { barcode -> viewModel.loadProduct(barcode) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.Profile -> {
                    ProfileScreen(
                        userProfile = userProfile,
                        onSelectProfile = { viewModel.selectProfile(it) },
                        onCreateProfile = { viewModel.createProfile(it) },
                        onDeleteProfile = { viewModel.deleteProfile(it) },
                        onSetPantryRemindersEnabled = { viewModel.setPantryRemindersEnabled(it) },
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
                        onOpenSupportDevelopment = { viewModel.openSupportDevelopment() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.SupportDevelopment -> {
                    SupportDevelopmentScreen(
                        isSupporter = userProfile.isSupporter,
                        billingState = billingState,
                        billingEvents = viewModel.billingEvents,
                        onRequestGooglePlayDonation = { tier -> viewModel.requestGooglePlayDonation(tier) },
                        onRestorePurchases = { viewModel.restoreGooglePlayPurchases() },
                        onBack = { viewModel.navigateBack() },
                        hostActivity = hostActivity,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ScreenDestination.Pantry -> {
                    PantryScreen(
                        pantryItems = pantryList,
                        pantryRemindersEnabled = userProfile.pantryRemindersEnabled,
                        onTogglePantryReminders = { viewModel.setPantryRemindersEnabled(it) },
                        onCheckExpiringNow = { onResult -> viewModel.triggerPantryExpiryCheckNow(onResult) },
                        onToggleConsumed = { barcode, consumed -> viewModel.togglePantryConsumed(barcode, consumed) },
                        onUpdateExpiration = { barcode, date -> viewModel.updatePantryExpiration(barcode, date) },
                        onDeleteItem = { barcode -> viewModel.deletePantryItem(barcode) },
                        onClearPantry = { viewModel.clearPantry() },
                        onScanProduct = { viewModel.navigateTo(ScreenDestination.Scanner) },
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
            DonationDialog(
                isSupporter = userProfile.isSupporter,
                billingState = billingState,
                onDismiss = { viewModel.dismissAutoPromptDialog() },
                onRequestGooglePlayDonation = { tier -> viewModel.requestGooglePlayDonation(tier) },
                hostActivity = hostActivity
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(420, easing = FastOutSlowInEasing)) +
                scaleOut(targetScale = 1.06f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        ) {
            NutriLensSplashScreen(
                onDismiss = { showSplash = false }
            )
        }
    }
}

@Composable
fun NutriLensSplashScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconScale = remember { Animatable(0.76f) }
    val iconAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(18f) }
    val badgeAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            iconAlpha.animateTo(1f, animationSpec = tween(320))
        }
        launch {
            delay(160)
            textAlpha.animateTo(1f, animationSpec = tween(360, easing = FastOutSlowInEasing))
        }
        launch {
            delay(160)
            textOffsetY.animateTo(
                0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch {
            delay(320)
            badgeAlpha.animateTo(1f, animationSpec = tween(340))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF075942),
                        Color(0xFF043D2C),
                        Color(0xFF071A14)
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
        // Subtle ambient radial spotlight behind emblem
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF34D399).copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.42f),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width / 2f, size.height * 0.42f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Brand Emblem Medallion with Concentric Sonar Halo & Scan Beam
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(164.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
            ) {
                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFF34D399).copy(alpha = ringAlpha))
                )

                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.18f))
                        .border(1.dp, Color(0xFF6EE7B7).copy(alpha = 0.35f), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_fg),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Crisp animated scan beam sweeping smoothly across the lens emblem
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y = size.height * laserProgress
                        val pad = size.width * 0.16f
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF6EE7B7),
                                    Color.White,
                                    Color(0xFF6EE7B7),
                                    Color.Transparent
                                )
                            ),
                            start = Offset(pad, y),
                            end = Offset(size.width - pad, y),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = textOffsetY.value.dp)
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 34.sp,
                        letterSpacing = 1.2.sp
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
            }

            Spacer(modifier = Modifier.height(30.dp))

            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .alpha(badgeAlpha.value)
                    .border(0.75.dp, Color(0xFF6EE7B7).copy(alpha = 0.28f), RoundedCornerShape(20.dp))
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
