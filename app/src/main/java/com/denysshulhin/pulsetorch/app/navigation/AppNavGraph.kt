package com.denysshulhin.pulsetorch.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.denysshulhin.pulsetorch.app.AppViewModel
import com.denysshulhin.pulsetorch.domain.model.Effect
import com.denysshulhin.pulsetorch.domain.model.Mode
import com.denysshulhin.pulsetorch.feature.capture.SystemCaptureScreen
import com.denysshulhin.pulsetorch.feature.control.HomeControlScreen
import com.denysshulhin.pulsetorch.feature.file.FilePlayerScreen
import com.denysshulhin.pulsetorch.feature.settings.SettingsScreen

@Composable
fun AppNavGraph(appVm: AppViewModel) {
    val nav = rememberNavController()
    val state by appVm.uiState.collectAsStateWithLifecycle()

    fun routeForMode(mode: Mode): String = when (mode) {
        Mode.FILE -> Screen.File.route
        Mode.SYSTEM -> Screen.Capture.route
        Mode.MIC -> Screen.Home.route
    }

    fun goMode(mode: Mode) {
        appVm.setMode(mode)
        nav.switchMode(routeForMode(mode))
    }

    NavHost(
        navController = nav,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeControlScreen(
                state = state,
                onOpenSettings = { nav.navigateSingleTop(Screen.Settings.route) },
                onSelectMode = { goMode(it) },
                onSelectEffectIndex = { idx ->
                    appVm.setEffect(
                        when (idx) {
                            0 -> Effect.SMOOTH
                            1 -> Effect.PULSE
                            else -> Effect.STROBE
                        }
                    )
                },
                onSensitivityChange = appVm::setSensitivity,
                onSmoothnessChange = appVm::setSmoothness,
                onToggleRunning = appVm::toggleRunning
            )
        }

        composable(Screen.File.route) {
            FilePlayerScreen(
                state = state,
                onBack = { goMode(Mode.MIC) },
                onUseInHome = { goMode(Mode.MIC) },
                onSelectMode = { goMode(it) }
            )
        }

        composable(Screen.Capture.route) {
            SystemCaptureScreen(
                state = state,
                onBack = { goMode(Mode.MIC) },
                onSelectMode = { goMode(it) },
                onToggleRunning = appVm::toggleRunning
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                state = state,
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() },

                onAutoBrightnessChange = appVm::setAutoBrightness,
                onMaxStrobeHzChange = appVm::setMaxStrobeHz,
                onMicGainChange = appVm::setMicGain,
                onSmoothingChange = appVm::setSmoothing,
                onBassFocusChange = appVm::setBassFocus,
                onStrobeWarningChange = appVm::setStrobeWarning
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

private fun NavHostController.switchMode(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(Screen.Home.route) {
            inclusive = false
            saveState = true
        }
    }
}
