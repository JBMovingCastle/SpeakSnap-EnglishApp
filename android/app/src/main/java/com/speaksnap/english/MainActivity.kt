package com.speaksnap.english

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.speaksnap.english.data.local.AppDatabase
import com.speaksnap.english.data.repository.AIService
import com.speaksnap.english.data.repository.ClaudeRepository
import com.speaksnap.english.data.repository.DeepSeekRepository
import com.speaksnap.english.data.repository.DoubaoRepository
import com.speaksnap.english.data.repository.SettingsRepository
import com.speaksnap.english.data.repository.TongyiRepository
import com.speaksnap.english.ui.navigation.Screen
import com.speaksnap.english.ui.navigation.SpeakSnapNavGraph
import com.speaksnap.english.ui.navigation.bottomNavItems
import com.speaksnap.english.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(this)
        val settingsRepo = SettingsRepository(this)
        val aiService = AIService(
            settings = settingsRepo,
            tongyi = TongyiRepository(),
            deepseek = DeepSeekRepository(),
            doubao = DoubaoRepository(),
            claude = ClaudeRepository()
        )

        setContent {
            SpeakSnapTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = bottomNavItems.any { it.route == currentRoute }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = SurfaceDark,
                                tonalElevation = 0.dp,
                                modifier = Modifier.height(64.dp)
                            ) {
                                bottomNavItems.forEach { screen ->
                                    val selected = currentRoute == screen.route
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) screen.selectedIcon else screen.icon,
                                                contentDescription = screen.title,
                                                tint = if (selected) SecondaryIndigo else Color(0xFF666666),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(screen.title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) SecondaryIndigo else Color(0xFF666666))
                                        },
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(indicatorColor = SecondaryIndigo.copy(alpha = 0.1f))
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SpeakSnapNavGraph(navController = navController, db = db, settingsRepo = settingsRepo, aiService = aiService)
                    }
                }
            }
        }
    }
}
