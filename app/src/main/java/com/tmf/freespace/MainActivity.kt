package com.tmf.freespace

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute
import com.tmf.freespace.presentationlayer.ui.screens.MainScreen
import com.tmf.freespace.presentationlayer.viewmodels.AppSummaryScreenVM


class MainActivity : ComponentActivity() {
    private val viewModel: AppSummaryScreenVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
//                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }

        ActivityCompat.requestPermissions(  //TODO Use user-oriented permission request (see video)
            this,
            permissions,
            0
        )

        //Display the AppSummary screen  //TODO Determine true starting screen
        setContent {
            val firstScreenRoute = if (allPermissionsAreGranted(this, permissions)) NavRoute.AppSummary else NavRoute.Welcome
            MainScreen(viewModel, firstScreenRoute)
        }
    }

    private fun allPermissionsAreGranted(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true  //TODO Change to TRUE to force UI to always start on first screen.
    }
}