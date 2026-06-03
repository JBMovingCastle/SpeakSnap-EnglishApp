package com.speaksnap.english.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.speaksnap.english.data.local.AppDatabase
import com.speaksnap.english.data.repository.AIService
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
    aiService: AIService
) {
    val scanRepo = ScanRepository(db)

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                scanRepo = scanRepo,
                onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                onNavigateToResult = { navController.navigate(DetailRoutes.aiResult(it)) },
                onNavigateToFlashcards = { navController.navigate(Screen.Flashcards.route) },
                onNavigateToPractice = { navController.navigate(Screen.Practice.route) },
                onNavigateToPlan = { navController.navigate(DetailRoutes.weeklyPlan(it)) }
            )
        }

        composable(Screen.Upload.route) {
            UploadScreen(aiService = aiService, scanRepo = scanRepo, settingsRepo = settingsRepo,
                onNavigateToResult = { navController.navigate(DetailRoutes.aiResult(it)) })
        }

        composable(Screen.Flashcards.route) {
            FlashCardScreen(scanRepo = scanRepo, aiService = aiService)
        }

        composable(Screen.Practice.route) {
            ConversationScreen(aiService = aiService, scanRepo = scanRepo)
        }

        composable(Screen.Plan.route) {
            WeeklyPlanScreen(scanRepo = scanRepo)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(settingsRepo = settingsRepo)
        }

        composable(route = DetailRoutes.AI_RESULT, arguments = listOf(navArgument("scanId") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("scanId") ?: 0L
            AIResultScreen(scanId = id, scanRepo = scanRepo,
                onNavigateToFlashcards = { navController.navigate(Screen.Flashcards.route) },
                onNavigateToConversation = { navController.navigate(Screen.Practice.route) },
                onNavigateToPlan = { navController.navigate(DetailRoutes.weeklyPlan(id)) })
        }

        composable(route = DetailRoutes.WEEKLY_PLAN_DETAIL, arguments = listOf(navArgument("scanId") { type = NavType.LongType })) {
            WeeklyPlanScreen(scanRepo = scanRepo)
        }
    }
}
