package com.tmf.freespace.presentationlayer.ui.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
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
    var showErrorDialog by remember { mutableStateOf(false) }
    val activity = LocalActivity.current!!

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Text(
                    text = "Subscription Error",
                    fontWeight = FontWeight.Bold
                )
                    },
            text = { Text("Unable to process your subscription request. You may not have Internet access or your phone may not have Google Play Services installed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        navController.popBackStack()
                    }
                ) {
                    Text("Close")
                }
            },
        )
    }

    GenericTextBody(
        imageID = R.drawable.subscription_promo,
        title = "FreeSpace MAX",
        bodyHtml =
            "FreeSpace keeps your life simple — and your memory limitless.<br>" +
            "<br>" +
            "You already get <b>${PropertyBag.getInt(TRIAL_GB_FREE)} GB of extra space for free</b>, but when you’re ready for more, <b>FreeSpace Max</b> gives you automatic expansion and almost <b>limitless</b> memory.",
        navButtonText = "SUBSCRIBE  -  ${monthlyPrice}/Mon",
        paddingValues = paddingValues,
        onNavButtonClick = {
            if (!viewModel.launchPurchaseFlow(activity, ProductIds.MONTHLY_200)) {
                showErrorDialog = true
            }
            else {
                navController.popBackStack()
            }
        }
    )
}
