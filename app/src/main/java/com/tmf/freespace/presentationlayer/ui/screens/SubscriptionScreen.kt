package com.tmf.freespace.presentationlayer.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tmf.freespace.R
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.TRIAL_GB_FREE
import com.tmf.freespace.domainlayer.general.BillingClientWrapper.ProductIds
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.viewmodels.CommonViewModel

@Composable
fun SubscriptionScreen(navController: NavHostController, paddingValues: PaddingValues, viewModel: CommonViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val subscriptionOfferDetails = products[ProductIds.MONTHLY_200]?.subscriptionOfferDetails
    val monthlyPrice = subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$2.00"

    GenericTextBody(
        imageID = R.drawable.subscription_promo,
        title = "FreeSpace MAX",
        bodyHtml =
            "FreeSpace keeps your life simple — and your memory limitless.<br>" +
            "<br>" +
            "You already get <b>${PropertyBag.getInt(TRIAL_GB_FREE)} GB of extra space for free</b>, but when you’re ready for more, <b>FreeSpace Max</b> gives you automatic expansion and almost <b>limitless</b> memory.",
        navButtonText = "SUBSCRIBE  -  ${monthlyPrice}/Mon",
        paddingValues = paddingValues,
        onNavButtonClick = handleSubscription(viewModel, LocalActivity.current!!, navController),
    )
}

private fun handleSubscription(
    viewModel: CommonViewModel,
    activity: Activity,
    navController: NavHostController,
): () -> Unit = {
    // Launch the purchase flow for the $2/mon subscription
    if (!viewModel.launchPurchaseFlow(activity, ProductIds.MONTHLY_200)) {
        Toast.makeText(activity, "Unable to process your subscription request. You may not have Internet access or your phone may not have Google Play Services installed.", Toast.LENGTH_LONG).show()
    }

    // The result of the purchase will be handled by the PurchasesUpdatedListener.
    // We no longer need to manually set the subscription status here.
    // The popBackStack() should also be moved to the listener's success callback if you
    // want the screen to close automatically only on a successful purchase.
    // For now, we can leave it here to close the screen immediately.
    navController.popBackStack()
}