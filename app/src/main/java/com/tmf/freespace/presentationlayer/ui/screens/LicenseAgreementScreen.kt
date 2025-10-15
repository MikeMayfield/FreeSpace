package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun LicenseAgreementScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        titleHtml = "<h1>Title 1</h1>",
        bodyHtml = "This is the text <b>body</b> 1",
        onBodyClick = { navController.navigate(NavRoute.SetItForgetIt.path) },
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.SetItForgetIt.path)
    }
}
