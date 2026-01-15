package com.denysshulhin.pulsetorch.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.denysshulhin.pulsetorch.feature.capture.SystemCaptureScreen
import com.denysshulhin.pulsetorch.feature.control.HomeControlScreen
import com.denysshulhin.pulsetorch.feature.file.FilePlayerScreen
import com.denysshulhin.pulsetorch.feature.settings.SettingsScreen

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeControlScreen(
                onOpenSettings = { nav.navigate(Screen.Settings.route) },
                onOpenFile = { nav.navigate(Screen.File.route) },
                onOpenCapture = { nav.navigate(Screen.Capture.route) }
            )
        }
        composable(Screen.File.route) {
            FilePlayerScreen(
                onBack = { nav.popBackStack() },
                onUseInHome = { nav.popBackStack(Screen.Home.route, inclusive = false) }
            )
        }
        composable(Screen.Capture.route) {
            SystemCaptureScreen(
                onBack = { nav.popBackStack() },
                onSwitchToMic = { nav.popBackStack(Screen.Home.route, inclusive = false) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() }
            )
        }
    }
}
