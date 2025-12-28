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
                "Turn your phone into a powerful, private memory machine. Store over <b>1,000GB of photos and videos on your 128GB phone</b> and capture life without limits.<br><br>It’s like having a phone that’s bigger on the inside than the outside. Take more photos, record more videos, and save every moment without ever running out of space.<br><br>" +
                "Put privacy first and keep your photos on your phone at all times. <b>No cloud. No servers. No data collection.</b> You stay in control, your memories stay yours, and your phone finally keeps up with the way you live and create.",
            paddingValues = paddingValues,
        ) {
            navController.navigate(NavRoute.HowDoesItWork.path)
        }
    }
}
