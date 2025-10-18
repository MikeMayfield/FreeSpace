package com.tmf.freespace.domainlayer.general

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class Permissions {

    fun allPermissionsAreGranted(context: Context): Boolean {
        for (permission in allPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return false//TODO true  //TODO Change to TRUE to force UI to always start on first screen.
    }

    companion object {

        val allPermissions = if (Build.VERSION.SDK_INT >= 33) {
            listOf<String>(
    //                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }
    }
}
