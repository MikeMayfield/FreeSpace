package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.tmf.freespace.R
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HowDoesItWorkScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = R.drawable.how_it_works,
        title = "HOW DOES IT WORK",
        bodyHtml =
            "FreeSpace uses a new, <b>proprietary, AI-designed compression</b> algorithm to actively reduce file sizes while preserving exceptional quality. Our technology analyzes your content in real time, keeping what matters most so the results look and feel original — most people <b>never notice any change at all</b>. Just more space to store more memories.<br><br>" +
            "Smaller files mean less storage used on your phone, freeing up memory for more photos and videos. Everything loads faster and works more efficiently. And, if you ever need it, the <b>original version is always available</b> for download from your backup, giving you complete control without sacrificing performance or quality.<br><br>",
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.SetItForgetIt.path)
    }
}
