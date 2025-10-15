package com.tmf.freespace.presentationlayer.ui.screens
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.presentationlayer.ui.components.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun SetItForgetItScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        titleHtml = "<h1>Title 2</h1>",
        bodyHtml = "This is the <ul>text body</ul> 2",
        navButtonText = "Next",
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.CloudBackup.path)
    }
}