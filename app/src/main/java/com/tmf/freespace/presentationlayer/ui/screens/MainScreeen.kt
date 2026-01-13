package com.tmf.freespace.presentationlayer.ui.screens

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.tmf.freespace.R
import com.tmf.freespace.domainlayer.general.FontSize
import com.tmf.freespace.presentationlayer.ui.navigation.NavGraph
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute
import com.tmf.freespace.presentationlayer.ui.theme.FreeSpaceTheme
import com.tmf.freespace.presentationlayer.viewmodels.CommonViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CommonViewModel, startRoute: NavRoute) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var versionName = "unknown"

    try {
        val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        versionName = pInfo.versionName ?: "unknown"
    }
    catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Replace with your actual app icon resource
                        Image(
                            painter = painterResource(id = R.drawable.icon_base96x96),
                            contentDescription = "FreeSpace Lite Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val max = if (uiState.isSubscribed) "Max" else ""
                        Text(
                            text = "FreeSpace $max",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            text = " v${versionName}",
                            fontSize = FontSize.Small,
                            color = Color.Gray,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.Bottom)
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
