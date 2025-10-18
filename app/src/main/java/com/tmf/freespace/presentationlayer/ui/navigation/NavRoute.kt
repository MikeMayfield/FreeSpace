package com.tmf.freespace.presentationlayer.ui.navigation


sealed class NavRoute(val path: String) {

    object Welcome: NavRoute("welcome")
    object SetItForgetIt: NavRoute("setItForgetIt")
    object CloudBackup: NavRoute("cloudBackup")
    object License: NavRoute("license")
    object LicenseAgreement: NavRoute("licenseAgreement")
    object Permissions: NavRoute("permissions")
    object SubscriptionPromo: NavRoute("subscriptionPromo")
    object Start: NavRoute("start")
    object AppSummary: NavRoute("appSummary")
}
