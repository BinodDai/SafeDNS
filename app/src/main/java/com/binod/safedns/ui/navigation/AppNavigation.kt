package com.binod.safedns.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.binod.safedns.ui.home.HomeScreen
import com.binod.safedns.ui.profile.CustomProfileScreen
import com.binod.safedns.ui.stats.StatsScreen
import com.binod.safedns.ui.logs.QueryLogsScreen
import com.binod.safedns.ui.whitelist.WhitelistScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenCustomProfile = { navController.navigate(Screen.CustomProfile.route) },
                onOpenStats = { navController.navigate(Screen.Stats.route) },
                onOpenLogs = { navController.navigate(Screen.Logs.route) },          // NEW
                onOpenWhitelist = { navController.navigate(Screen.Whitelist.route) } // NEW
            )
        }

        composable(Screen.CustomProfile.route) {
            CustomProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // NEW: Statistics Screen
        composable(Screen.Stats.route) {
            StatsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // NEW: Query Logs Screen
        composable(Screen.Logs.route) {
            QueryLogsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // NEW: Whitelist Screen
        composable(Screen.Whitelist.route) {
            WhitelistScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Optional: Settings Screen (if you want to add it)
        composable(Screen.Settings.route) {
            // SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}