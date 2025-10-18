package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.R
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun LicenseScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = R.drawable.license_agreement,
        title = "LICENSE",
        bodyHtml = "Like all apps, FreeSpace comes with a <u><font color=blue>License Agreement</font></u> — basically, it’s there to outline how we work together. It clearly states that we never collect or share your personal information with anyone. Your privacy stays 100% yours.<br><br>" +
                "You’ll also find the usual legal bits about things like intellectual property, warranties, and liability. Nothing surprising — just the standard stuff to keep everything clear and transparent.",
        onBodyClick = { navController.navigate(NavRoute.LicenseAgreement.path) },
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.Permissions.path)
    }
}