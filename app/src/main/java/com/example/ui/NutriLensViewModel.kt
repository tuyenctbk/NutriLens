package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.BillingEvent
import com.example.data.BillingLaunchRequest
import com.example.data.BillingRepository
import com.example.data.BillingUiState
import com.example.data.HealthAnalyzer
import com.example.data.PantryRepository
import com.example.data.ProductLookupException
import com.example.data.ProductRepository
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.model.HealthWarning
import com.example.model.PantryItem
import com.example.model.Product
import com.example.model.UserProfile
import com.example.notification.PantryReminderScheduler
import com.example.notification.SafetyNotificationManager
import com.example.ui.components.DonationTier
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Scanner : ScreenDestination()
    object History : ScreenDestination()
    object Pantry : ScreenDestination()
    object Dashboard : ScreenDestination()
    object Profile : ScreenDestination()
    object SupportDevelopment : ScreenDestination()
    data class ProductDetail(val barcode: String) : ScreenDestination()
    data class Compare(val leftBarcode: String, val rightBarcode: String? = null) : ScreenDestination()
    data class IngredientAnalyzer(val initialBarcode: String? = null) : ScreenDestination()
}

enum class HistoryFilter {
    ALL,
    FAVORITES,
    ALERTED, // Has danger or caution warnings
    CLEAN_SAFE // Clean items with no allergen or diet alerts
}

data class ScannerUiState(
    val currentDestination: ScreenDestination = ScreenDestination.Scanner,
    val selectedProduct: Product? = null,
    val productWarnings: List<HealthWarning> = emptyList(),
    val betterAlternatives: List<Product> = emptyList(),
    val isLoadingAlternatives: Boolean = false,
    val compareLeftProduct: Product? = null,
    val compareRightProduct: Product? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isTorchEnabled: Boolean = false,
    val showManualInputDialog: Boolean = false,
    val showDonationDialog: Boolean = false,
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val searchQuery: String = ""
)

class NutriLensViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ProductRepository(
        productDao = database.productDao(),
        defaultProductName = application.getString(R.string.unnamed_food_product),
        defaultBrandName = application.getString(R.string.brand_unknown)
    )
    private val pantryRepository = PantryRepository(database.pantryDao())
    private val preferencesRepository = UserPreferencesRepository(application)
    private val billingRepository = BillingRepository(application.applicationContext, viewModelScope)

    val billingState: StateFlow<BillingUiState> = billingRepository.billingState
    val billingEvents: SharedFlow<BillingEvent> = billingRepository.billingEvents

    private val _billingLaunchRequests = Channel<BillingLaunchRequest>(capacity = Channel.BUFFERED)
    val billingLaunchRequests: Flow<BillingLaunchRequest> = _billingLaunchRequests.receiveAsFlow()

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val userProfile: StateFlow<UserProfile> = preferencesRepository.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    val historyList: StateFlow<List<Product>> = repository.historyProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentHistoryList: StateFlow<List<Product>> = repository.getRecentScanned(20)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteList: StateFlow<List<Product>> = repository.favoriteProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pantryList: StateFlow<List<PantryItem>> = pantryRepository.pantryItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val backStack = mutableListOf<ScreenDestination>(ScreenDestination.Scanner)
    private var loadProductJob: Job? = null
    private var alternativesJob: Job? = null

    init {
        billingRepository.startConnection()
        viewModelScope.launch {
            preferencesRepository.recordAppLaunch()
        }
        viewModelScope.launch {
            billingRepository.supporterEntitlementFlow.collect { unlocked ->
                if (unlocked) {
                    markAsSupporter(true)
                }
            }
        }
        // Reactively keep warnings, healthier alternatives, and pantry reminder schedule in sync with profile
        viewModelScope.launch {
            userProfile.collect { profile ->
                PantryReminderScheduler.syncSchedule(getApplication(), profile.pantryRemindersEnabled)
                val currentProd = _uiState.value.selectedProduct ?: return@collect
                val updatedWarnings = HealthAnalyzer.evaluate(currentProd, profile)
                _uiState.value = _uiState.value.copy(productWarnings = updatedWarnings)
                loadBetterAlternatives(currentProd, profile)
            }
        }
    }

    fun navigateTo(destination: ScreenDestination) {
        val isSubScreen = destination is ScreenDestination.ProductDetail ||
            destination is ScreenDestination.Compare ||
            destination is ScreenDestination.IngredientAnalyzer ||
            destination is ScreenDestination.SupportDevelopment

        if (isSubScreen) {
            if (backStack.lastOrNull() != destination) {
                backStack.add(destination)
            }
        } else {
            backStack.clear()
            backStack.add(ScreenDestination.Scanner)
            if (destination !is ScreenDestination.Scanner) {
                backStack.add(destination)
            }
        }
        _uiState.value = _uiState.value.copy(
            currentDestination = destination,
            errorMessage = null
        )
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            val previous = backStack.last()
            _uiState.value = _uiState.value.copy(
                currentDestination = previous,
                errorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                currentDestination = ScreenDestination.Scanner,
                errorMessage = null
            )
        }
    }

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchEnabled = !_uiState.value.isTorchEnabled)
    }

    fun setManualInputDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showManualInputDialog = visible)
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _uiState.value = _uiState.value.copy(historyFilter = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun scanBarcode(barcode: String) {
        val clean = barcode.trim()
        if (clean.isBlank()) return
        loadProduct(clean)
    }

    fun loadProduct(barcode: String, forceRefresh: Boolean = false) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return

        val detailDest = ScreenDestination.ProductDetail(cleanBarcode)
        if (backStack.lastOrNull() != detailDest) {
            backStack.add(detailDest)
        }

        // Cancel any in-flight scan/fetch to avoid race conditions
        loadProductJob?.cancel()
        alternativesJob?.cancel()

        // Clear stale product if navigating to a different barcode so error states never display stale data
        val preservedProduct = if (_uiState.value.selectedProduct?.barcode == cleanBarcode) {
            _uiState.value.selectedProduct
        } else {
            null
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            selectedProduct = preservedProduct,
            productWarnings = if (preservedProduct != null) _uiState.value.productWarnings else emptyList(),
            betterAlternatives = emptyList(),
            isLoadingAlternatives = false,
            errorMessage = null,
            currentDestination = detailDest
        )

        loadProductJob = viewModelScope.launch {
            repository.getProductFlow(cleanBarcode, forceRefresh).collect { result ->
                result.onSuccess { product ->
                    val profile = userProfile.value
                    val warnings = HealthAnalyzer.evaluate(product, profile)

                    // Trigger system notification alert if critical dietary warnings exist
                    SafetyNotificationManager.sendDietaryAlertNotification(
                        context = getApplication(),
                        product = product,
                        warnings = warnings
                    )

                    // Automatically add scanned product to pantry management list
                    pantryRepository.addProductToPantry(product)

                    // Increment verified operations counter and check Value-Realization milestone
                    val updatedOpsCount = preferencesRepository.incrementOperationsCount()
                    val milestoneProfile = profile.copy(successfulOperationsCount = updatedOpsCount)
                    val triggerMilestone = milestoneProfile.shouldTriggerMilestonePrompt
                    if (triggerMilestone) {
                        preferencesRepository.recordAutoPromptShown()
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedProduct = product,
                        productWarnings = warnings,
                        showDonationDialog = if (triggerMilestone) true else _uiState.value.showDonationDialog,
                        errorMessage = null
                    )

                    loadBetterAlternatives(product, profile)
                }.onFailure { err ->
                    val app = getApplication<Application>()
                    val localizedError = when (err) {
                        is IllegalArgumentException -> app.getString(R.string.error_barcode_blank)
                        is ProductLookupException.NotFound -> app.getString(R.string.error_product_not_found_barcode, err.barcode)
                        is ProductLookupException.NetworkUnavailable -> app.getString(
                            R.string.error_offline_not_cached,
                            err.detail ?: app.getString(R.string.error_network_generic)
                        )
                        else -> err.message ?: app.getString(R.string.error_product_fetch_failed)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = localizedError
                    )
                }
            }
        }
    }

    private fun loadBetterAlternatives(product: Product, profile: UserProfile) {
        alternativesJob?.cancel()
        alternativesJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAlternatives = true)
            val alternatives = repository.getHealthierAlternatives(product, profile)
            // Only apply if this product is still selected
            if (_uiState.value.selectedProduct?.barcode == product.barcode) {
                _uiState.value = _uiState.value.copy(
                    betterAlternatives = alternatives,
                    isLoadingAlternatives = false
                )
            }
        }
    }

    // Side-by-Side Comparison actions
    fun openCompare(leftBarcode: String, rightBarcode: String? = null) {
        val dest = ScreenDestination.Compare(leftBarcode, rightBarcode)
        navigateTo(dest)

        viewModelScope.launch {
            val left = resolveProductForCompare(leftBarcode)
            val right = when {
                !rightBarcode.isNullOrBlank() -> resolveProductForCompare(rightBarcode)
                else -> {
                    // Pre-pick a meaningful partner from better alternatives or scan history
                    val fromAlternatives = _uiState.value.betterAlternatives.firstOrNull { it.barcode != leftBarcode }
                    val fromHistory = historyList.value.firstOrNull { it.barcode != leftBarcode }
                    fromAlternatives ?: fromHistory
                }
            }
            _uiState.value = _uiState.value.copy(
                compareLeftProduct = left,
                compareRightProduct = right
            )
        }
    }

    fun selectCompareProduct(isLeft: Boolean, product: Product) {
        _uiState.value = if (isLeft) {
            _uiState.value.copy(compareLeftProduct = product)
        } else {
            _uiState.value.copy(compareRightProduct = product)
        }
    }

    fun swapCompareProducts() {
        val current = _uiState.value
        _uiState.value = current.copy(
            compareLeftProduct = current.compareRightProduct,
            compareRightProduct = current.compareLeftProduct
        )
    }

    private suspend fun resolveProductForCompare(barcode: String): Product? {
        if (_uiState.value.selectedProduct?.barcode == barcode) {
            return _uiState.value.selectedProduct
        }
        _uiState.value.betterAlternatives.find { it.barcode == barcode }?.let { return it }
        historyList.value.find { it.barcode == barcode }?.let { return it }
        return repository.getProduct(barcode).getOrNull()
    }

    // Ingredient Label Analyzer actions
    fun openIngredientAnalyzer(initialBarcode: String? = null) {
        navigateTo(ScreenDestination.IngredientAnalyzer(initialBarcode))
    }

    fun saveAnalyzedIngredientProduct(product: Product) {
        viewModelScope.launch {
            repository.saveCustomProduct(product)
            pantryRepository.addProductToPantry(product)
            preferencesRepository.incrementOperationsCount()
            loadProduct(product.barcode)
        }
    }

    fun openSupportDevelopment() {
        navigateTo(ScreenDestination.SupportDevelopment)
    }

    /**
     * Emits a ViewModel-scoped billing launch request that MainActivity collects strictly
     * while RESUMED, guaranteeing a live Activity session for BillingClient.launchBillingFlow.
     */
    fun requestGooglePlayDonation(tier: DonationTier) {
        _billingLaunchRequests.trySend(BillingLaunchRequest(tier))
    }

    fun executeGooglePlayBillingLaunch(activity: Activity, request: BillingLaunchRequest) {
        viewModelScope.launch {
            billingRepository.launchBillingFlow(activity, request.tier)
        }
    }

    fun onHostActivityResumed() {
        billingRepository.onHostActivityResumed()
    }

    fun restoreGooglePlayPurchases() {
        billingRepository.restorePurchases()
    }

    fun setDonationDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showDonationDialog = visible)
    }

    fun dismissHomeBanner() {
        viewModelScope.launch {
            preferencesRepository.dismissHomeBanner()
        }
    }

    fun dismissAutoPromptDialog() {
        viewModelScope.launch {
            preferencesRepository.dismissAutoPromptDialog()
            _uiState.value = _uiState.value.copy(showDonationDialog = false)
        }
    }

    fun markAsSupporter(isSupporter: Boolean = true) {
        viewModelScope.launch {
            preferencesRepository.markAsSupporter(isSupporter)
            _uiState.value = _uiState.value.copy(showDonationDialog = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingRepository.release()
        _billingLaunchRequests.close()
    }

    fun toggleFavorite(product: Product) {
        viewModelScope.launch {
            val newFav = !product.isFavorite
            repository.toggleFavorite(product.barcode, newFav)
            if (_uiState.value.selectedProduct?.barcode == product.barcode) {
                _uiState.value = _uiState.value.copy(
                    selectedProduct = _uiState.value.selectedProduct?.copy(isFavorite = newFav)
                )
            }
        }
    }

    fun deleteProduct(barcode: String) {
        viewModelScope.launch {
            repository.deleteScan(barcode)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Pantry methods
    fun togglePantryConsumed(barcode: String, isConsumed: Boolean) {
        viewModelScope.launch {
            pantryRepository.updateConsumed(barcode, isConsumed)
        }
    }

    fun updatePantryExpiration(barcode: String, expirationDate: Long) {
        viewModelScope.launch {
            pantryRepository.updateExpiration(barcode, expirationDate)
        }
    }

    fun deletePantryItem(barcode: String) {
        viewModelScope.launch {
            pantryRepository.deleteItem(barcode)
        }
    }

    fun clearPantry() {
        viewModelScope.launch {
            pantryRepository.clearPantry()
        }
    }

    fun setPantryRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPantryRemindersEnabled(enabled)
            PantryReminderScheduler.syncSchedule(getApplication(), enabled)
            if (enabled) {
                PantryReminderScheduler.checkAndNotifyExpiringItems(getApplication(), forceCheck = true)
            }
        }
    }

    fun triggerPantryExpiryCheckNow(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = PantryReminderScheduler.checkAndNotifyExpiringItems(getApplication(), forceCheck = true)
            onComplete(count)
        }
    }

    // Household multi-profile actions
    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            preferencesRepository.selectProfile(profileId)
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            preferencesRepository.createProfile(name)
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            preferencesRepository.deleteProfile(profileId)
        }
    }

    // Profile updates (reactive collector automatically re-evaluates selected product warnings)
    fun toggleAllergen(allergen: String, isMonitored: Boolean) {
        viewModelScope.launch {
            preferencesRepository.toggleAllergen(allergen, isMonitored)
        }
    }

    fun setVegan(isVegan: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVegan(isVegan)
        }
    }

    fun setVegetarian(isVegetarian: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVegetarian(isVegetarian)
        }
    }

    fun setGlutenFree(isGlutenFree: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setGlutenFree(isGlutenFree)
        }
    }

    fun addCustomKeyword(keyword: String) {
        viewModelScope.launch {
            preferencesRepository.addCustomKeyword(keyword)
        }
    }

    fun removeCustomKeyword(keyword: String) {
        viewModelScope.launch {
            preferencesRepository.removeCustomKeyword(keyword)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
        }
    }

    fun setAvoidPalmOil(avoid: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAvoidPalmOil(avoid)
        }
    }

    fun setAvoidNova4(avoid: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAvoidNova4(avoid)
        }
    }

    fun setAvoidHighRiskAdditives(avoid: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAvoidHighRiskAdditives(avoid)
        }
    }
}
