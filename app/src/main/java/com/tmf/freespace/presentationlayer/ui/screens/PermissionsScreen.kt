package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
    var permissionStatus by remember { mutableStateOf(mapOf<String,com.tmf.freespace.presentationlayer.ui.composables.Status>()) }
    val request = requestMultiplePermission(
        permissions = Permissions.allPermissions ) {
            permissionStatus = it
        }
    var showSettingsDialogStep by remember { mutableStateOf(0) }
    val context = LocalContext.current

    //If permissions have already been granted, we must have already done the setup flow and should skip right to the AppSummary screen
    //When permissions have been granted, continue on to the next screen in the flow
    if (permissionStatus.allGranted()) {
        navController.popBackStack()
        PeriodicBackgroundProcessingWorker.queuePeriodicProcessing()  //Start background processing as soon as we have permissions
        navController.navigate(NavRoute.SubscriptionPromo.path)
        return
    }

//    LaunchedEffect(shouldAskForPermissions) {
//        if (shouldAskForPermissions) {
//            request.launch(Permissions.allPermissions.toTypedArray())
////            val context = LocalContext.current
////                delay(1000)
////                if (permissionStatus == Status.DENIED_WITH_NEVER_ASK && (LocalActivity.current?.hasWindowFocus() == true)) {
////                    context.openAppSystemSettings()
////                }
////            }
//        }
//    }

    GenericTextBody(
        imageID = R.drawable.permisssions,
        title = "PERMISSIONS",
        bodyHtml =
            "We need your permission to work for you.<br><br>" +
                    "FreeSpace works a lot like your cloud backup app — It needs access to all your photos and videos so it can safely optimize older ones and make room for new memories. It also needs to display a small icon on the notifications bar when processing in the background to give you more control.<br><br>" +
                    "On the next screens, tap “Allow” or “Allow All” to confirm your permission. This access is required for FreeSpace to create more memory for you.",
        paddingValues = paddingValues,
    ) {
        if (permissionStatus.someNeverAsk()) {
            showSettingsDialogStep = 1
        }
        else {
            request.launch(Permissions.allPermissions.toTypedArray())
        }
    }

    if (showSettingsDialogStep > 0) {
        when (showSettingsDialogStep) {
            1 -> {
                AlertDialog(
                    onDismissRequest = { showSettingsDialogStep = 0 },
                    title = {
                        Text("Blocked Permission")
                    },
                    text = {
                        Text(text = "It looks like FreeSpace doesn’t have all the access it needs yet. To help FreeSpace free up more space, please go to the app’s Settings and grant all of the required permissions.\n" +
                            "\n" +
                            "Without these permissions, FreeSpace won’t be able to create extra memory for you.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSettingsDialogStep = 2
                                context.openAppSystemSettings()
                            }
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                )
            }
            else -> {
                AlertDialog(
                    onDismissRequest = { showSettingsDialogStep = 0 },
                    title = { Text(text = "Blocked Permission") },
                    text = { Text(text = "Press OK to continue") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSettingsDialogStep = 0
                            }
                        ) {
                            Text("OK")
                        }
                    }
                )
            }
        }

    }
}
