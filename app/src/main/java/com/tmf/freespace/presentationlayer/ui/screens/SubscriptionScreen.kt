package com.tmf.freespace.presentationlayer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.BaseApplication
import com.tmf.freespace.R
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.TRIAL_GB_FREE
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState

@Composable
fun SubscriptionScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = R.drawable.subscription_promo,
        title = "FreeSpace MAX",
        bodyHtml =
            "FreeSpace keeps your life simple — and your memory limitless.<br>" +
                    "<br>" +
                    "You already get <b>${PropertyBag.getInt(TRIAL_GB_FREE)} GB of extra space for free</b>, but when you’re ready for more, <b>FreeSpace Max</b> gives you automatic expansion and almost <b>limitless</b> memory.<br>" +
                    "<br>" +
                    "Go monthly, yearly, or unlock it forever with our lifetime plan — and never worry about running out of memory again!<br>" +
                    "<br>" +
                    "&nbsp;&nbsp;&nbsp;• \$2.00/month - Billed Monthly<br>" +
                    "<br>" +
                    "&nbsp;&nbsp;&nbsp;• \$1.67/month - Billed Annually (\$20)<br>" +
                    "<br>" +
                    "&nbsp;&nbsp;&nbsp;• Lifetime Subscription (\$60)<br>",
        navButtonText = "SUBSCRIBE",
        paddingValues = paddingValues,
        onBodyClick = handleSubscription(navController),
        onNavButtonClick = handleSubscription(navController),
    )
}

private fun handleSubscription(navController: NavHostController): () -> Unit = {
    Toast.makeText(BaseApplication.instance.baseContext, "TODO: Process Play Store subscription flow. Enabling new Subscription for testing", Toast.LENGTH_SHORT).show()

    PropertyBag.setString(PropertyBag.SUBSCRIPTION_STATUS, HomeScreenState.SubscriptionStatus.SUBSCRIBED.toString())
    navController.popBackStack()
}