package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.R
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun CloudBackupScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = R.drawable.cloud_backup,
        title = "CLOUD BACKUP",
        bodyHtml =
            "Most of the time, you'll never notice any change in your photo and video quality. But for some older pictures you haven’t looked at in ages, you might see some image degradation.<br><br>" +
            "FreeSpace works hand-in-hand with your private or cloud backup service as long as you have your backup app set for automatic backup. If you ever want to restore an original photo, you can easily restore it from your automatic cloud backup.<br><br>" +
            "Don't forget that now that you can keep so many more photos on your phone, you might want more backup storage."
        ,
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.License.path)
    }
}
