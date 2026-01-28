package com.binod.safedns.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CustomProfile : Screen("custom_profile")
    object Stats : Screen("stats")              // NEW
    object Logs : Screen("logs")                // NEW
    object Whitelist : Screen("whitelist")      // NEW
    object Settings : Screen("settings")

    // Optional - remove if not using
    object Apps : Screen("apps")
    object Rules : Screen("rules")
}