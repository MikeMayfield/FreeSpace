package com.tmf.freespace.presentationlayer.ui.homescreen

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tmf.freespace.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeSpaceHomeScreen() {
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
                        Text(
                            text = "FreeSpace Lite",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Nearly unlimited space for all your photos and videos",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val availableMB = 648000  //TODO Compute actual value
                val formatter = DecimalFormat("###,###,###", DecimalFormatSymbols(Locale.getDefault()))

                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("${formatter.format(availableMB / 1000)}GB More Storage Available with Max")  //TODO Remove "with Max" if a subscriber
                        }
                    },
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StorageBar(31500, 12100, 20100, 8000, 720000)

            Spacer(modifier = Modifier.height(24.dp))

            StorageInfoSection(31500, 12100, 20100, 8000, 720000)

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Status: ",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Expanded to 8GB Lite version limit",
                    color = Color.DarkGray,
                    fontSize = 16.sp
                )

            }

            Spacer(modifier = Modifier.height(16.dp))

            KeepStorageFreeSection() { selectedOption ->
                //TODO Handle dropdown selection
            }

            //TODO Exclude if a subscriber
            Spacer(modifier = Modifier.weight(1f, fill = true)) // Pushes button to bottom
            Spacer(modifier = Modifier.height(24.dp))

            SubscribeButton() {
                val todo = true
                //TODO Handle button click
            }

            Spacer(modifier = Modifier.height(16.dp)) // Padding at the very bottom
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun StorageBar(photosMB: Int, videosMB: Int, appsMB: Int, addedMB: Int, maxMB: Int) {
    val totalGB = maxMB / 1024f
    val futureMB = maxMB - (photosMB + videosMB + appsMB + addedMB)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //.weight of each Box is percentage of total sizes in GB
            Box(Modifier
                .weight((photosMB + 1) / totalGB)
                .fillMaxHeight()
                .background(Color(0xFFF9770E)))
            Box(Modifier
                .weight((videosMB + 1) / totalGB)
                .fillMaxHeight()
                .background(Color(0xFF1942FE)))
            Box(Modifier
                .weight((appsMB + 1) / totalGB)
                .fillMaxHeight()
                .background(Color(0xFFF918F3)))
            Box(Modifier
                .weight((addedMB + 1) / totalGB)
                .fillMaxHeight()
                .background(Color(0xFF00C752)))
            Box(modifier = Modifier
                    .weight((futureMB + 1) / totalGB)
                    .fillMaxHeight()
                    .background(Color(0xFFB7F9B6)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${String.format("%.0f", futureMB / 1000f)}GB Space Available",
                    color = Color.DarkGray,
                    fontSize = 12.sp
                )
            }
        }

    }
}


@SuppressLint("DefaultLocale")
@Composable
fun StorageInfoSection(photosMB: Int = 0, videosMB: Int = 0, appsMB: Int = 0, addedMB: Int = 0, maxMB: Int = 720000) {
    val totalUsedMb = photosMB + videosMB + appsMB + addedMB
    val futureKB = (maxMB - totalUsedMb) * 1000
    val avgPhotoKB = 500
    val avgVideoKB = 1000
    val gbFormatter = DecimalFormat("###,###,###.0", DecimalFormatSymbols(Locale.getDefault()))
    val formatter = DecimalFormat("###,###,###", DecimalFormatSymbols(Locale.getDefault()))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StorageDetailItem(
            color = Color(0xFFF9770E),
            storageAmount = "${gbFormatter.format(photosMB / 1000f)}GB of Photos",
            description = "Room for ${formatter.format(futureKB / avgPhotoKB)} more photos"
        )
        StorageDetailItem(
            color = Color(0xFF1942FE),
            storageAmount = "${gbFormatter.format(videosMB / 1000f)}GB of Videos",
            description = "Room for ${formatter.format(futureKB / avgVideoKB)} more videos"
        )
        StorageDetailItem(
            color = Color(0xFFF918F3),
            storageAmount = "${gbFormatter.format(appsMB / 1000f)}GB of Apps, etc.",
            description = "Remove apps to add storage"
        )
        StorageDetailItem(
            color = Color(0xFF00C752),
            storageAmount = "${gbFormatter.format(addedMB / 1000f)}GB of Space was Added",
            description = "Lite version limited to 8GB"  //TODO Remove if a subscriber
        )
        StorageDetailItem(
            color = Color(0xFFB7F9B6),
            storageAmount = "${gbFormatter.format((maxMB - totalUsedMb) / 1000f)}GB Future Expansion",
            description = "Available only with Max subscription"

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
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = storageAmount,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Text(
                text = description,
                fontSize = 16.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun KeepStorageFreeSection(onClick: (selectedOption: String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("2 GB", "5 GB", "10 GB", "5%", "10%")
    var selectedOptionText by remember { mutableStateOf(options[0]) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Keep", fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
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
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            selectedOptionText = selectionOption
                            expanded = false
                            onClick(selectionOption)
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("of storage free", fontSize = 16.sp, color = Color.Black)
    }
}

@Composable
fun SubscribeButton(onClick: () -> Unit) {
    Button(
        onClick = {
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "Subscribe to Max Storage",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "$1.99",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_4")
@Composable
fun FreeSpaceHomeScreenPreview() {
    MaterialTheme {
        FreeSpaceHomeScreen()
    }
}