package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HealthAnalyzer
import com.example.data.PantryRepository
import com.example.data.ProductRepository
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.model.HealthWarning
import com.example.model.PantryItem
import com.example.model.Product
import com.example.model.UserProfile
import com.example.model.WarningSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Scanner : ScreenDestination()
    object History : ScreenDestination()
    object Pantry : ScreenDestination()
    object Dashboard : ScreenDestination()
    object Profile : ScreenDestination()
    data class ProductDetail(val barcode: String) : ScreenDestination()
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
    private val repository = ProductRepository(database.productDao())
    private val pantryRepository = PantryRepository(database.pantryDao())
    private val preferencesRepository = UserPreferencesRepository(application)

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

    fun navigateTo(destination: ScreenDestination) {
        _uiState.value = _uiState.value.copy(
            currentDestination = destination,
            errorMessage = null
        )
    }

    fun navigateBack() {
        _uiState.value = _uiState.value.copy(
            currentDestination = ScreenDestination.Scanner,
            errorMessage = null
        )
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
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            currentDestination = ScreenDestination.ProductDetail(barcode)
        )

        viewModelScope.launch {
            repository.getProductFlow(barcode, forceRefresh).collect { result ->
                result.onSuccess { product ->
                    val profile = userProfile.value
                    val warnings = HealthAnalyzer.evaluate(product, profile)

                    // Trigger system notification alert if critical dietary warnings exist
                    com.example.notification.SafetyNotificationManager.sendDietaryAlertNotification(
                        context = getApplication(),
                        product = product,
                        warnings = warnings
                    )

                    // Automatically add scanned product to pantry management list
                    pantryRepository.addProductToPantry(product)

                    // Increment verified operations counter for satisfaction triggers
                    preferencesRepository.incrementOperationsCount()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedProduct = product,
                        productWarnings = warnings,
                        errorMessage = null
                    )
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to find product data"
                    )
                }
            }
        }
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

    fun seedSampleScans() {
        viewModelScope.launch {
            repository.seedSampleScans()
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

    fun seedSamplePantry() {
        viewModelScope.launch {
            pantryRepository.seedSamplePantry()
        }
    }

    // Profile updates
    fun toggleAllergen(allergen: String, isMonitored: Boolean) {
        viewModelScope.launch {
            preferencesRepository.toggleAllergen(allergen, isMonitored)
            reEvaluateCurrent()
        }
    }

    fun setVegan(isVegan: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVegan(isVegan)
            reEvaluateCurrent()
        }
    }

    fun setVegetarian(isVegetarian: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVegetarian(isVegetarian)
            reEvaluateCurrent()
        }
    }

    fun setGlutenFree(isGlutenFree: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setGlutenFree(isGlutenFree)
            reEvaluateCurrent()
        }
    }

    fun addCustomKeyword(keyword: String) {
        viewModelScope.launch {
            preferencesRepository.addCustomKeyword(keyword)
            reEvaluateCurrent()
        }
    }

    fun removeCustomKeyword(keyword: String) {
        viewModelScope.launch {
            preferencesRepository.removeCustomKeyword(keyword)
            reEvaluateCurrent()
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
            reEvaluateCurrent()
        }
    }

    fun setAvoidPalmOil(avoid: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAvoidPalmOil(avoid)
            reEvaluateCurrent()
        }
    }

    fun setAvoidNova4(avoid: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAvoidNova4(avoid)
            reEvaluateCurrent()
        }
    }

    fun setAvoidHighRiskAdditives(avoid: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAvoidHighRiskAdditives(avoid)
            reEvaluateCurrent()
        }
    }

    private fun reEvaluateCurrent() {
        _uiState.value.selectedProduct?.let { prod ->
            val warnings = HealthAnalyzer.evaluate(prod, userProfile.value)
            _uiState.value = _uiState.value.copy(productWarnings = warnings)
        }
    }
}
