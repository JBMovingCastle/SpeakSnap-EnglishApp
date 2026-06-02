package com.speaksnap.english.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Outlined.Home, Icons.Filled.Home)
    data object Upload : Screen("upload", "拍照", Icons.Outlined.CameraAlt, Icons.Filled.CameraAlt)
    data object Flashcards : Screen("flashcards", "背单词", Icons.Outlined.MenuBook, Icons.Filled.MenuBook)
    data object Practice : Screen("practice", "对话", Icons.Outlined.Chat, Icons.Filled.Chat)
    data object Plan : Screen("plan", "计划", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth)
    data object Settings : Screen("settings", "设置", Icons.Outlined.Settings, Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Upload,
    Screen.Flashcards,
    Screen.Practice,
    Screen.Settings
)

// Detail screen routes (not in bottom nav)
object DetailRoutes {
    const val AI_RESULT = "ai_result/{scanId}"
    const val MISTAKE_BOOK = "mistake_book"
    const val WEEKLY_PLAN_DETAIL = "weekly_plan/{scanId}"

    fun aiResult(scanId: Long) = "ai_result/$scanId"
    fun weeklyPlan(scanId: Long) = "weekly_plan/$scanId"
}
