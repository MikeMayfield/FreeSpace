package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.tmf.freespace.R
import com.tmf.freespace.domainlayer.general.Permissions
import com.tmf.freespace.presentationlayer.ui.composables.ConfirmExit
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WelcomeScreen(navController: NavHostController, paddingValues: PaddingValues) {
    //If permissions have already been granted, we must have already done the setup flow and should skip right to the AppSummary screen
    if (Permissions().allPermissionsAreGranted(LocalContext.current)) {
        navController.navigate(NavRoute.AppSummary.path)
    }

    ConfirmExit(navController, paddingValues) {
        GenericTextBody(
            imageID = R.drawable.welcome_screen,
            title = "WELCOME TO FREESPACE",
            bodyHtml =
                "Picture this: you’re on that once-in-a-lifetime trip. Every view is breathtaking, every moment worth capturing — but suddenly, your phone flashes that dreaded message: “<b>Storage Full</b>.” Now you’re stuck deleting old memories just to make room for new ones.<br><br>" +
                        "Not anymore.<br><br>" +
                        "With FreeSpace, you’ll always have room for every photo, every video, every memory. No more deleting. No more “Storage Full” alerts. Just endless space for the moments that matter most.<br>",
            paddingValues = paddingValues,
        ) {
            navController.navigate(NavRoute.SetItForgetIt.path)
        }
    }
}
