package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.presentationlayer.ui.components.ConfirmExit
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun WelcomeScreen(navController: NavHostController, paddingValues: PaddingValues) {
    ConfirmExit(navController, paddingValues) {
        GenericTextBody(
            imageID = com.tmf.freespace.R.drawable.welcome_screen,
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
