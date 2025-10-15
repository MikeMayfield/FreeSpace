package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun PermissionsScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        titleHtml = "PERMISSIONS",
        bodyHtml = "TODO: Placeholder for permissions screen",
        paddingValues = paddingValues,
    ) {
//        PeriodicBackgroundProcessingWorker.queuePeriodicProcessing(LocalContext.current)  //TODO enable this after getting permissions

        //Navigate to next screen and don't allow back navigation
        navController.navigate(NavRoute.Start.path, NavOptions.Builder()
            .setPopUpTo(navController.graph.startDestinationId, true)
            .build()
        )
    }

}
