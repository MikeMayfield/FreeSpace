package com.tmf.freespace.presentationlayer.ui.screens
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun SetItForgetItScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = com.tmf.freespace.R.drawable.video,
        titleHtml = "SET IT AND FORGET IT",
        bodyHtml = "FreeSpace works the way every app <em>should</em> — simple, effortless, and totally hands-off. Just set it up once and it takes care of the rest. Whenever your phone starts running low on memory, FreeSpace quietly steps in to make more room for your photos and videos.<br><br>" +
                "It’s kind of like magic — making your phone feel bigger on the inside than it is on the outside and removing the worry.",
        navButtonText = "Next",
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.CloudBackup.path)
    }
}