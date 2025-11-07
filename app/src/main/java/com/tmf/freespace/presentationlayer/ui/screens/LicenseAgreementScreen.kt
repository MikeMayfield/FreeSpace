package com.tmf.freespace.presentationlayer.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.tmf.freespace.presentationlayer.ui.composables.GenericTextBody
import com.tmf.freespace.presentationlayer.ui.navigation.NavRoute

@Composable
fun LicenseAgreementScreen(navController: NavHostController, paddingValues: PaddingValues) {
    GenericTextBody(
        title = "LICENSE AGREEMENT",
        bodyHtml = 
            "<br>This End User License Agreement (“Agreement”) is a legal agreement between you (“User” or “you”) and TMF Enterprises, LLC (“Company,” “we,” “our,” or “us”) governing your use of the FreeSpace mobile application (“App”) for Android devices.<br><br>" +
                "By installing, accessing, or using the App, you agree to be bound by the terms of this Agreement. If you do not agree, do not install or use the App.<br><br>" +

                "<b>1. License Grant</b><br><br>" +
                "We grant you a limited, non-exclusive, non-transferable, revocable license to install and use the App on your personal Android device, solely for your own non-commercial use and in accordance with this Agreement and applicable laws.<br><br>" +

                "<b>2. Privacy and Data Use</b><br><br>" +
                "We respect your privacy. FreeSpace does not collect, store, or share any personal information or media content. All your photos, videos, and data remain on your device and in your chosen cloud storage provider (such as Google, Microsoft, Dropbox, or TeraBox).<br><br>" +
                "You are responsible for enabling and maintaining your own cloud backups if you wish to protect your media files. FreeSpace does not transmit, access, or retain copies of your content.<br><br>" +

                "<b>3. Intellectual Property</b><br><br>" +
                "All copyright and other personal rights to your photos, videos, and other media remain your exclusive property. All intellectual property rights in and to the App, including but not limited to software, design, and trademarks, remain the exclusive property of Company. You are granted only the limited rights expressly stated in this Agreement.<br><br>" +

                "<b>4. Photo and Video Quality</b><br><br>" +
                "To provide expanded free space on your device, the App may optimize, compress, or modify stored photos and videos. You acknowledge and agree that some reduction in photo or video quality may occur, particularly in those more than 1-2 years old. You accept these risks as part of using the App and are responsible for maintaining original backups through supported cloud services.<br><br>" +

                "<b>5. No Warranty</b><br><br>" +
                "The App is provided “as is” and without warranty of any kind, express or implied. We make no guarantees that the App will be error-free, uninterrupted, secure, or suitable for any particular purpose. You assume full responsibility for your use of the App and any outcomes, including potential data loss or reduced media quality.<br><br>" +

                "<b>6. Limitation of Liability</b><br><br>" +
                "To the fullest extent permitted by law Company and its affiliates, officers, employees, and partners shall not be liable for any direct, indirect, incidental, special, or consequential damages, including but not limited to loss of data, photos, videos, profits, or device functionality, arising from your use or inability to use the App — even if we have been advised of the possibility of such damages.<br><br>" +

                "<b>7. Termination</b><br><br>" +
                "This Agreement is effective until terminated. You may terminate it at any time by uninstalling the App. We may also terminate it at any time by revoking this License to you. Upon termination, you must cease all use of the App and delete all copies from your devices.<br><br>" +

                "<b>8. Governing Law</b><br><br>" +
                "This Agreement shall be governed by and construed in accordance with the laws of King County, Washington, USA, without regard to its conflict of law provisions.<br><br>" +

                "<b>9. Entire Agreement</b><br><br>" +
                "This Agreement constitutes the entire understanding between you and Company concerning the App and supersedes all prior communications, agreements or understandings.<br><br>" +

                "<b>By using FreeSpace, you acknowledge that you have read, understood, and agree to this Agreement, including the disclaimers about photo and video quality and the absence of warranties or liability.</b><br><br>" +

                "<em>Last updated: 07-Nov-2025</em><br><br>",
        navButtonText = "ACCEPT",
        paddingValues = paddingValues,
    ) {
        navController.navigate(NavRoute.Permissions.path)
    }
}
