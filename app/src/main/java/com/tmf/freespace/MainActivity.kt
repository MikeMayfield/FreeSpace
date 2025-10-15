package com.tmf.freespace

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.tmf.freespace.domainlayer.backgroundworkers.PeriodicBackgroundProcessingWorker
import com.tmf.freespace.presentationlayer.ui.screens.MainScreen
import com.tmf.freespace.presentationlayer.viewmodels.AppSummaryScreenVM


class MainActivity : ComponentActivity() {
    private val viewModel: AppSummaryScreenVM by viewModels()

//    private val viewModel: MainViewModel by viewModels<MainViewModel>(
//        factoryProducer = {
//            object : ViewModelProvider.Factory {
//                override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
//                        return MainViewModel(mediaReader) as T
//                    }
//                    throw IllegalArgumentException("Unknown ViewModel class")
//                }
//            }
//        }
//    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
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
            MainScreen(viewModel)
        }

        //Start background compression processing, if needed
//        queuePeriodicBackgroundWorkers(this.applicationContext)  //TODO enable this after getting permissions
    }

    private fun queuePeriodicBackgroundWorkers(context: Context) {
        PeriodicBackgroundProcessingWorker.queuePeriodicProcessing(context)
    }
}