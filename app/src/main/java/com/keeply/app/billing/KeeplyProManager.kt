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

class KeeplyProManager(context: Context) {
    private val appContext = context.applicationContext

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener { _, purchases ->
            if (purchases != null) updateEntitlement(purchases)
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
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    refreshEntitlement()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun updateEntitlement(purchases: List<com.android.billingclient.api.Purchase>) {
        val ownedPurchase = purchases.firstOrNull { purchase ->
            purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED &&
                KEEPLY_PRO_PRODUCT_ID in purchase.products
        }

        _isPro.value = ownedPurchase != null

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
