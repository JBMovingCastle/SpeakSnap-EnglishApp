package com.speaksnap.english.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.speaksnap.english.data.local.AppDatabase
import com.speaksnap.english.data.repository.ClaudeRepository
import com.speaksnap.english.data.repository.ScanRepository
import com.speaksnap.english.data.repository.SettingsRepository
import com.speaksnap.english.ui.conversation.ConversationScreen
import com.speaksnap.english.ui.flashcards.FlashCardScreen
import com.speaksnap.english.ui.home.HomeScreen
import com.speaksnap.english.ui.plan.WeeklyPlanScreen
import com.speaksnap.english.ui.result.AIResultScreen
import com.speaksnap.english.ui.settings.SettingsScreen
import com.speaksnap.english.ui.upload.UploadScreen

@Composable
fun SpeakSnapNavGraph(
    navController: NavHostController,
    db: AppDatabase,
    settingsRepo: SettingsRepository,
    claudeRepo: ClaudeRepository
) {
    val scanRepo = ScanRepository(db)

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                scanRepo = scanRepo,
                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                onNavigateToResult = { scanId -> navController.navigate(DetailRoutes.aiResult(scanId)) },
                onNavigateToFlashcards = { navController.navigate(Screen.Flashcards.route) },
                onNavigateToPlan = { scanId -> navController.navigate(DetailRoutes.weeklyPlan(scanId)) }
            )
        }

        composable(Screen.Upload.route) {
            UploadScreen(
                claudeRepo = claudeRepo,
                scanRepo = scanRepo,
                settingsRepo = settingsRepo,
                onNavigateToResult = { scanId -> navController.navigate(DetailRoutes.aiResult(scanId)) }
            )
        }

        composable(Screen.Flashcards.route) {
            FlashCardScreen(scanRepo = scanRepo)
        }

        composable(Screen.Practice.route) {
            ConversationScreen(
                claudeRepo = claudeRepo,
                settingsRepo = settingsRepo,
                scanRepo = scanRepo
            )
        }

        composable(Screen.Plan.route) {
            WeeklyPlanScreen(scanRepo = scanRepo)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(settingsRepo = settingsRepo)
        }

        composable(
            route = DetailRoutes.AI_RESULT,
            arguments = listOf(navArgument("scanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scanId = backStackEntry.arguments?.getLong("scanId") ?: 0L
            AIResultScreen(
                scanId = scanId,
                scanRepo = scanRepo,
                onNavigateToFlashcards = { navController.navigate(Screen.Flashcards.route) },
                onNavigateToConversation = { navController.navigate(Screen.Practice.route) },
                onNavigateToPlan = { navController.navigate(DetailRoutes.weeklyPlan(scanId)) }
            )
        }

        composable(
            route = DetailRoutes.WEEKLY_PLAN_DETAIL,
            arguments = listOf(navArgument("scanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scanId = backStackEntry.arguments?.getLong("scanId") ?: 0L
            WeeklyPlanScreen(scanRepo = scanRepo)
        }
    }
}
