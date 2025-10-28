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

//        val permissions = if (Build.VERSION.SDK_INT >= 33) {
//            arrayOf(
////                Manifest.permission.READ_MEDIA_AUDIO,
//                Manifest.permission.READ_MEDIA_VIDEO,
//                Manifest.permission.READ_MEDIA_IMAGES,
//            )
//        } else {
//            arrayOf(
//                Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            )
//        }
//
//        ActivityCompat.requestPermissions(  //TODO Use user-oriented permission request (see video)
//            this,
//            permissions,
//            0
//        )

        //Display the Welcome or AppSummary screen
        var firstScreenRoute: NavRoute = NavRoute.Welcome
        if (Permissions().allPermissionsAreGranted(this)) {
            firstScreenRoute = NavRoute.AppSummary
            PeriodicBackgroundProcessingWorker.queueImmediateProcessing()  //Ensure that periodic processing is running, just in case we need to catch up with new files
        }
        setContent {
           MainScreen(viewModel, firstScreenRoute)
        }
    }

//    private fun allPermissionsAreGranted(context: Context, permissions: Array<String>): Boolean {  //TODO REMOVE THIS
//        for (permission in permissions) {
//            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
//                return false
//            }
//        }
//        return false//TODO true  //TODO Change to TRUE to force UI to always start on first screen.
//    }

//    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String?>?, @NonNull grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
//            // Check if the request was granted.
//            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission was granted, proceed with your app's logic.
//                openCamera()
//            } else {
//                // Permission was denied.
//                // You may want to show an explanation or disable the related feature.
//                Toast.makeText(this, "Camera permission was denied.", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
}