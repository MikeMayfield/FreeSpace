package com.tmf.freespace.presentationlayer.ui.composables

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun GenericTextBody(
    modifier: Modifier = Modifier,
    imageID: Int? = null,  //Resource ID for the image, if any
    title: String? = null,  //Title text, if any
    bodyHtml: String,
    navButtonText: String? = "NEXT",  //Bottom navigation button text, if any
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBodyClick: () -> Unit = {},  //Callback for body click
    onNavButtonClick: () -> Unit = {},  //Callback for navigation button click
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp - paddingValues.calculateLeftPadding(LayoutDirection.Ltr) + paddingValues.calculateRightPadding(LayoutDirection.Ltr)
    val smallLayout = screenWidthDp < 370.dp
    val largeLayout = screenWidthDp >= 700.dp


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f, fill = true))  //Proportionally add space between top and bottom areas

        //Top image
        if (imageID != null) {
            Image(
                painter = painterResource(id = imageID),
                contentDescription = "Content image",
                modifier = Modifier.fillMaxWidth(if (largeLayout) 0.5f else 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        //Title
        if (title != null) {
            Text(
                AnnotatedString.fromHtml(title),
                fontSize = if (smallLayout) 18.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xff04049d),
                modifier = modifier
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        //Main body
        Text(
            AnnotatedString.fromHtml(
                bodyHtml,
                linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        fontStyle = FontStyle.Italic,
                        color = Color.Blue
                    )
                ),
            ),
            fontSize = if (smallLayout) 12.sp else 18.sp,
            modifier = modifier
                .align(Alignment.Start)
                .clickable() {
                    onBodyClick()
                }
        )

        //Bottom navigation button
        Spacer(modifier = Modifier.weight(1f, fill = true))  //Proportionally add space between top and bottom areas

        if (navButtonText != null) {
            Button(
                colors = ButtonColors(Color(0xff04049d), White, Color(0xff04049d), White),
                onClick = { onNavButtonClick() }) {
                Text(
                    text = navButtonText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Padding at the very bottom
    }
}