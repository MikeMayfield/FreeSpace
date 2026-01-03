package com.tmf.freespace.presentationlayer.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tmf.freespace.presentationlayer.ui.screens.AppSummaryScreen
import com.tmf.freespace.presentationlayer.ui.screens.CloudBackupScreen
import com.tmf.freespace.presentationlayer.ui.screens.HowDoesItWorkScreen
import com.tmf.freespace.presentationlayer.ui.screens.LicenseAgreementScreen
import com.tmf.freespace.presentationlayer.ui.screens.LicenseScreen
import com.tmf.freespace.presentationlayer.ui.screens.PermissionsScreen
import com.tmf.freespace.presentationlayer.ui.screens.SetItForgetItScreen
import com.tmf.freespace.presentationlayer.ui.screens.StartScreen
import com.tmf.freespace.presentationlayer.ui.screens.SubscriptionPromoScreen
import com.tmf.freespace.presentationlayer.ui.screens.SubscriptionScreen
import com.tmf.freespace.presentationlayer.ui.screens.WelcomeScreen
import com.tmf.freespace.presentationlayer.viewmodels.CommonViewModel

@Composable
fun NavGraph(navController: NavHostController, startRoute: NavRoute, commonViewModel: CommonViewModel, paddingValues: PaddingValues) {

    //Overall navigation graph:
    //  Welcome -> SetItForgetIt -> CloudBackup -> License -> Permissions -> SubscriptionPromo -> Start -> AppSummary
    //    License.onBodyClick -> LicenseAgreement -> Permissions
    //  Welcome: If already have permissions -> AppSummary
    NavHost(
        navController = navController,
        startDestination = startRoute.path,
        enterTransition = {
            fadeIn(animationSpec = tween(500)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,  tween(1000))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(500)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left, tween(1000))
        },
        ) {
        addWelcomeScreen(navController, this, paddingValues)

        addHowDoesItWorkScreen(navController, this, paddingValues)

        addSetItForgetIt(navController, this, paddingValues)

        addCloudBackup(navController, this, paddingValues)


        addLicense(navController, this, paddingValues)

        addLicenseAgreement(navController, this, paddingValues)

        addPermissions(navController, this, paddingValues)

        addSubscriptionPromo(navController, this, paddingValues)

        addStart(navController, this, paddingValues)

        addAppSummaryScreen(navController, this, commonViewModel, paddingValues)

        addSubscriptionScreen(navController, this, paddingValues, commonViewModel)
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

private fun addHowDoesItWorkScreen(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.HowDoesItWork.path) {
        HowDoesItWorkScreen(navController, paddingValues)
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

private fun addSubscriptionPromo(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.SubscriptionPromo.path) {
        SubscriptionPromoScreen(navController, paddingValues)
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

private fun addAppSummaryScreen(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    viewModel: CommonViewModel,
    paddingValues: PaddingValues
) {
    navGraphBuilder.composable(route = NavRoute.AppSummary.path) {
        AppSummaryScreen(viewModel, paddingValues, navController)
    }
}

private fun addSubscriptionScreen(
    navController: NavHostController,
    navGraphBuilder: NavGraphBuilder,
    paddingValues: PaddingValues,
    viewModel: CommonViewModel
) {
    navGraphBuilder.composable(route = NavRoute.Subscription.path) {
        SubscriptionScreen(viewModel, navController, paddingValues)
    }
}
