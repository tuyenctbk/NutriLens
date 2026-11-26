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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

private data class DietaryCriteriaKey(
    val activeProfileId: String,
    val monitoredAllergens: Set<String>,
    val isVegan: Boolean,
    val isVegetarian: Boolean,
    val isGlutenFree: Boolean,
    val customAvoidKeywords: Set<String>,
    val avoidPalmOil: Boolean,
    val avoidNova4UltraProcessed: Boolean,
    val avoidHighRiskAdditives: Boolean
)

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
    val searchQuery: String = "",
    val cameraPromptHandled: Boolean = false,
    val notifPromptRequested: Boolean = false,
    val isCameraPermanentlyDenied: Boolean = false
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
        // Sync JobScheduler only when pantryRemindersEnabled actually changes (avoids resetting periodic job on every scan)
        viewModelScope.launch {
            userProfile
                .map { it.pantryRemindersEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    PantryReminderScheduler.syncSchedule(getApplication(), enabled)
                }
        }
        // Re-evaluate warnings & healthier swaps ONLY when dietary profile criteria change (not on operation count / banner ticks)
        viewModelScope.launch {
            userProfile
                .map { profile ->
                    DietaryCriteriaKey(
                        activeProfileId = profile.activeProfileId,
                        monitoredAllergens = profile.monitoredAllergens,
                        isVegan = profile.isVegan,
                        isVegetarian = profile.isVegetarian,
                        isGlutenFree = profile.isGlutenFree,
                        customAvoidKeywords = profile.customAvoidKeywords,
                        avoidPalmOil = profile.avoidPalmOil,
                        avoidNova4UltraProcessed = profile.avoidNova4UltraProcessed,
                        avoidHighRiskAdditives = profile.avoidHighRiskAdditives
                    ) to profile
                }
                .distinctUntilChanged { old, new -> old.first == new.first }
                .drop(1)
                .collect { (_, profile) ->
                    val currentProd = _uiState.value.selectedProduct ?: return@collect
                    val updatedWarnings = HealthAnalyzer.evaluate(currentProd, profile)
                    _uiState.update { it.copy(productWarnings = updatedWarnings) }
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
        _uiState.update {
            it.copy(
                currentDestination = destination,
                errorMessage = null
            )
        }
    }

    fun navigateBack() {
        val previous = if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            backStack.last()
        } else {
            ScreenDestination.Scanner
        }

        if (previous is ScreenDestination.ProductDetail &&
            _uiState.value.selectedProduct?.barcode != previous.barcode
        ) {
            loadProduct(
                barcode = previous.barcode,
                forceRefresh = false,
                isNewScan = false,
                pushToBackStack = false
            )
            return
        }

        _uiState.update {
            it.copy(
                currentDestination = previous,
                errorMessage = null
            )
        }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchEnabled = !it.isTorchEnabled) }
    }

    fun onCameraPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        _uiState.update {
            it.copy(
                cameraPromptHandled = true,
                isCameraPermanentlyDenied = if (granted) false else permanentlyDenied
            )
        }
    }

    fun onNotifPromptRequested() {
        _uiState.update { it.copy(notifPromptRequested = true) }
    }

    fun setManualInputDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showManualInputDialog = visible) }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun scanBarcode(barcode: String) {
        val clean = barcode.trim()
        if (clean.isBlank()) return
        loadProduct(clean, forceRefresh = false, isNewScan = true)
    }

    fun loadProduct(
        barcode: String,
        forceRefresh: Boolean = false,
        isNewScan: Boolean = false,
        pushToBackStack: Boolean = true
    ) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return

        val detailDest = ScreenDestination.ProductDetail(cleanBarcode)
        if (pushToBackStack && backStack.lastOrNull() != detailDest) {
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

        _uiState.update {
            it.copy(
                isLoading = true,
                selectedProduct = preservedProduct,
                productWarnings = if (preservedProduct != null) it.productWarnings else emptyList(),
                betterAlternatives = emptyList(),
                isLoadingAlternatives = false,
                errorMessage = null,
                currentDestination = detailDest
            )
        }

        loadProductJob = viewModelScope.launch {
            repository.getProductFlow(cleanBarcode, forceRefresh).collect { result ->
                result.onSuccess { product ->
                    val profile = userProfile.value
                    val warnings = HealthAnalyzer.evaluate(product, profile)

                    var triggerMilestone = false
                    if (isNewScan) {
                        // Trigger system notification alert if critical dietary warnings exist
                        SafetyNotificationManager.sendDietaryAlertNotification(
                            context = getApplication(),
                            product = product,
                            warnings = warnings
                        )

                        // Automatically add newly scanned product to pantry management list
                        pantryRepository.addProductToPantry(product)

                        // Increment verified operations counter and check Value-Realization milestone
                        val updatedOpsCount = preferencesRepository.incrementOperationsCount()
                        val milestoneProfile = profile.copy(successfulOperationsCount = updatedOpsCount)
                        triggerMilestone = milestoneProfile.shouldTriggerMilestonePrompt
                        if (triggerMilestone) {
                            preferencesRepository.recordAutoPromptShown()
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedProduct = product,
                            productWarnings = warnings,
                            showDonationDialog = if (triggerMilestone) true else it.showDonationDialog,
                            errorMessage = null
                        )
                    }

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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = localizedError
                        )
                    }
                }
            }
        }
    }

    private fun loadBetterAlternatives(product: Product, profile: UserProfile) {
        alternativesJob?.cancel()
        alternativesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAlternatives = true) }
            val alternatives = repository.getHealthierAlternatives(product, profile)
            // Only apply if this product is still selected
            _uiState.update { current ->
                if (current.selectedProduct?.barcode == product.barcode) {
                    current.copy(
                        betterAlternatives = alternatives,
                        isLoadingAlternatives = false
                    )
                } else {
                    current
                }
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
            _uiState.update {
                it.copy(
                    compareLeftProduct = left,
                    compareRightProduct = right
                )
            }
        }
    }

    fun selectCompareProduct(isLeft: Boolean, product: Product) {
        _uiState.update {
            if (isLeft) it.copy(compareLeftProduct = product)
            else it.copy(compareRightProduct = product)
        }
    }

    fun swapCompareProducts() {
        _uiState.update { current ->
            current.copy(
                compareLeftProduct = current.compareRightProduct,
                compareRightProduct = current.compareLeftProduct
            )
        }
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
            val profile = userProfile.value
            val warnings = HealthAnalyzer.evaluate(product, profile)
            SafetyNotificationManager.sendDietaryAlertNotification(
                context = getApplication(),
                product = product,
                warnings = warnings
            )
            val updatedOpsCount = preferencesRepository.incrementOperationsCount()
            val triggerMilestone = profile.copy(successfulOperationsCount = updatedOpsCount).shouldTriggerMilestonePrompt
            if (triggerMilestone) {
                preferencesRepository.recordAutoPromptShown()
                _uiState.update { it.copy(showDonationDialog = true) }
            }
            // Replace IngredientAnalyzer on back stack so pressing Back returns cleanly to previous screen
            if (backStack.lastOrNull() is ScreenDestination.IngredientAnalyzer && backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
            loadProduct(product.barcode, forceRefresh = false, isNewScan = false)
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
        _uiState.update { it.copy(showDonationDialog = visible) }
    }

    fun dismissHomeBanner() {
        viewModelScope.launch {
            preferencesRepository.dismissHomeBanner()
        }
    }

    fun dismissAutoPromptDialog() {
        viewModelScope.launch {
            preferencesRepository.dismissAutoPromptDialog()
            _uiState.update { it.copy(showDonationDialog = false) }
        }
    }

    fun markAsSupporter(isSupporter: Boolean = true) {
        viewModelScope.launch {
            preferencesRepository.markAsSupporter(isSupporter)
            _uiState.update { it.copy(showDonationDialog = false) }
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
            _uiState.update { current ->
                if (current.selectedProduct?.barcode == product.barcode) {
                    current.copy(selectedProduct = current.selectedProduct.copy(isFavorite = newFav))
                } else {
                    current
                }
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
