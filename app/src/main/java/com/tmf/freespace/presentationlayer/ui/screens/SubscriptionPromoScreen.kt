package com.tmf.freespace.presentationlayer.ui.screens
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun SubscriptionPromoScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = com.tmf.freespace.R.drawable.subscription_promo,
        title = "SUBSCRIBE ANYTIME",
        bodyHtml =
            "<b>Get started with FreeSpace Lite — Totally free!</b> \uD83C\uDF89\n<br><br>" +
                    "You get <b>8 GB of memory</b>, all yours, forever. No catches, no expiration, just more space to keep your favorite moments.<br><br>" +
                    "Need more memory? Say hello to <b>FreeSpace Max</b> — the upgrade that gives you endless space for all your photos and videos. No more choosing which memories to delete.<br><br>" +
                    "And the best part? It costs <b>less than a cup of coffee</b> ☕ — so you can keep snapping, saving, and smiling without limits!",
        navButtonText = "NEXT",
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.Start.path)
    }
}