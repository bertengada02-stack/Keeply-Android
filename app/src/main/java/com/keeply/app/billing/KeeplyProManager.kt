package com.keeply.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val KEEPLY_PRO_PRODUCT_ID = "keeply_pro"

sealed interface KeeplyProPurchaseState {
    data object Idle : KeeplyProPurchaseState
    data object Purchased : KeeplyProPurchaseState
    data object Pending : KeeplyProPurchaseState
    data object Cancelled : KeeplyProPurchaseState
    data class Error(val message: String) : KeeplyProPurchaseState
}

class KeeplyProManager(context: Context) {
    private val appContext = context.applicationContext
    private val entitlementPrefs = appContext.getSharedPreferences("keeply_pro_entitlement", Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(entitlementPrefs.getBoolean("is_pro", false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _proPrice = MutableStateFlow<String?>(null)
    val proPrice: StateFlow<String?> = _proPrice.asStateFlow()

    private val _purchaseState = MutableStateFlow<KeeplyProPurchaseState>(KeeplyProPurchaseState.Idle)
    val purchaseState: StateFlow<KeeplyProPurchaseState> = _purchaseState.asStateFlow()

    private var proProductDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener { billingResult, purchases ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val proPurchases = purchases.orEmpty().filter {
                        KEEPLY_PRO_PRODUCT_ID in it.products
                    }
                    updateEntitlement(proPurchases)
                    _purchaseState.value = when {
                        proPurchases.any {
                            it.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
                        } -> KeeplyProPurchaseState.Purchased
                        proPurchases.any {
                            it.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PENDING
                        } -> KeeplyProPurchaseState.Pending
                        else -> KeeplyProPurchaseState.Idle
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    _purchaseState.value = KeeplyProPurchaseState.Cancelled
                }
                else -> {
                    _purchaseState.value = KeeplyProPurchaseState.Error(
                        billingResult.debugMessage.ifBlank { "Google Play purchase failed." }
                    )
                }
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        connect()
    }

    fun clearPurchaseState() {
        _purchaseState.value = KeeplyProPurchaseState.Idle
    }

    fun refreshEntitlement() {
        if (!billingClient.isReady) {
            connect()
            return
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                updateEntitlement(purchases)
            }
        }
    }

    private fun connect() {
        if (billingClient.isReady) {
            refreshEntitlement()
            refreshProductDetails()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    refreshEntitlement()
                    refreshProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun refreshProductDetails() {
        if (!billingClient.isReady) {
            connect()
            return
        }

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(KEEPLY_PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _proPrice.value = null
                return@queryProductDetailsAsync
            }

            val details = result.productDetailsList.firstOrNull {
                it.productId == KEEPLY_PRO_PRODUCT_ID
            }
            proProductDetails = details
            _proPrice.value = details
                ?.oneTimePurchaseOfferDetailsList
                ?.firstOrNull()
                ?.formattedPrice
        }
    }

    fun launchPurchase(activity: Activity): BillingResult? {
        if (!billingClient.isReady) {
            connect()
            return null
        }

        val details = proProductDetails ?: run {
            refreshProductDetails()
            return null
        }
        val offerToken = details.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.offerToken
            ?: return null
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, flowParams)
    }

    private fun updateEntitlement(purchases: List<com.android.billingclient.api.Purchase>) {
        val ownedPurchase = purchases.firstOrNull { purchase ->
            purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED &&
                KEEPLY_PRO_PRODUCT_ID in purchase.products
        }

        val isPro = ownedPurchase != null
        _isPro.value = isPro
        entitlementPrefs.edit().putBoolean("is_pro", isPro).apply()

        ownedPurchase
            ?.takeIf { !it.isAcknowledged }
            ?.let { purchase ->
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                ) { }
            }
    }
}
