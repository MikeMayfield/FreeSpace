package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.R.drawable
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun PermissionsScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = drawable.permisssions,
        title = "PERMISSIONS",
        bodyHtml =
            "We need your permission to work for you.<br><br>" +
                    "FreeSpace works a lot like your cloud backup app — It needs access to all your photos and videos so it can safely optimize older ones and make room for new memories.<br><br>" +
                    "On the next screen, tap “Allow All” to confirm your permission. This access is required for FreeSpace to create more memory for you.",
        paddingValues = paddingValues,
    ) {
        //        PeriodicBackgroundProcessingWorker.queuePeriodicProcessing(LocalContext.current)  //TODO enable this after getting permissions

        navController.navigate(NavRoute.SubscriptionPromo.path)
    }
}
