package com.tmf.freespace.presentationlayer.ui.composables

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController

@Composable
fun ConfirmExit(navController: NavHostController, paddingValues: PaddingValues, childComposable: @Composable () -> Unit) {
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalActivity.current

    BackHandler(enabled = true) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Confirm Exit") },
            text = { Text("Are you sure you want to exit FreeSpace?") },
            confirmButton = {
                TextButton(onClick = {
                    context?.finishAffinity() // Exit the app
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    childComposable()
}