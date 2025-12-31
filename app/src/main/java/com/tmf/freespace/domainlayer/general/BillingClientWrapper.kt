package com.tmf.freespace.domainlayer.general

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.SUBSCRIPTION_STATUS
import com.tmf.freespace.domainlayer.general.BillingClientWrapper.ProductIds.LIFETIME_6000
import com.tmf.freespace.domainlayer.general.BillingClientWrapper.ProductIds.MONTHLY_200
import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED
import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState.SubscriptionStatus.SUBSCRIBED
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
//        const val YEARLY_2000 = "yearly_2000"  //Yearly subscription, $20.00
        const val LIFETIME_6000 = "lifetime_6000"  //Lifetime subscription, $60.00
    }

    //All product details, keyed by product ID.
    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails = _productDetails.asStateFlow()  //Public Flow of product details, reflecting any changes from Google Play
    var billingIsStarted = false

    /**
     * Launches the billing flow for a given product ID.
     *
     * @param activity The activity to launch the billing flow from.
     * @param productId The ID of the product to purchase.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String): Boolean {
        try {
            val productDetails = _productDetails.value[productId]
                ?: run {
                    DLog.e(tag, "Cannot launch purchase flow, product details not found for $productId")
                    return false
                }

            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            val isSubscription = productDetails.productType == ProductType.SUBS

            if (isSubscription && offerToken == null) {
                DLog.e(tag, "Cannot launch subscription, no valid offer token found for $productId")
                return false
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
        catch (e: Exception) {
            DLog.e(tag, "Failed to launch purchase flow: ${e.message}")
            return false
        }

        return true
    }


    // Listener for purchase updates. This is where the user's action in the purchase dialog is handled.
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }

            BillingResponseCode.USER_CANCELED -> {
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
     * Starts the connection to the Google Play Billing library
     */
    fun startConnection() {
        if (billingIsStarted) return

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    DLog.i(tag, "BillingClient setup finished successfully.")
                    billingIsStarted = true
                    // The client is ready. Query for products and existing purchases.
                    coroutineScope.launch {
                        try {
                            queryProductDetails()
                            querySubscriptionStatus()
                        }
                        catch (e: Exception) {
                            DLog.e(tag, "Error in onBillingSetupFinished: ${e.message}")
                        }
                    }
                } else {
                    DLog.e(tag, "BillingClient setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // The auto-reconnect feature will handle this, but you can log it.
                DLog.w(tag, "Billing service disconnected.")
                billingIsStarted = false
            }
        })
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    private suspend fun queryProductDetails() {
        DLog.i(tag, "Querying product details")
        if (!billingIsStarted) return

        try {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(MONTHLY_200)
                    .setProductType(ProductType.SUBS)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
            params.setProductList(productList)

            val productDetailsResult = withContext(Dispatchers.IO) {
                billingClient.queryProductDetails(params.build())
            }
            if (productDetailsResult.billingResult.responseCode == BillingResponseCode.OK && productDetailsResult.productDetailsList != null) {
                for (productDetail in productDetailsResult.productDetailsList) {
                    _productDetails.value = _productDetails.value + Pair(productDetail.productId, productDetail)
                }
            }
        }
        catch (e: Exception) {
            DLog.e(tag, "Failed to query product details: ${e.message}")
        }
    }

    /**
     * Checks for any active subscriptions or one-time purchases the user already owns.
     * This is crucial for restoring purchases and verifying status on app or service start.
     */
    suspend fun querySubscriptionStatus(onStatusChanged: () -> Unit = {}) {
        if (!billingIsStarted) return

        try {
            val subsParams = QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
            val inAppParams = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()

            // Use the modern suspend functions and run them on an IO thread
            val (subsResult, inAppResult) = withContext(Dispatchers.IO) {
                val subs = billingClient.queryPurchasesAsync(subsParams)
                val inApp = billingClient.queryPurchasesAsync(inAppParams)
                Pair(subs, inApp)
            }

            // Check if any subscription is active
            var isSubscribed = false
            if (subsResult.billingResult.responseCode == BillingResponseCode.OK) {
                isSubscribed = subsResult.purchasesList.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            }

            // If not subscribed, check for a lifetime purchase.  Note: "LIFETIME_6000" must be defined in ProductIds for this to work.
            if (!isSubscribed && inAppResult.billingResult.responseCode == BillingResponseCode.OK) {
                isSubscribed = inAppResult.purchasesList.any { purchase ->
                    purchase.products.contains(LIFETIME_6000) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
            }

            //Expose the subscription status to the UI and viewModel
            val newStatus = if (isSubscribed) SUBSCRIBED else NOT_SUBSCRIBED
            if (newStatus.toString() != PropertyBag.getString(SUBSCRIPTION_STATUS)) {
                onStatusChanged()
                DLog.i(tag, "Subscription status updated to: $newStatus")
            }
            PropertyBag.setString(SUBSCRIPTION_STATUS, newStatus.toString())
        }
        catch (e: Exception) {
            DLog.e(tag, "Failed to query subscription status: ${e.message}")
            PropertyBag.setString(SUBSCRIPTION_STATUS, NOT_SUBSCRIBED.toString())
            onStatusChanged()
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        try {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    DLog.i(tag, "Purchase acknowledged successfully: ${purchase.orderId}")
                    // Grant entitlement and update UI by re-querying status.
                    coroutineScope.launch { querySubscriptionStatus() }
                } else {
                    DLog.e(tag, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                }
            }
        }
        catch (e: Exception) {
            DLog.e(tag, "Failed to acknowledge purchase: ${e.message}")
        }
    }
}