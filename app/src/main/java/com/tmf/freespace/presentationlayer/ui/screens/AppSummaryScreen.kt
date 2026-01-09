package com.tmf.freespace.presentationlayer.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.domainlayer.backgroundworkers.PeriodicBackgroundProcessingWorker
import com.tmf.freespace.domainlayer.general.Const
import com.tmf.freespace.domainlayer.general.DLog
import com.tmf.freespace.presentationlayer.ui.composables.ConfirmExit
import com.tmf.freespace.presentationlayer.ui.composables.DynamicButton
import com.tmf.freespace.presentationlayer.viewmodels.CommonViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSummaryScreen(navController: NavHostController, paddingValues: PaddingValues, viewModel: CommonViewModel) {
    val tag = "AppSummaryScreen"
    val mbToGb = 1_000L
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    ConfirmExit() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
                .padding(24.dp),  //Apply left/right border padding to the whole screen
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (scrollState.canScrollBackward) {
                HorizontalDivider()
            }

            // This inner Column holds all the scrollable content
            Column(
                modifier = Modifier
                    .weight(1f) // Takes up all available space, pushing the button to the bottom
                    .verticalScroll(scrollState), // Makes this section scrollable
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val formatter = DecimalFormat("###,###,##0", DecimalFormatSymbols(Locale.getDefault()))
                val uncompressedMB = min(uiState.uncompressedMB, uiState.physicalMB)

                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Grow your ${formatter.format(uiState.physicalMB / mbToGb)} GB of built-in SD memory to ${
                                formatter.format((uiState.currentExpansionMB + max(uiState.expansionAvailableMB,
                                    0)) / mbToGb)
                            } GB with FreeSpace Max")
                        }
                    },
                    fontSize = Const.FontSizeH1,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Const.SpacerHeightExtra))

                //Storage bar section
                StorageBar(
                    uncompressedMB,
                    uiState.currentExpansionMB,
                    uiState.freeMemoryMB,
                    uiState.expansionAvailableMB
                )

                //Storage info section
                Spacer(modifier = Modifier.height(Const.SpacerHeightExtra))

                StorageInfoSection(
                    uiState.physicalMB,
                    uncompressedMB,
                    uiState.currentExpansionMB,
                    uiState.freeMemoryMB,
                    uiState.expansionAvailableMB,
                    uiState.isSubscribed
                )

                Spacer(modifier = Modifier.height(Const.SpacerHeightDivision))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Status: ",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = Const.FontSizeBody
                    )
                    Text(
                        text = uiState.status,
                        color = Color.DarkGray,
                        fontStyle = FontStyle.Italic,
                        fontSize = Const.FontSizeBody
                    )

                }

                Spacer(modifier = Modifier.height(Const.SpacerHeightExtra))

                KeepStorageFreeSection(uiState.keepFreeOptionIdx) { selectedOptionIdx ->
                    val newMinFreeSpaceGoalMb: Long = when (selectedOptionIdx) {
                        0 -> 2_000L  //2GB
                        1 -> 5_000L  //5GB
                        2 -> 10_000L  //10GB
                        3 -> (uiState.physicalMB * 0.05f).toLong()  //5%
                        4 -> (uiState.physicalMB * 0.10f).toLong()  //10%
                        else -> 2_000L
                    }
                    PropertyBag.setLong(PropertyBag.MIN_FREE_SPACE_GOAL_MB, newMinFreeSpaceGoalMb)
                    viewModel.updateKeepFreeOptionIdx(selectedOptionIdx)
                    DLog.d(tag, "Starting background processing in case we need to free up space or stop processing against a larger size goal")
                    PeriodicBackgroundProcessingWorker.queueImmediateProcessing()  //Start background processing to free up more space
                }
            }

            //Bottom navigation button (This is outside the scrollable Column)
            Spacer(modifier = Modifier.height(Const.SpacerHeightExtra))
            if (scrollState.canScrollForward) {
                HorizontalDivider()
            }
            Spacer(modifier = Modifier.height(Const.SpacerHeightDefault))

            if (!uiState.isSubscribed) {
                DynamicButton("Subscribe to FreeSpace Max") {
                    navController.navigate("subscription")
                }
            }
            else {
                val context = LocalContext.current
                ManageSubscriptionButton() {
                    try {
                        val uri = "https://play.google.com/store/account/subscriptions".toUri()
                        val webIntent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(webIntent)
                    } catch(e: Exception) {
                        DLog.e(tag, "Failed to open subscription page: ${e.message}")
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun StorageBar(uncompressedMB: Long, expandedMB: Long, availableNowMB: Long, futureExpansionMB: Long) {
    val totalMemoryMB = max((uncompressedMB + expandedMB + availableNowMB + futureExpansionMB).toFloat(), 1f)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Weight of each Box is percentage of total sizes in GB
            Box(Modifier  //Apps, etc. bar (up to total without FreeSpace)
                .weight(max(uncompressedMB / totalMemoryMB, 0.0001f))
                .fillMaxHeight()
                .background(Color(0xFFFA79F6)))
            Box(Modifier  //Current expansion bar
                .weight(max(expandedMB / totalMemoryMB, 0.0001f))
                .fillMaxHeight()
                .background(Color(0xFFFA03F3)))
            Box(Modifier  //Available now bar
                .weight(max(availableNowMB / totalMemoryMB, 0.0001f))
                .fillMaxHeight()
                .background(Color(0xFFACFACC)))
            Box(Modifier  //Future expansion space available bar
                .weight(max(futureExpansionMB / totalMemoryMB, 0.0001f))
                .fillMaxHeight()
                .background(Color(0xFF00C752)),
            )
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
fun StorageInfoSection(physicalMB: Long, uncompressedMB: Long = 0, expandedMB: Long, freeMemoryMB: Long = 0, futureExpansionMB: Long, isSubscribed: Boolean) {
    val mbToGb = 1000L
    val formatter = DecimalFormat("###,###,##0", DecimalFormatSymbols(Locale.getDefault()))

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        //Physical SD memory usage
        val physicalMBUsed = min(uncompressedMB, physicalMB)
        StorageDetailItem(
            color = Color(0xFFFA79F6),
            storageAmount = "${if (physicalMBUsed >= physicalMB) "All" else "${formatter.format(min(uncompressedMB, physicalMB) / 1_000f)} GB"} of your ${formatter.format(physicalMB / mbToGb)} GB built-in SD memory used",
            description = if (uncompressedMB < physicalMB)
                "Your built-in memory still has room for more photos and videos"
            else
                "All of your built-in memory has been used, but FreeSpace will automatically add more when needed"
        )

        //Expanded SD memory
        val storageAmountDetail = if (expandedMB < mbToGb)
                "${formatter.format(expandedMB)} MB more SD memory was added by FreeSpace" + if (isSubscribed) " Max" else ""
            else
                "${formatter.format(expandedMB / mbToGb)} GB more SD memory was added by FreeSpace" + if (isSubscribed) " Max" else ""
        val description = if (!isSubscribed)
            "FreeSpace Lite will add up to 10 GB"
        else
            ""
        StorageDetailItem(
            color = Color(0xFFFA03F3),
            storageAmount = storageAmountDetail,
            description = description
        )

        //Free physical SD memory
        StorageDetailItem(
            color = Color(0xFFACFACC),
            storageAmount = "${formatter.format(freeMemoryMB / mbToGb)} GB of built-in SD memory is currently free",
            description = "More free memory will be added when needed"
        )

        //Amount the SD memory can still be expanded
        StorageDetailItem(
            color = Color(0xFF00C752),
            storageAmount = "${formatter.format(max(futureExpansionMB / mbToGb, 0L))} GB more free memory can be added with FreeSpace Max",
            description = if (isSubscribed) if (futureExpansionMB > mbToGb) "Relax - With FreeSpace Max you have all the memory you need for all your favorite photos and videos" else "FreeSpace has added as much memory as possible."
                else "Subscribe to FreeSpace Max now and stop worrying about running out of memory forever"
        )
    }
}

@Composable
fun StorageDetailItem(color: Color, storageAmount: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(Const.SpacerHeightExtra))
        Column {
            Text(
                text = storageAmount,
                fontWeight = FontWeight.SemiBold,
                fontSize = Const.FontSizeBody,
                color = Color(0xFF010373)
            )
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = Const.FontSizeBody,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun KeepStorageFreeSection(selectedOptionIdx: Int = 0, onClick: (selectedOptionIdx: Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("2 GB", "5 GB", "10 GB", "5%", "10%")
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    selectedOptionText = options[selectedOptionIdx]

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Keep at least", fontSize = Const.FontSizeBody, color = Color.Black)

        Spacer(modifier = Modifier.width(Const.SpacerHeightDefault))

        Box {
            OutlinedButton(
                onClick = {
                    expanded = true
                          },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(),
                modifier = Modifier.height(36.dp).align(Alignment.Center)
            ) {
                Text(
                    text = selectedOptionText,
                    color = Color.Black,
                    fontSize = 3.em,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Icon(
                    painter = painterResource(id = android.R.drawable.arrow_down_float),
                    contentDescription = "Dropdown",
                    tint = Color.Black,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { idx, selectionOption ->
                    DropdownMenuItem(
                        text = { Text(
                            text = selectionOption,
                            fontSize = Const.FontSizeBody,
                            color = Color.Black
                        ) },
                        onClick = {
                            selectedOptionText = selectionOption
                            expanded = false
                            onClick(idx)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(Const.SpacerHeightDefault))

        Text("of memory free", fontSize = Const.FontSizeBody, color = Color.Black)
    }
}

@Composable
fun ManageSubscriptionButton(onClick: () -> Unit) {
    TextButton(
        onClick = {
            onClick()
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
    ) {
        Text(
            text = "Manage Subscription",
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = Const.FontSizeBody
        )
    }
}
