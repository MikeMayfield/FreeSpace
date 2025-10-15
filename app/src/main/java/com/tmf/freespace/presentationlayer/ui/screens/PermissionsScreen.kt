package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun PermissionsScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        titleHtml = "<h1>Title 1</h1>",
        bodyHtml = "Placeholder for permissions screen",
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.Start.path)
    }
}
