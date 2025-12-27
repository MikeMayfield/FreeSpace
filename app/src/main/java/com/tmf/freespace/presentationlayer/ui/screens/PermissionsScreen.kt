package com.tmf.freespace.presentationlayer.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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


@Composable
fun PermissionsScreen(navController: NavHostController, paddingValues: PaddingValues) {
    var permissionStatus by remember { mutableStateOf(mapOf<String, com.tmf.freespace.presentationlayer.ui.composables.Status>()) }
    val request = requestMultiplePermission(permissions = Permissions.allPermissions) {
        permissionStatus = it
    }

    val specialScreenIdle = 0
    val specialScreenNormalPermissions = 1
    val specialScreenPermissionDeclined = 2
    val specialScreenAskForAllAccessPermission = 3
    val specialScreenAlmostDone = 4
    var specialScreen by remember { mutableIntStateOf(specialScreenIdle) }

    val context = LocalContext.current
    val permissions = Permissions()


    //If permissions have already been granted, we must have already done the setup flow and should skip right to the AppSummary screen
    //When permissions have been granted, continue on to the next screen in the flow
    if (permissions.allPermissionsAreGranted(context)) {
        navController.popBackStack()
        PeriodicBackgroundProcessingWorker.queuePeriodicProcessing()  //Start background processing as soon as we have permissions
        navController.navigate(NavRoute.SubscriptionPromo.path)
        return
    }

    //Base of screen display is a generic text body
    GenericTextBody(
        imageID = R.drawable.permisssions,
        title = "PERMISSIONS",
        bodyHtml =
            "We need your permission to work for you.<br><br>" +
                    "FreeSpace works a lot like your cloud backup app — It needs access to all your photos and videos so it can safely optimize older ones to make room for new memories. It also needs to display a small icon on the notifications bar when processing in the background to give you more control.<br><br>" +
                    "On the next screens, tap “Allow” or “Allow All” to confirm your permission. This access is required for FreeSpace to create more memory for you.",
        paddingValues = paddingValues,
    ) {
        //onClick handler for normal screen
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

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager() ->
                specialScreenAskForAllAccessPermission

            else ->
                specialScreenAlmostDone
        }
    }

    if (specialScreen == specialScreenAlmostDone) {
        AlmostDone() {
            specialScreen = specialScreenIdle
        }
    }

    if (specialScreen == specialScreenNormalPermissions) {
        request.launch(Permissions.allPermissions.toTypedArray())
        specialScreen = specialScreenAskForAllAccessPermission
    }
    if (specialScreen == specialScreenPermissionDeclined) {
        PermissionScreenPermissionDeclined(context) {
            specialScreen = specialScreenAlmostDone
        }
    }
    if (specialScreen == specialScreenAskForAllAccessPermission) {
        PermissionScreenAskForAllAccessPermission(context) {
            specialScreen = if (!permissionStatus.allGranted()) specialScreenPermissionDeclined else specialScreenAlmostDone
        }
    }
}

@Composable
fun PermissionScreenPermissionDeclined(context: Context, onClicked
: () -> Unit) {
    AlertDialog(
        onDismissRequest = {
            onClicked()
        },
        title = {
            Text("Blocked Permission")
        },
        text = {
            Text(text = "It looks like you permanently denied a permission that FreeSpace requires to work for you. Press Grant Permissions, below, to go to the app’s settings and then grant the required permissions.\n" +
                    "\n" +
                    "Step-By-Step:\n" +
                    "On the App Info screen, click the Permissions group. Click on any permission that is shown as not allowed and change it to Always Allow. When finished, navigate back to FreeSpace and you're ready to go. " +
                    "Note that some versions of Android may be slightly different.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    context.openAppSystemSettings()
                    onClicked()
                }
            ) {
                Text("Grant Permissions")
            }
        }
    )
}

@Composable
fun PermissionScreenAskForAllAccessPermission(context: Context, onClicked: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        AlertDialog(
            onDismissRequest = {
                onClicked()
            },
            title = {
                Text("Media Management Permission")
            },
            text = {
                Text(text = "Please confirm that FreeSpace can modify your photos and videos during optimization. Press Grant Permission, below.\n" +
                        "\n" +
                        "On the Media Management Apps screen, select FreeSpace, if it isn't already selected. Then enable the 'Allow app to manage media' option and navigate back to FreeSpace. " +
                        "Note that some versions of Android may be slightly different.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Direct user to settings to enable permission
                        val activity = context
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = "package:${activity.packageName}".toUri()
                        activity.startActivity(intent)
                        onClicked()
                    }
                ) {
                    Text("Grant Permission")
                }
            }
        )
    }
}

@Composable
fun AlmostDone(onClicked: () -> Unit) {
    AlertDialog(
        onDismissRequest = {
            onClicked()
        },
        title = { Text(text = "Almost Done") },
        text = { Text(text = "Press OK to continue") },
        confirmButton = {
            TextButton(
                onClick = {
                    onClicked()
                }
            ) {
                Text("OK")
            }
        }
    )
}
