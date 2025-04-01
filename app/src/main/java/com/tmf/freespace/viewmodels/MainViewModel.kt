package com.tmf.freespace.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.tmf.freespace.MediaReader
import com.tmf.freespace.models.MediaFile

class MainViewModel(
): ViewModel() {

    var mediaReader: MediaReader? = null
        set(value) {
            if (mediaReader == null) {
                field = value
            }
        }

    val files : MutableState<List<MediaFile>> = mutableStateOf(emptyList())

//    fun getAllMediaFilesAsync() {  //TODO Is this needed for this project?
//        viewModelScope.launch(Dispatchers.IO) {
//            files.value = mediaReader?.getAllMediaFiles() ?: emptyList()
//        }
//    }
}