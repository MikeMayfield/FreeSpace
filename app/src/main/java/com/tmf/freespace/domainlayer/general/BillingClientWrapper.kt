package com.tmf.freespace.domainlayer.general

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages all interactions with the Google Play Billing library.
 * This includes starting a connection, querying products, launching purchase flows,
 * and handling purchase updates.
 *
 * @param context The application context.
 * @param coroutineScope A CoroutineScope to launch async operations.
 */
class BillingClientWrapper(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) {
    private val tag = "BillingClientWrapper"

    // Define the product IDs. These must match the IDs you create in the Google Play Console.
    object ProductIds {
        const val MONTHLY_200 = "monthly_200"  //Monthly subscription, $2.00
        const val YEARLY_2000 = "yearly_2000"  //Yearly subscription, $20.00
        const val LIFETIME_6000 = "lifetime_6000"  //Lifetime subscription, $60.00
    }

    //All product details, keyed by product ID.
    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails = _productDetails.asStateFlow()

    /**
     * Launches the billing flow for a given product ID.
     *
     * @param activity The activity to launch the billing flow from.
     * @param productId The ID of the product to purchase.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val productDetails = _productDetails.value[productId]
            ?: run {
                DLog.e(tag, "Cannot launch purchase flow, product details not found for $productId")
                return
            }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        val isSubscription = productDetails.productType == BillingClient.ProductType.SUBS

        if (isSubscription && offerToken == null) {
            DLog.e(tag, "Cannot launch subscription, no valid offer token found for $productId")
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply {
                if (isSubscription) {
                    setOfferToken(offerToken!!)
                }
            }
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }


    // Listener for purchase updates. This is where the user's action in the purchase dialog is handled.
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // The user explicitly canceled the purchase dialog.
                DLog.i(tag, "User canceled the purchase flow.")
            }

            else -> {
                // Handle other error codes.
                DLog.e(tag, "Purchase flow failed with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    // Initialize the BillingClient.
    val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()) // Required for subscriptions
        .enableAutoServiceReconnection() // Automatically handles most connection drops
        .build()

    /**
     * Starts the connection to the Google Play Billing library when class is instantiated
     */
    init {
        startConnection()
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    DLog.i(tag, "BillingClient setup finished successfully.")
                    // The client is ready. Query for products and existing purchases.
                    coroutineScope.launch {
                        queryProductDetails()
                        querySubscriptionStatus()
                    }
                } else {
                    DLog.e(tag, "BillingClient setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // The auto-reconnect feature will handle this, but you can log it.
                DLog.w(tag, "Billing service disconnected.")
            }
        })
    }

    private val queryProductDetailsParams =
        QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf<QueryProductDetailsParams.Product>(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("product_id_example")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()))
            .build()

    private fun queryProductDetails() {
        billingClient.queryProductDetailsAsync(queryProductDetailsParams) {
            billingResult,
            queryProductDetailsResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (productDetail in queryProductDetailsResult.productDetailsList) {
                        productDetails.value.plus(Pair(productDetail.productId, productDetail))
                    }

                    for (unfetchedProduct in queryProductDetailsResult.unfetchedProductList) {
                        productDetails.value.plus(Pair(unfetchedProduct.productId, unfetchedProduct))
                    }
                }
        }
    }

    /**
     * Checks for any active subscriptions or one-time purchases the user already owns.
     * This is crucial for restoring purchases and verifying status on app start.
     */
    private suspend fun querySubscriptionStatus() {
        val subsParams = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        val inAppParams = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()

        // Use the modern suspend functions and run them on an IO thread
        val (subsResult, inAppResult) = withContext(Dispatchers.IO) {
            val subs = billingClient.queryPurchasesAsync(subsParams)
            val inApp = billingClient.queryPurchasesAsync(inAppParams)
            Pair(subs, inApp)
        }

        var isSubscribed = false
        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // Check if any subscription is active
            isSubscribed = subsResult.purchasesList.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        }

        if (!isSubscribed && inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // If not subscribed, check for a lifetime purchase.
            // Note: "LIFETIME_6000" must be defined in ProductIds for this to work.
             isSubscribed = inAppResult.purchasesList.any { purchase ->
                 purchase.products.contains(ProductIds.LIFETIME_6000) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
             }
        }

        //Expose the subscription status to the UI and viewModel
        val newStatus = if (isSubscribed) HomeScreenState.SubscriptionStatus.SUBSCRIBED else HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED
        if (newStatus.toString() != PropertyBag.getString(PropertyBag.SUBSCRIPTION_STATUS)) {
            PropertyBag.setString(PropertyBag.SUBSCRIPTION_STATUS, newStatus.toString())
            DLog.i(tag, "Subscription status updated to: $newStatus")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                DLog.i(tag, "Purchase acknowledged successfully: ${purchase.orderId}")
                // Grant entitlement and update UI by re-querying status.
                coroutineScope.launch { querySubscriptionStatus() }
            } else {
                DLog.e(tag, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }
}