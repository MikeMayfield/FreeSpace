package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun PermissionsScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        title = "PERMISSIONS",
        bodyHtml = "TODO: Placeholder for permissions screen",
        paddingValues = paddingValues,
    ) {
        //        PeriodicBackgroundProcessingWorker.queuePeriodicProcessing(LocalContext.current)  //TODO enable this after getting permissions

        navController.navigate(NavRoute.SubscriptionPromo.path)
    }
}
