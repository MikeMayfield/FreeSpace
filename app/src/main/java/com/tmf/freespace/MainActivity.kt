package com.tmf.freespace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tmf.freespace.domainlayer.backgroundworkers.PeriodicBackgroundProcessingWorker
import com.tmf.freespace.domainlayer.general.Permissions
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute
import com.tmf.freespace.presentationlayer.ui.screens.MainScreen
import com.tmf.freespace.presentationlayer.viewmodels.CommonViewModel


class MainActivity : ComponentActivity() {
    private val viewModel: CommonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        //Display the Welcome or AppSummary screen
        var firstScreenRoute: NavRoute = NavRoute.Welcome
        if (Permissions().allPermissionsAreGranted(this)) {
            firstScreenRoute = NavRoute.AppSummary
            PeriodicBackgroundProcessingWorker.queuePeriodicProcessing()  //Ensure that periodic processing is queued. We seem to lose it sometimes
            PeriodicBackgroundProcessingWorker.queueImmediateProcessing()  //Ensure that periodic processing is queued. We seem to lose it sometimes
        }
        setContent {
           MainScreen(viewModel, firstScreenRoute)
        }
    }
}