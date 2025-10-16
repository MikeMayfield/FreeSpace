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

//    object Search: NavRoute("search") {
//        val query = "query"
//    }

    // build navigation path (for screen navigation)
    fun withArgs(vararg args: String): String {
        return buildString {
            append(path)
            args.forEach{ arg ->
                append("/$arg")
            }
        }
    }

    // build and setup route format (in navigation graph)
    fun withArgsFormat(vararg args: String) : String {
        return buildString {
            append(path)
            args.forEach{ arg ->
                append("/{$arg}")
            }
        }
    }
}

