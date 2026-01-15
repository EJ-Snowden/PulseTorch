package com.denysshulhin.pulsetorch.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
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
                onOpenSettings = { nav.navigateSingleTop(Screen.Settings.route) },
                onOpenFile = { nav.switchMode(Screen.File.route) },
                onOpenCapture = { nav.switchMode(Screen.Capture.route) }
            )
        }

        composable(Screen.File.route) {
            FilePlayerScreen(
                onBack = { nav.popBackStack(Screen.Home.route, inclusive = false) },
                onUseInHome = { nav.popBackStack(Screen.Home.route, inclusive = false) },

                // Mode tabs on this screen
                onOpenFile = { nav.switchMode(Screen.File.route) },
                onOpenSystem = { nav.switchMode(Screen.Capture.route) },
                onOpenMic = { nav.switchMode(Screen.Home.route) }
            )
        }

        composable(Screen.Capture.route) {
            SystemCaptureScreen(
                onBack = { nav.popBackStack(Screen.Home.route, inclusive = false) },
                onSwitchToMic = { nav.switchMode(Screen.Home.route) },

                // Mode tabs on this screen
                onOpenFile = { nav.switchMode(Screen.File.route) },
                onOpenSystem = { nav.switchMode(Screen.Capture.route) },
                onOpenMic = { nav.switchMode(Screen.Home.route) }
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

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Switch between File/System/Mic modes without growing the back stack.
 * Always keeps Home (Mic) as the base.
 */
private fun NavHostController.switchMode(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true

        // keep Home as the root of mode navigation
        popUpTo(Screen.Home.route) {
            inclusive = false
            saveState = true
        }
    }
}
