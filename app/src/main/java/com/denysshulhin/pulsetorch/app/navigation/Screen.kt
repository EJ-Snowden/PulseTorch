package com.denysshulhin.pulsetorch.app.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object File : Screen("file")
    data object Capture : Screen("capture")
    data object Settings : Screen("settings")
}