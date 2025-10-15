package com.tmf.freespace.presentationlayer.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tmf.freespace.presentationlayer.ui.screens.AppSummaryScreen
import com.tmf.freespace.presentationlayer.ui.screens.CloudBackupScreen
import com.tmf.freespace.presentationlayer.ui.screens.LicenseAgreementScreen
import com.tmf.freespace.presentationlayer.ui.screens.LicenseScreen
import com.tmf.freespace.presentationlayer.ui.screens.PermissionsScreen
import com.tmf.freespace.presentationlayer.ui.screens.SetItForgetItScreen
import com.tmf.freespace.presentationlayer.ui.screens.StartScreen
import com.tmf.freespace.presentationlayer.ui.screens.WelcomeScreen
import com.tmf.freespace.presentationlayer.viewmodels.AppSummaryScreenVM

@Composable
fun NavGraph(navController: NavHostController, startRoute: NavRoute, appSummaryViewModel: AppSummaryScreenVM, paddingValues: PaddingValues) {

    //Overall navigation graph:
    //  Welcome -> SetItForgetIt -> CloudBackup -> License -> Permissions -> Start -> AppSummary
    //    License.onBodyClick -> LicenseAgreement -> Permissions
    //  Welcome: If already have permissions -> AppSummary
    NavHost(
        navController = navController,
        startDestination = startRoute.path
    ) {
        addWelcomeScreen(navController, this, paddingValues)

        addSetItForgetIt(navController, this, paddingValues)

        addCloudBackup(navController, this, paddingValues)

        addLicense(navController, this, paddingValues)

        addLicenseAgreement(navController, this, paddingValues)

        addPermissions(navController, this, paddingValues)

        addStart(navController, this, paddingValues)

        AddAppSummaryScreen(navController, this, appSummaryViewModel, paddingValues)

    }
}

private fun addWelcomeScreen(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.Welcome.path) {
        WelcomeScreen(navController, paddingValues)
    }
}

private fun addSetItForgetIt(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.SetItForgetIt.path) {
        SetItForgetItScreen(navController, paddingValues)
    }
}

private fun addCloudBackup(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.CloudBackup.path) {
        CloudBackupScreen(navController, paddingValues)
    }
}

private fun addLicense(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.License.path) {
        LicenseScreen(navController, paddingValues)
    }
}

private fun addLicenseAgreement(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.LicenseAgreement.path) {
        LicenseAgreementScreen(navController, paddingValues)
    }
}

private fun addPermissions(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.Permissions.path) {
        PermissionsScreen(navController, paddingValues)
    }
}

private fun addStart(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.Start.path) {
        StartScreen(navController, paddingValues)
    }
}

private fun AddAppSummaryScreen(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    viewModel: AppSummaryScreenVM,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.AppSummary.path) {
        AppSummaryScreen(viewModel, paddingValues, navController)
    }
}
