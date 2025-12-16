package com.tmf.freespace.presentationlayer.ui.screens

import android.app.Activity
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
fun SubscriptionScreen(viewModel: CommonViewModel, navController: NavHostController, paddingValues: PaddingValues) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val subscriptionOfferDetails = products[ProductIds.MONTHLY_200]?.subscriptionOfferDetails
    val monthlyPrice = subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$2.00"

    GenericTextBody(
        imageID = R.drawable.subscription_promo,
        title = "FreeSpace MAX",
        bodyHtml =
            "FreeSpace keeps your life simple — and your memory limitless.<br>" +
                    "<br>" +
                    "You already get <b>${PropertyBag.getInt(TRIAL_GB_FREE)} GB of extra space for free</b>, but when you’re ready for more, <b>FreeSpace Max</b> gives you automatic expansion and almost <b>limitless</b> memory.<br>" +
                    "<br>" +
//                    "Go monthly, yearly, or unlock it forever with our lifetime plan — and never worry about running out of memory again!<br>" +
//                    "<br>" +
                    "&nbsp;&nbsp;&nbsp;<b>$monthlyPrice/month - Billed Monthly</b><br>",
//                    + "<br>" +
//                    "&nbsp;&nbsp;&nbsp;• \$1.67/month - Billed Annually (\$20)<br>" +
//                    "<br>" +
//                    "&nbsp;&nbsp;&nbsp;• Lifetime Subscription (\$60)<br>"
        navButtonText = "SUBSCRIBE",
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
    viewModel.launchPurchaseFlow(activity, ProductIds.MONTHLY_200)  //TODO *** Use ViewModel for launch

    // The result of the purchase will be handled by the PurchasesUpdatedListener.
    // We no longer need to manually set the subscription status here.
    // The popBackStack() should also be moved to the listener's success callback if you
    // want the screen to close automatically only on a successful purchase.
    // For now, we can leave it here to close the screen immediately.
    navController.popBackStack()
}