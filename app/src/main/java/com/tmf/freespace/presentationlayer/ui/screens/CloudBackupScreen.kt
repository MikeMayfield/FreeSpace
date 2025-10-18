package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.R
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun CloudBackupScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = R.drawable.cloud_backup,
        title = "CLOUD BACKUP",
        bodyHtml = "Most of the time, you'll never notice any change in your photo and video quality at all. But for some older pictures you haven’t looked at in ages, you might see some image degradation.<br><br>" +
                "FreeSpace works hand-in-hand with top cloud backup services like Google, Microsoft, Dropbox, and TeraBox. If you ever want to bring a photo back to its full, original quality, you can easily restore it from your automatic cloud backup.",
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.License.path)
    }
}
