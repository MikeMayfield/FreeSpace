package com.tmf.freespace.presentationlayer.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.TRIAL_GB_FREE
import com.tmf.freespace.domainlayer.backgroundworkers.PeriodicBackgroundProcessingWorker
import com.tmf.freespace.presentationlayer.ui.composables.ConfirmExit
import com.tmf.freespace.presentationlayer.viewmodels.CommonViewModel
import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.max

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSummaryScreen(viewModel: CommonViewModel, paddingValues: PaddingValues, navController: NavHostController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSubscribed = uiState.subscriptionStatus != HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp - paddingValues.calculateTopPadding() + paddingValues.calculateBottomPadding()
    val shortScreenHeightDp = 750.dp  //Height of screens too short to show full content
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp - paddingValues.calculateLeftPadding(LayoutDirection.Ltr) + paddingValues.calculateRightPadding(LayoutDirection.Ltr)
    val smallLayout = screenWidthDp < 370.dp

    ConfirmExit(navController, paddingValues) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f, fill = true))  //Proportionally add space between top and bottom areas

            if (screenHeightDp > shortScreenHeightDp || isSubscribed) {
                Text(
                    text = "Ever-expanding space for all your treasured photos and videos",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray,
                )
            }

            //Grow your memory promo section
            if (!isSubscribed) {
                if (screenHeightDp > shortScreenHeightDp) {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val sizeAtMaxExpansion = uiState.expansionAvailableMB + uiState.physicalMB + uiState.currentExpansionMB  //TODO uiState.expansionAvailableMB / 1000f
                val formatter = DecimalFormat("###,###,##0", DecimalFormatSymbols(Locale.getDefault()))
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Grow your ${formatter.format(uiState.physicalMB / 1_000L)} GB of actual memory to ${formatter.format(sizeAtMaxExpansion / 1000L)} GB with FreeSpace Max")
                        }
                    },
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))  //Proportionally add space between top and bottom areas

            if (screenHeightDp <= shortScreenHeightDp) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            //Storage bar section
            StorageBar(
                uiState.usedMB,
                uiState.availableNowMB,
                uiState.currentExpansionMB,
                uiState.expansionAvailableMB,
            )

            //Storage info section
            Spacer(modifier = Modifier.height(16.dp))

            StorageInfoSection(
                uiState.usedMB,
                uiState.availableNowMB,
                uiState.currentExpansionMB,
                uiState.expansionAvailableMB,
                isSubscribed,
                smallLayout
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Status: ",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = uiState.status,
                    color = Color.DarkGray,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp
                )

            }

            Spacer(modifier = Modifier.height(16.dp))

            KeepStorageFreeSection(uiState.keepFreeOptionIdx, smallLayout) { selectedOptionIdx ->
                val currentMinFreeSpaceGoalMB = PropertyBag.getLong(PropertyBag.MIN_FREE_SPACE_GOAL_MB)
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
                if (newMinFreeSpaceGoalMb > currentMinFreeSpaceGoalMB) {
                    PeriodicBackgroundProcessingWorker.queueImmediateProcessing()  //Start background processing to free up more space
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = true)) // Pushes button to bottom

            if (!isSubscribed) {
                Spacer(modifier = Modifier.height(24.dp))

                SubscribeButton(smallLayout) {
                    navController.navigate("subscription")
                }

                Spacer(modifier = Modifier.height(16.dp)) // Padding at the very bottom
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun StorageBar(usedMB: Long, availableNowMB: Long, currentExpansionMB: Long, futureExpansionMB: Long) {
    val totalMemoryMB = max(usedMB + availableNowMB + futureExpansionMB, 1)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Weight of each Box is percentage of total sizes in GB
            Box(Modifier  //Apps, etc. bar
                .weight((usedMB + 1f) / totalMemoryMB)
                .fillMaxHeight()
                .background(Color(0xFFF918F3)))
            Box(Modifier  //Available now bar
                .weight((availableNowMB + 1f) / totalMemoryMB)
                .fillMaxHeight()
                .background(Color(0xFF919090)))
            Box(Modifier  //Available now bar
                .weight((currentExpansionMB + 1f) / totalMemoryMB)
                .fillMaxHeight()
                .background(Color(0xFF02EA62)))
            Box(modifier = Modifier  //Future expansion space available bar
                    .weight((futureExpansionMB + 1f) / totalMemoryMB)
                .fillMaxHeight()
                .background(Color(0xFF00C752)),
            )
        }

    }
}


@SuppressLint("DefaultLocale")
@Composable
fun StorageInfoSection(usedMB: Long = 0, availableNowMB: Long = 0, currentExpansionMB: Long, futureExpansionMB: Long, isSubscribed: Boolean, smallLayout: Boolean) {
    val virtualAvailableKB = (availableNowMB + futureExpansionMB) * 1_000L
    val avgPhotoKB = 500
    val avgVideoKB = 20_000
    val gbFormatter = DecimalFormat("###,###,##0", DecimalFormatSymbols(Locale.getDefault()))
    val formatter = DecimalFormat("###,###,##0", DecimalFormatSymbols(Locale.getDefault()))

    Column(verticalArrangement = Arrangement.spacedBy(if (smallLayout) 12.dp else 24.dp)) {
        StorageDetailItem(
            color = Color(0xFFF918F3),
            storageAmount = "${gbFormatter.format(usedMB / 1_000f)} GB of Photos, Videos and other files",
            description = "Add ${formatter.format(virtualAvailableKB / avgPhotoKB)} more photos or ${formatter.format(virtualAvailableKB / avgVideoKB)} videos with FreeSpace Max",
            smallLayout
        )
        StorageDetailItem(
            color = Color(0xFF919090),
            storageAmount = "${gbFormatter.format(availableNowMB / 1000f)} GB free memory currently available",
            description = "More free memory will be added when needed",
            smallLayout
        )
        val storageAmountDetail = if (currentExpansionMB < 1000)
            "${gbFormatter.format(currentExpansionMB)} MB memory already added by FreeSpace"
            else "${gbFormatter.format(currentExpansionMB / 1000f)} GB memory already added by FreeSpace"
        StorageDetailItem(
            color = Color(0xFF02EA62),
            storageAmount = storageAmountDetail,
            description = "FreeSpace Lite is limited to ${PropertyBag.getInt(TRIAL_GB_FREE)} GB",
            smallLayout
        )
        StorageDetailItem(
            color = Color(0xFF01AD48),
            storageAmount = "${gbFormatter.format(futureExpansionMB / 1000f)} GB more can be added with FreeSpace Max",
            description = if (isSubscribed) "Relax - With FreeSpace Max you have all the memory you need for all your favorite photos and videos"
                else "Subscribe now and stop worrying about running out of memory forever",
            smallLayout
        )
    }
}

@Composable
fun StorageDetailItem(color: Color, storageAmount: String, description: String, smallLayout: Boolean) {
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
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = storageAmount,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (smallLayout) 12.sp else 16.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = if (smallLayout) 12.sp else 16.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun KeepStorageFreeSection(selectedOptionIdx: Int = 0, smallLayout: Boolean, onClick: (selectedOptionIdx: Int) -> Unit) {
    val textFontSize = if (smallLayout) 12.sp else 16.sp
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("2 GB", "5 GB", "10 GB", "5%", "10%")
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    selectedOptionText = options[selectedOptionIdx]

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Keep at least", fontSize = textFontSize, color = Color.Black)

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            OutlinedButton(
                onClick = {
                    expanded = true
                          },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(selectedOptionText, color = Color.Black)
                Icon(
                    painter = painterResource(id = android.R.drawable.arrow_down_float),
                    contentDescription = "Dropdown",
                    tint = Color.Black,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { idx, selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            selectedOptionText = selectionOption
                            expanded = false
                            onClick(idx)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text("of memory free", fontSize = textFontSize, color = Color.Black)
    }
}

@Composable
fun SubscribeButton(smallLayout: Boolean, onClick: () -> Unit) {
    Button(
        onClick = {
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
        modifier = Modifier
            .fillMaxWidth(if (smallLayout) 0.9f else 1f)
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "Subscribe to FreeSpace Max",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "$2.00",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
