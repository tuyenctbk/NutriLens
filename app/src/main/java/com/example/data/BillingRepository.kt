package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.R
import com.example.ui.components.DonationTier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    UNAVAILABLE
}

/**
 * ViewModel-scoped request emitted when the user initiates a Google Play Billing donation.
 * Collected strictly by the live hosting Activity while RESUMED so BillingClient never
 * encounters "Billing Activity requires an Activity session" or leaks an Activity reference.
 */
data class BillingLaunchRequest(
    val tier: DonationTier,
    val requestId: Long = System.nanoTime()
)

data class BillingUiState(
    val connectionState: BillingConnectionState = BillingConnectionState.DISCONNECTED,
    val localizedPrices: Map<String, String> = emptyMap(),
    val availableProductIds: Set<String> = emptySet(),
    val processingTier: DonationTier? = null,
    val isRestoring: Boolean = false
) {
    fun displayPriceFor(tier: DonationTier): String {
        return localizedPrices[tier.skuId] ?: tier.priceUsd
    }
}

sealed class BillingEvent {
    data class PurchaseSucceeded(val tier: DonationTier?) : BillingEvent()
    data object PurchasePending : BillingEvent()
    data object PurchaseCancelled : BillingEvent()
    data class RestoreResult(val restored: Boolean) : BillingEvent()
    data class BillingMessage(val messageRes: Int, val formatArg: String? = null) : BillingEvent()
    data class BillingError(val debugMessage: String) : BillingEvent()
}

/**
 * Dedicated Google Play Billing repository scoped to the ViewModel coroutine scope.
 * Holds only the Application context and never retains an Activity reference.
 * Activity instances are passed transiently per-call from a Lifecycle-aware collector
 * running on the hosting ComponentActivity in Lifecycle.State.RESUMED.
 */
class BillingRepository(
    context: Context,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener {

    private val appContext: Context = context.applicationContext
    private val productDetailsCache = ConcurrentHashMap<String, ProductDetails>()
    private val isReleased = AtomicBoolean(false)
    private val isLaunchingFlow = AtomicBoolean(false)
    private val connectionMutex = Mutex()
    private val reconnectAttempt = AtomicInteger(0)

    @Volatile
    private var connectionDeferred: CompletableDeferred<Boolean>? = null

    @Volatile
    private var pendingTier: DonationTier? = null

    private val _billingState = MutableStateFlow(BillingUiState())
    val billingState: StateFlow<BillingUiState> = _billingState.asStateFlow()

    private val _billingEvents = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    val billingEvents: SharedFlow<BillingEvent> = _billingEvents.asSharedFlow()

    private val _supporterEntitlementFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 4)
    val supporterEntitlementFlow: SharedFlow<Boolean> = _supporterEntitlementFlow.asSharedFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        if (isReleased.get()) return
        scope.launch {
            ensureConnected()
        }
    }

    /**
     * Called when the host Activity resumes so pending transactions or external Play Store
     * purchases are reconciled immediately without holding an Activity reference.
     */
    fun onHostActivityResumed() {
        if (isReleased.get()) return
        scope.launch {
            if (billingClient.isReady) {
                queryExistingPurchases(notifyUser = false)
            } else if (_billingState.value.connectionState != BillingConnectionState.CONNECTING) {
                if (ensureConnected()) {
                    queryExistingPurchases(notifyUser = false)
                }
            }
        }
    }

    /**
     * Validates that the provided Activity has a live, active window session and launches
     * Google Play Billing on the Main thread.
     */
    suspend fun launchBillingFlow(activity: Activity, tier: DonationTier) {
        if (isReleased.get()) return
        if (!isLaunchingFlow.compareAndSet(false, true)) return

        try {
            val isAtLeastStarted = (activity as? LifecycleOwner)?.lifecycle?.currentState
                ?.isAtLeast(Lifecycle.State.STARTED) ?: true

            if (!isActivitySessionValid(activity.isFinishing, activity.isDestroyed, isAtLeastStarted)) {
                _billingEvents.emit(BillingEvent.BillingMessage(R.string.billing_activity_unavailable))
                return
            }

            _billingState.update { it.copy(processingTier = tier) }
            pendingTier = tier

            val connected = ensureConnected()
            if (!connected) {
                clearProcessingState()
                _billingEvents.emit(
                    BillingEvent.BillingMessage(
                        messageRes = R.string.billing_store_sku_unavailable,
                        formatArg = tier.skuId
                    )
                )
                return
            }

            if (!productDetailsCache.containsKey(tier.skuId)) {
                queryDonationProducts()
            }

            val details = productDetailsCache[tier.skuId]
            if (details == null) {
                clearProcessingState()
                _billingEvents.emit(
                    BillingEvent.BillingMessage(
                        messageRes = R.string.billing_store_sku_unavailable,
                        formatArg = tier.skuId
                    )
                )
                return
            }

            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            val launchResult = withContext(Dispatchers.Main.immediate) {
                // Re-verify Activity session immediately before invoking Play Billing UI
                val stillStarted = (activity as? LifecycleOwner)?.lifecycle?.currentState
                    ?.isAtLeast(Lifecycle.State.STARTED) ?: true
                if (!isActivitySessionValid(activity.isFinishing, activity.isDestroyed, stillStarted)) {
                    return@withContext null
                }
                billingClient.launchBillingFlow(activity, flowParams)
            }

            if (launchResult == null) {
                clearProcessingState()
                _billingEvents.emit(BillingEvent.BillingMessage(R.string.billing_activity_unavailable))
                return
            }

            when (launchResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    // Purchase completion arrives asynchronously in onPurchasesUpdated
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    clearProcessingState()
                    _billingEvents.emit(BillingEvent.PurchaseCancelled)
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    clearProcessingState()
                    queryExistingPurchases(notifyUser = true)
                }
                else -> {
                    clearProcessingState()
                    _billingEvents.emit(
                        BillingEvent.BillingError(
                            launchResult.debugMessage.ifBlank {
                                appContext.getString(R.string.billing_error_code, launchResult.responseCode)
                            }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Google Play Billing flow", e)
            clearProcessingState()
            _billingEvents.emit(
                BillingEvent.BillingError(
                    e.localizedMessage ?: appContext.getString(R.string.billing_unknown_error)
                )
            )
        } finally {
            isLaunchingFlow.set(false)
        }
    }

    private fun clearProcessingState() {
        pendingTier = null
        _billingState.update { it.copy(processingTier = null) }
    }

    fun restorePurchases() {
        if (isReleased.get()) return
        scope.launch {
            _billingState.update { it.copy(isRestoring = true) }
            try {
                val connected = ensureConnected()
                if (!connected) {
                    _billingEvents.emit(BillingEvent.RestoreResult(restored = false))
                    return@launch
                }
                queryExistingPurchases(notifyUser = true)
            } finally {
                _billingState.update { it.copy(isRestoring = false) }
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (isReleased.get()) return
        scope.launch {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (purchases.isNullOrEmpty()) {
                        clearProcessingState()
                    } else {
                        purchases.forEach { purchase ->
                            handlePurchase(purchase, emitSuccessToast = true)
                        }
                        clearProcessingState()
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    clearProcessingState()
                    _billingEvents.emit(BillingEvent.PurchaseCancelled)
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    clearProcessingState()
                    queryExistingPurchases(notifyUser = true)
                }
                else -> {
                    clearProcessingState()
                    _billingEvents.emit(
                        BillingEvent.BillingError(
                            billingResult.debugMessage.ifBlank {
                                appContext.getString(R.string.billing_error_code, billingResult.responseCode)
                            }
                        )
                    )
                }
            }
        }
    }

    private suspend fun ensureConnected(): Boolean {
        if (isReleased.get()) return false
        if (billingClient.isReady) {
            _billingState.update { it.copy(connectionState = BillingConnectionState.CONNECTED) }
            return true
        }

        val deferred = connectionMutex.withLock {
            if (isReleased.get()) return false
            if (billingClient.isReady) {
                _billingState.update { it.copy(connectionState = BillingConnectionState.CONNECTED) }
                return true
            }
            val existing = connectionDeferred
            if (existing != null && existing.isActive) {
                existing
            } else {
                val created = CompletableDeferred<Boolean>()
                connectionDeferred = created
                _billingState.update { it.copy(connectionState = BillingConnectionState.CONNECTING) }
                try {
                    billingClient.startConnection(object : BillingClientStateListener {
                        override fun onBillingSetupFinished(billingResult: BillingResult) {
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                reconnectAttempt.set(0)
                                _billingState.update {
                                    it.copy(connectionState = BillingConnectionState.CONNECTED)
                                }
                                created.complete(true)
                                scope.launch {
                                    queryDonationProducts()
                                    queryExistingPurchases(notifyUser = false)
                                }
                            } else {
                                val unavailable = billingResult.responseCode ==
                                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ||
                                    billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
                                _billingState.update {
                                    it.copy(
                                        connectionState = if (unavailable) {
                                            BillingConnectionState.UNAVAILABLE
                                        } else {
                                            BillingConnectionState.DISCONNECTED
                                        }
                                    )
                                }
                                created.complete(false)
                            }
                        }

                        override fun onBillingServiceDisconnected() {
                            _billingState.update {
                                it.copy(connectionState = BillingConnectionState.DISCONNECTED)
                            }
                            if (!created.isCompleted) {
                                created.complete(false)
                            }
                            scheduleReconnect()
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start BillingClient connection", e)
                    _billingState.update { it.copy(connectionState = BillingConnectionState.UNAVAILABLE) }
                    created.complete(false)
                }
                created
            }
        }

        return deferred.await()
    }

    private fun scheduleReconnect() {
        if (isReleased.get()) return
        val attempt = reconnectAttempt.incrementAndGet()
        if (attempt > MAX_RECONNECT_ATTEMPTS) return
        val backoffMs = 1000L * (1 shl (attempt - 1))
        scope.launch {
            delay(backoffMs)
            if (!isReleased.get() && !billingClient.isReady) {
                ensureConnected()
            }
        }
    }

    private suspend fun queryDonationProducts() {
        if (!billingClient.isReady || isReleased.get()) return

        val productList = DonationTier.entries.map { tier ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(tier.skuId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val detailsList = suspendCancellableCoroutine<List<ProductDetails>> { cont ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (cont.isActive) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(productDetailsList)
                    } else {
                        cont.resume(emptyList())
                    }
                }
            }
        }

        if (detailsList.isNotEmpty()) {
            val prices = mutableMapOf<String, String>()
            val ids = mutableSetOf<String>()
            detailsList.forEach { details ->
                productDetailsCache[details.productId] = details
                ids.add(details.productId)
                details.oneTimePurchaseOfferDetails?.formattedPrice?.let { formatted ->
                    prices[details.productId] = formatted
                }
            }
            _billingState.update { current ->
                current.copy(
                    localizedPrices = current.localizedPrices + prices,
                    availableProductIds = current.availableProductIds + ids
                )
            }
        }
    }

    private suspend fun queryExistingPurchases(notifyUser: Boolean) {
        if (!billingClient.isReady || isReleased.get()) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val purchases = suspendCancellableCoroutine<List<Purchase>> { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
                if (cont.isActive) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(purchaseList)
                    } else {
                        cont.resume(emptyList())
                    }
                }
            }
        }

        val supporterSkus = DonationTier.entries.map { it.skuId }.toSet()
        val matchingPurchases = purchases.filter { purchase ->
            purchase.products.any { it in supporterSkus }
        }

        var restoredAny = false
        matchingPurchases.forEach { purchase ->
            if (handlePurchase(purchase, emitSuccessToast = false)) {
                restoredAny = true
            }
        }

        if (notifyUser) {
            _billingEvents.emit(BillingEvent.RestoreResult(restored = restoredAny))
        }
    }

    /**
     * Grants permanent Supporter entitlement strictly for PURCHASED transactions and consumes
     * the donation token (which also acknowledges it) so supporters can optionally tip again.
     */
    private suspend fun handlePurchase(
        purchase: Purchase,
        emitSuccessToast: Boolean
    ): Boolean {
        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                val matchedTier = DonationTier.entries.firstOrNull { tier ->
                    purchase.products.contains(tier.skuId)
                } ?: pendingTier

                val consumeResult = consumePurchase(purchase.purchaseToken)
                if (consumeResult.first) {
                    _supporterEntitlementFlow.emit(true)
                    if (emitSuccessToast) {
                        _billingEvents.emit(BillingEvent.PurchaseSucceeded(matchedTier))
                    }
                    true
                } else {
                    if (emitSuccessToast) {
                        _billingEvents.emit(
                            BillingEvent.BillingError(
                                consumeResult.second ?: appContext.getString(R.string.billing_unknown_error)
                            )
                        )
                    }
                    false
                }
            }
            Purchase.PurchaseState.PENDING -> {
                if (emitSuccessToast) {
                    _billingEvents.emit(BillingEvent.PurchasePending)
                }
                false
            }
            else -> false
        }
    }

    private suspend fun consumePurchase(purchaseToken: String): Pair<Boolean, String?> {
        if (!billingClient.isReady || isReleased.get()) {
            return false to appContext.getString(R.string.billing_status_unavailable)
        }
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        return suspendCancellableCoroutine { cont ->
            billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                if (cont.isActive) {
                    val ok = billingResult.responseCode == BillingClient.BillingResponseCode.OK ||
                        billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED
                    val errorMsg = if (ok) null else {
                        billingResult.debugMessage.ifBlank {
                            appContext.getString(R.string.billing_error_code, billingResult.responseCode)
                        }
                    }
                    cont.resume(ok to errorMsg)
                }
            }
        }
    }

    fun release() {
        if (isReleased.compareAndSet(false, true)) {
            connectionDeferred?.cancel()
            pendingTier = null
            try {
                billingClient.endConnection()
            } catch (e: Exception) {
                Log.w(TAG, "Error ending BillingClient connection", e)
            }
            _billingState.update {
                it.copy(
                    connectionState = BillingConnectionState.DISCONNECTED,
                    processingTier = null,
                    isRestoring = false
                )
            }
        }
    }

    companion object {
        private const val TAG = "BillingRepository"
        private const val MAX_RECONNECT_ATTEMPTS = 3

        /**
         * Pure lifecycle validator used before invoking BillingClient.launchBillingFlow.
         */
        fun isActivitySessionValid(
            isFinishing: Boolean,
            isDestroyed: Boolean,
            isAtLeastStarted: Boolean
        ): Boolean {
            return !isFinishing && !isDestroyed && isAtLeastStarted
        }
    }
}
