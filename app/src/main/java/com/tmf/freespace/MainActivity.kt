package com.tmf.freespace

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.tmf.freespace.services.CompressionService
import com.tmf.freespace.viewmodels.MainViewModel


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
        super.onCreate(savedInstanceState)

        viewModel.mediaReader = mediaReader
        enableEdgeToEdge()

        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(  //TODO Use user-oriented permission request (see video)
            this,
            permissions,
            0
        )

        //TODO Test simulated background compression service
        val compressionService = CompressionService()
        compressionService.start(this)

//        setContent {
//            ReadExternalMediaFilesAPI35Theme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    LazyColumn(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .padding(innerPadding)
//                    ) {
//                        items(viewModel.files.value.size) {
//                            MediaListItem(
//                                file = viewModel.files.value[it],
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                            )
//                        }
//                    }
//                }
//            }
//        }
    }
}