//package com.tmf.freespace
//
//import android.app.Activity
//import android.content.Context
//import com.android.billingclient.api.*
//import com.tmf.freespace.datalayer.datasources.local.PropertyBag
//import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class BillingClientWrapper(
//    private val context: Context,
//    private val coroutineScope: CoroutineScope, // Pass a scope for coroutine operations
//) {
//
//    // Define your product IDs. These must match the IDs in Google Play Console.
//    object ProductIds {
//        const val MONTHLY = "MONTHLY"
//        const val YEARLY = "YEARLY"
//        const val LIFETIME = "LIFETIME"
//        const val BETA = "FREE_BETA"
//    }
//
//    // Holds the retrieved product details, keyed by product ID.
//    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
//    val productDetails = _productDetails.asStateFlow()
//
//    // Listener for purchase updates from Google Play.
//    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
//        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
//            for (purchase in purchases) {
//                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
//                    // Acknowledge new purchases to complete the transaction.
//                    acknowledgePurchase(purchase)
//                }
//            }
//            // After a purchase, re-check subscription status to update the UI
//            coroutineScope.launch {
//                querySubscriptionStatus()
//            }
//        }
//        // TODO: Handle other billing results (e.g., user cancelled, item already owned, error)
//    }
//
//    // Initialize the BillingClient.
//    private var billingClient: BillingClient = BillingClient.newBuilder(context)
//        .setListener(purchasesUpdatedListener)
//        .enablePendingPurchases() // Required for subscriptions
//        .enableAutoServiceReconnection()
//        .build()
//
//    init {
//        startConnection()
//    }
//
//    /**
//     * Starts the connection to Google Play Billing.
//     */
//    private fun startConnection() {
//        billingClient.startConnection(object : BillingClientStateListener {
//            override fun onBillingSetupFinished(billingResult: BillingResult) {
//                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                    // The BillingClient is ready. Query products and check for existing purchases.
//                    coroutineScope.launch {
//                        queryProductDetails()
//                        querySubscriptionStatus()
//                    }
//                }
//            }
//
//            override fun onBillingServiceDisconnected() {
//                // With enableAutoServiceReconnection, this is less likely to be needed for simple
//                // reconnections. You can still use it for logging or more complex retry logic.
//            }
//        })
//    }
//
//    /**
//     * Queries for product details using the modern suspend function.
//     * This should be called from a coroutine.
//     */
//    private suspend fun queryProductDetails() {
//        val productList = listOf(
//            QueryProductDetailsParams.Product.newBuilder()
//                .setProductId(ProductIds.MONTHLY)
//                .setProductType(BillingClient.ProductType.SUBS)
//                .build(),
//            QueryProductDetailsParams.Product.newBuilder()
//                .setProductId(ProductIds.YEARLY)
//                .setProductType(BillingClient.ProductType.SUBS)
//                .build(),
//            QueryProductDetailsParams.Product.newBuilder()
//                .setProductId(ProductIds.LIFETIME)
//                .setProductType(BillingClient.ProductType.INAPP) // Lifetime is a one-time purchase
//                .build(),
//            QueryProductDetailsParams.Product.newBuilder()
//                .setProductId(ProductIds.BETA)
//                .setProductType(BillingClient.ProductType.SUBS) // FREE_BETA is a subscription
//                .build(),
//        )
//
//        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
//
//        // Use the suspend function queryProductDetails
//        val productDetailsResult = withContext(Dispatchers.IO) {
//            billingClient.queryProductDetails(params.build())
//        }
//
//        // Process the result
//        if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//            productDetailsResult.productDetailsList?.let {
//                _productDetails.value = it.associateBy { details -> details.productId }
//            }
//        }
//        // TODO: Handle errors from productDetailsResult.billingResult
//    }
//
//    /**
//     * Queries for active subscriptions and one-time purchases to determine user entitlement.
//     * Updates PropertyBag with the current subscription status.
//     */
//    private suspend fun querySubscriptionStatus() {
//        // Query for active subscriptions
//        val subsParams = QueryPurchasesParams.newBuilder()
//            .setProductType(BillingClient.ProductType.SUBS)
//            .build()
//        val subsResult = withContext(Dispatchers.IO) { billingClient.queryPurchases(subsParams) }
//
//        // Query for one-time (in-app) purchases
//        val inAppParams = QueryPurchasesParams.newBuilder()
//            .setProductType(BillingClient.ProductType.INAPP)
//            .build()
//        val inAppResult = withContext(Dispatchers.IO) { billingClient.queryPurchases(inAppParams) }
//
//        // Process the results
//        var isSubscribed = false
//        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//            // Any active, purchased subscription means the user is subscribed.
//            isSubscribed = subsResult.purchasesList.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
//        }
//
//        if (!isSubscribed && inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//            // If not subscribed via recurring plan, check for a lifetime purchase.
//            isSubscribed = inAppResult.purchasesList.any { purchase ->
//                purchase.products.contains(ProductIds.LIFETIME) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
//            }
//        }
//
//        // Update the app's state based on the findings
//        if (isSubscribed) {
//            PropertyBag.setString(PropertyBag.SUBSCRIPTION_STATUS, HomeScreenState.SubscriptionStatus.SUBSCRIBED.toString())
//        } else {
//            // Only set to NOT_SUBSCRIBED if it's currently SUBSCRIBED to avoid overwriting TRIAL.
//            // A more robust solution might use a dedicated "hasCheckedEntitlements" flag.
//            if (PropertyBag.getString(PropertyBag.SUBSCRIPTION_STATUS) == HomeScreenState.SubscriptionStatus.SUBSCRIBED.toString()) {
//                PropertyBag.setString(PropertyBag.SUBSCRIPTION_STATUS, HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED.toString())
//            }
//        }
//    }
//
//
//    /**
//     * Acknowledges a purchase to finalize the transaction.
//     * Failure to acknowledge within 3 days results in a refund.
//     */
//    private fun acknowledgePurchase(purchase: Purchase) {
//        // Only acknowledge purchases that are in the "purchased" state.
//        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
//            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
//                .setPurchaseToken(purchase.purchaseToken)
//                .build()
//
//            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
//                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                    // Purchase acknowledged successfully. Grant entitlement to the user.
//                    // This is where you would update the UI and app state.
//                    coroutineScope.launch {
//                        querySubscriptionStatus() // Re-check status to confirm entitlement.
//                    }
//                }
//                // TODO: Handle acknowledgment errors.
//            }
//        }
//    }
//
//    /**
//     * Launches the billing flow for a selected product.
//     *
//     * @param activity The activity that is launching the flow.
//     * @param productId The ID of the product to purchase.
//     */
//    fun launchPurchaseFlow(activity: Activity, productId: String) {
//        val product = _productDetails.value[productId] ?: return // Exit if product details not found
//
//        // For subscriptions, you must get the offer token from the selected base plan.
//        // This example just picks the first one. A real app might have UI to select different offers.
//        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
//
//        // A subscription product must have an offer token to be purchased.
//        if (product.productType == BillingClient.ProductType.SUBS && offerToken == null) {
//            // This can happen if the product is not configured correctly in Play Console.
//            return
//        }
//
//        val productDetailsParamsList = listOf(
//            BillingFlowParams.ProductDetailsParams.newBuilder()
//                .setProductDetails(product)
//                .apply {
//                    // For subscriptions, set the offer token.
//                    if (product.productType == BillingClient.ProductType.SUBS) {
//                        setOfferToken(offerToken!!)
//                    }
//                }
//                .build()
//        )
//
//        val billingFlowParams = BillingFlowParams.newBuilder()
//            .setProductDetailsParamsList(productDetailsParamsList)
//            .build()
//
//        // Launch the billing flow
//        billingClient.launchBillingFlow(activity, billingFlowParams)
//    }
//}
