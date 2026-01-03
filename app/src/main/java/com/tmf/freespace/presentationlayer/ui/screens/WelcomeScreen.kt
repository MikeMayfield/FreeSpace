package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.tmf.freespace.R
import com.tmf.freespace.presentationlayer.ui.composables.ConfirmExit
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WelcomeScreen(navController: NavHostController, paddingValues: PaddingValues) {
    //If permissions have already been granted, we must have already done the setup flow and should skip right to the AppSummary screen
//    if (Permissions().allPermissionsAreGranted(LocalContext.current)) {
//        navController.navigate(NavRoute.AppSummary.path)
//    }

    ConfirmExit() {
        GenericTextBody(
            imageID = R.drawable.welcome_screen,
            title = "WELCOME TO FREESPACE",
            bodyHtml =
                "<b>Your phone can now hold over a million photos and videos — right in your hand, not somewhere in the cloud.</b> Store over a terabyte of photos and videos on a 128 GB phone</b> and capture life without limits.<br><br>" +
                "It's like having a phone that's bigger on the inside than the outside. Take more photos, record more videos, and save every moment without ever running out of space.<br><br>" +
                "Put privacy first and keep your photos on your phone at all times. <b>No cloud. No servers. No data collection. No ads.</b> You stay in control, your memories stay yours, and your phone finally keeps up with the way you live and create.",
            paddingValues = paddingValues,
        ) {
            navController.navigate(NavRoute.HowDoesItWork.path)
        }
    }
}
