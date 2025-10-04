package com.tmf.freespace

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.tmf.freespace.domainlayer.backgroundworkers.PeriodicBackgroundProcessingWorker
import com.tmf.freespace.presentationlayer.ui.components.SetSystemBarColors
import com.tmf.freespace.presentationlayer.ui.theme.FreeSpaceTheme
import com.tmf.freespace.presentationlayer.viewmodels.MainViewModel


class MainActivity : ComponentActivity() {
    private val mediaReader by lazy {
        MediaReader(applicationContext)
    }
    private val viewModel: MainViewModel by viewModels<MainViewModel>(
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
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        viewModel.mediaReader = mediaReader

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

        //TODO Test simulated background compression service
        queuePeriodicBackgroundWorkers(this.applicationContext)


        defineScreenLayout()
    }

    private fun queuePeriodicBackgroundWorkers(context: Context) {
        PeriodicBackgroundProcessingWorker.queuePeriodicProcessing(context)
    }

    private fun defineScreenLayout() {
        setContent {
            FreeSpaceTheme() {
                SetSystemBarColors()   // For changing system bar's colors
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text("Main UI placeholder", modifier = Modifier
                        .padding(innerPadding)
                    )
                }
            }
        }
    }
}