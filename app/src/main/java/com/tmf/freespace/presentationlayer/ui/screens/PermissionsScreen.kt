package com.tmf.freespace.presentationlayer.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.tmf.freespace.R
import com.tmf.freespace.domainlayer.backgroundworkers.PeriodicBackgroundProcessingWorker
import com.tmf.freespace.domainlayer.general.Permissions
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.composables.allGranted
import com.tmf.freespace.presentationlayer.ui.composables.openAppSystemSettings
import com.tmf.freespace.presentationlayer.ui.composables.requestMultiplePermission
import com.tmf.freespace.presentationlayer.ui.composables.someNeverAsk
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute
import kotlinx.coroutines.delay


@Composable
fun PermissionsScreen(navController: NavHostController, paddingValues: PaddingValues) {
    var permissionStatus by remember { mutableStateOf(mapOf<String, com.tmf.freespace.presentationlayer.ui.composables.Status>()) }
    val request = requestMultiplePermission(permissions = Permissions.allPermissions) {
        permissionStatus = it
    }
    val context = LocalContext.current
    val permissions = Permissions()
    var heartbeat by remember { mutableIntStateOf(0) }  //One-second heartbeat to keep the screen checking for state change
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            heartbeat++
        }
    }
    if (heartbeat < 0) {
        val heartbeatTicked = heartbeat
    }

    val specialScreenNormalPermissions = 1
    val specialScreenPermissionDeclined = 2
    val specialScreenAskForAllAccessPermission = 3
    var specialScreen by remember { mutableIntStateOf(specialScreenNormalPermissions) }
//    var priorSpecialScreen = -1


    //If permissions have already been granted, we must have already done the setup flow and should skip right to the AppSummary screen
    //When permissions have been granted, continue on to the next screen in the flow
    if (permissions.allPermissionsAreGranted(context)) {
        navController.popBackStack()
        PeriodicBackgroundProcessingWorker.queuePeriodicProcessing()  //Start background processing as soon as we have permissions
        navController.navigate(NavRoute.SubscriptionPromo.path)
        return
    }

    //Screen to show:
    //  Blocked Permission: If some normal permissions declined; else
    //  Normal Permissions: If all normal permissions are not yet granted; else
    //  AllAccess Permissions: If All_FILES_ACCESS permission is not yet granted; else
    //  Else: Navigate to next screen (since all permissions have been granted)
    specialScreen = when {
        permissionStatus.someNeverAsk() ->
            specialScreenPermissionDeclined

        !permissionStatus.allGranted() ->
            specialScreenNormalPermissions

        Build.VERSION.SDK_INT >= VERSION_CODES.R && !Environment.isExternalStorageManager() ->
            specialScreenAskForAllAccessPermission

        else ->
            specialScreenNormalPermissions
    }

    when (specialScreen) {
        //Base of UI screen when no permissions have been requested yet
        specialScreenNormalPermissions -> {
            GenericTextBody(
                imageID = R.drawable.permisssions,
                title = "PERMISSIONS",
                bodyHtml =
                    "We need your permission to work for you.<br><br>" +
                            "FreeSpace works a lot like your regular backup app — It needs access to all your photos and videos so it can safely optimize older ones to make room for new memories. It also needs to display a small icon on the notifications bar when processing in the background to give you more control.<br><br>" +
                            "On the next screens, tap <b>Allow</b> or <b>Allow All</b> to confirm your permission. This access is required for FreeSpace to create more memory for you.",
                navButtonText = "NEXT",
                paddingValues = paddingValues,
            ) {
                request.launch(Permissions.allPermissions.toTypedArray())
            }
        }

        //Permissions requested, but at least one normal permission declined
        specialScreenPermissionDeclined -> {
            GenericTextBody(
                imageID = R.drawable.missing_permission_icon,
                title = "MISSING PERMISSION",
                bodyHtml =
                    "It looks like you denied a permission that FreeSpace requires to work for you. Press GRANT PERMISSIONS, below, to go to the app’s settings and then grant the required permissions.<br><br>" +
                            "► On the App Info screen, click the Permissions group.<br>" +
                            "► Click on any permission that is shown as 'Not allowed'<br>" +
                            "► Allow the permission, e.g. <b>Always allow all</b><br>" +
                            "► When finished, navigate back to FreeSpace and you're ready to go.<br><br>" +
                            "Note that some versions of Android may be slightly different.",
                navButtonText = "GRANT PERMISSIONS",
                paddingValues = paddingValues,
            ) {
                context.openAppSystemSettings()
            }
        }

        specialScreenAskForAllAccessPermission -> {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
                GenericTextBody(
                    imageID = R.drawable.all_files_access_icon,
                    title = "MANAGE MEDIA PERMISSION",
                    bodyHtml =
                        "Please confirm that FreeSpace can manage your photos and videos during optimization. Press GRANT PERMISSION, below.<br>" +
                                "<br>" +
                                "Enable the <b>Allow app to manage media</b> option and then navigate back to FreeSpace.",
                    navButtonText = "GRANT PERMISSION",
                    paddingValues = paddingValues,
                ) {
                    // Direct user to settings to enable permission
                    val activity = context
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = "package:${activity.packageName}".toUri()
                    activity.startActivity(intent)
                }
            }
        }
    }
}
