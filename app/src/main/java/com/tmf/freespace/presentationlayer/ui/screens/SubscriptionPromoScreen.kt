package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.R
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.TRIAL_GB_FREE
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun SubscriptionPromoScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = R.drawable.subscription_promo,
        title = "SUBSCRIBE ANYTIME",
        bodyHtml =
            "\uD83C\uDF89 <b>Get started with FreeSpace Lite — Totally free</b> \uD83C\uDF89\n<br><br>" +
                    "You get <b>${PropertyBag.getInt(TRIAL_GB_FREE)} GB of memory</b>, all yours, forever. No catch, no expiration, just more space right on your phone or tablet to keep your favorite moments.<br><br>" +
                    "Need more memory? Say hello to <b>FreeSpace Max</b> — the $2 upgrade that gives you up to 1,000 GB <em>(1 TB)</em> more space for all your photos and videos. No more choosing which memories to delete.<br><br>" +
                    "And the best part? It costs <b>less than a cup of coffee</b> ☕ — so you can keep snapping, saving, and smiling without limits!",
        navButtonText = "NEXT",
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.Start.path)
    }
}
