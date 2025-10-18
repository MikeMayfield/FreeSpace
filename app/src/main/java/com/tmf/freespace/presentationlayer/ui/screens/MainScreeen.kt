package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.tmf.freespace.R
import com.tmf.freespace.presentationlayer.ui.navigation.NavGraph
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute
import com.tmf.freespace.presentationlayer.ui.theme.FreeSpaceTheme
import com.tmf.freespace.presentationlayer.viewmodels.CommonViewModel
import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CommonViewModel, startRoute: NavRoute) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Replace with your actual app icon resource
                        Image(
                            painter = painterResource(id = R.drawable.xxxhdpi_icon),
                            contentDescription = "FreeSpace Lite Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val version = if (uiState.subscriptionStatus != HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED) "Max" else "Lite"
                        Text(
                            text = "FreeSpace $version",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        FreeSpaceTheme {
            val navController = rememberNavController()
            NavGraph(navController, startRoute, viewModel, paddingValues)
        }
    }
}
