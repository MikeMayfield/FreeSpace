package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.tmf.freespace.R
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun StartScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        imageID = R.drawable.start_the_magic,
        title = "START THE MAGIC",
        bodyHtml =
            "FreeSpace will automatically optimize and expand your phone or tablet's memory whenever it starts getting full — so you’ll always have room for all your favorite photos and videos.<br><br>" +
                    "<b>NOTE</b>: <em>The first optimization may take a while, maybe even several hours. Hang tight — it’s doing a lot behind the scenes to make your phone bigger on the inside than it is on the outside.</em>",
        paddingValues = paddingValues,
    ) {
        //Navigate to next screen and don't allow back navigation
        navController.navigate(NavRoute.AppSummary.path, NavOptions.Builder()
            .setPopUpTo(navController.graph.startDestinationId, true)
            .build()
        )
    }
}
