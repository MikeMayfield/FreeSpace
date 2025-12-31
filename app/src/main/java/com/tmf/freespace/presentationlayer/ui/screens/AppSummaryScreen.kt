package com.tmf.freespace.presentationlayer.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.domainlayer.backgroundworkers.PeriodicBackgroundProcessingWorker
import com.tmf.freespace.domainlayer.general.DLog
import com.tmf.freespace.presentationlayer.ui.composables.ConfirmExit
import com.tmf.freespace.presentationlayer.viewmodels.CommonViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSummaryScreen(viewModel: CommonViewModel, paddingValues: PaddingValues, navController: NavHostController) {
    val tag = "AppSummaryScreen"
    val mbToGb = 1000
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp - paddingValues.calculateTopPadding() + paddingValues.calculateBottomPadding()
    val shortScreenHeightDp = 750.dp  //Height of screens too short to show full content
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp - paddingValues.calculateLeftPadding(LayoutDirection.Ltr) + paddingValues.calculateRightPadding(LayoutDirection.Ltr)
    val smallLayout = screenWidthDp < 370.dp

    ConfirmExit(navController, paddingValues) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val formatter = DecimalFormat("###,###,##0", DecimalFormatSymbols(Locale.getDefault()))
            val uncompressedMB = min(uiState.uncompressedMB, uiState.physicalMB)

            Spacer(modifier = Modifier.weight(1f, fill = true))  //Proportionally add space between top and bottom areas

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Grow your ${formatter.format(uiState.physicalMB / mbToGb)} GB of physical memory to ${formatter.format(uiState.expansionAvailableMB / 1000L)} GB with FreeSpace Max")
                    }
                },
                fontSize = 24.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f, fill = true))  //Proportionally add space between top and bottom areas

            if (screenHeightDp <= shortScreenHeightDp) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            //Storage bar section
            StorageBar(
                uncompressedMB,
                uiState.currentExpansionMB,
                uiState.availableNowMB,
                uiState.expansionAvailableMB
            )

            //Storage info section
            Spacer(modifier = Modifier.height(16.dp))

            StorageInfoSection(
                uiState.physicalMB,
                uncompressedMB,
                uiState.currentExpansionMB,
                uiState.availableNowMB,
                uiState.expansionAvailableMB,
                uiState.isSubscribed,
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

            Spacer(modifier = Modifier.weight(1f, fill = true)) // Pushes button to bottom

            Spacer(modifier = Modifier.height(24.dp))

            if (!uiState.isSubscribed) {
                SubscribeButton(smallLayout) {
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

            Spacer(modifier = Modifier.height(16.dp)) // Padding at the very bottom
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun StorageBar(uncompressedMB: Long, expandedMB: Long, availableNowMB: Long, futureExpansionMB: Long) {
    val totalMemoryMB = max(uncompressedMB + expandedMB + availableNowMB + futureExpansionMB, 1)
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
                .weight((uncompressedMB + 1f) / totalMemoryMB)
                .fillMaxHeight()
                .background(Color(0xFFFA79F6)))
            Box(Modifier  //Available now bar
                .weight((expandedMB + 1f) / totalMemoryMB)
                .fillMaxHeight()
                .background(Color(0xFFFA03F3)))
            Box(Modifier  //Available now bar
                .weight((availableNowMB + 1f) / totalMemoryMB)
                .fillMaxHeight()
                .background(Color(0xFFACFACC)))
            Box(Modifier  //Future expansion space available bar
                .weight((futureExpansionMB + 1f) / totalMemoryMB)
                .fillMaxHeight()
                .background(Color(0xFF00C752)),
            )
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
fun StorageInfoSection(physicalMB: Long, uncompressedMB: Long = 0, expandedMB: Long, availableNowMB: Long = 0, futureExpansionMB: Long, isSubscribed: Boolean, smallLayout: Boolean) {
    val mbToGb = 1000L
    val formatter = DecimalFormat("###,###,##0", DecimalFormatSymbols(Locale.getDefault()))
    val totalExpansionMB = expandedMB + futureExpansionMB

    Column(verticalArrangement = Arrangement.spacedBy(if (smallLayout) 12.dp else 24.dp)) {

        StorageDetailItem(
            color = Color(0xFFFA79F6),
            storageAmount = "${formatter.format(min(uncompressedMB, physicalMB) / 1_000f)} GB of ${formatter.format(physicalMB / mbToGb)} GB physical memory used",
            description = if (uncompressedMB < physicalMB)
                "Your physical memory still has room for more photos and videos"
            else
                "All of your physical memory has been used, but will automatically expand with FreeSpace",
            smallLayout
        )

        val storageAmountDetail = if (expandedMB < mbToGb)
                "${formatter.format(expandedMB)} MB of memory added by FreeSpace" + if (isSubscribed) " Max" else ""
            else
                "${formatter.format(expandedMB / mbToGb)} GB of memory added by FreeSpace" + if (isSubscribed) " Max" else ""
        val description = if (uncompressedMB < physicalMB)
            "FreeSpace Lite will add up to 10 GB"
        else
            "FreeSpace Lite reached its 10 GB limit. FreeSpace Max removes this limit"
        StorageDetailItem(
            color = Color(0xFFFA03F3),
            storageAmount = storageAmountDetail,
            description = description,
            smallLayout
        )

        StorageDetailItem(
            color = Color(0xFFACFACC),
            storageAmount = "${formatter.format(availableNowMB / mbToGb)} GB of physical free memory currently available",
            description = "More free memory will be added when needed",
            smallLayout
        )

        val storageAmountGB  = (futureExpansionMB / mbToGb) - PropertyBag.getLong(PropertyBag.TRIAL_GB_FREE)  //Excludes trial space from total expansion so that totals add up
        StorageDetailItem(
            color = Color(0xFF00C752),
            storageAmount = "${formatter.format(storageAmountGB)} GB more memory can be added with FreeSpace Max",
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
        }
    }
}

@Composable
fun ManageSubscriptionButton(onClick: () -> Unit) {
    Button(
        onClick = {
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
        modifier = Modifier
            .fillMaxWidth(.7f)
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "Manage Subscription",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
