package com.speaksnap.english.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speaksnap.english.data.local.entity.ScanRecord
import com.speaksnap.english.data.repository.ScanRepository
import com.speaksnap.english.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(private val scanRepo: ScanRepository) : ViewModel() {
    val scans: StateFlow<List<ScanRecord>> = scanRepo.getAllScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWords = scans.map { list -> list.sumOf { it.wordCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streakDays = MutableStateFlow(21) // TODO: compute from UserStats
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scanRepo: ScanRepository,
    onNavigateToUpload: () -> Unit,
    onNavigateToResult: (Long) -> Unit,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToPlan: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(scanRepo) as T
    })
) {
    val scans by viewModel.scans.collectAsState()
    val totalWords by viewModel.totalWords.collectAsState()
    val streak by viewModel.streakDays.collectAsState()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet)))
                    .padding(horizontal = 20.dp, vertical = 50.dp)
            ) {
                Column {
                    Text("🏰 New Oriental · 早日退休", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("今天也要努力学英语 💪", color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Spacer(Modifier.height(12.dp))
                Text("Hi, Alex 👋", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("周三 · 2026年6月3日 · 学习第 ${streak} 天", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))

                // Stats cards
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("本周掌握", "$totalWords", "持续增长", Modifier.weight(1f))
                    StatCard("连续打卡", "${streak}🔥", "连续3周达成", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }

            // Quick Actions
            item {
                Text("⚡ 快捷操作", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction("📸", "拍照上传", onClick = onNavigateToUpload)
                    QuickAction("📖", "背单词", onClick = onNavigateToFlashcards)
                    QuickAction("💬", "AI 对话", onClick = onNavigateToPractice)
                    QuickAction("📅", "学习计划", onClick = { if (scans.isNotEmpty()) onNavigateToPlan(scans.first().id) })
                }
                Spacer(Modifier.height(20.dp))
            }

            // Recent Courses
            item {
                Text("📚 最近课程", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
            }

            if (scans.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📸", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("还没有课程记录", color = TextMuted, fontSize = 14.sp)
                            Text("点击拍照上传开始学习", color = TextDim, fontSize = 12.sp)
                        }
                    }
                }
            }

            items(scans) { scan ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onNavigateToResult(scan.id) },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📷", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(scan.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            val df = SimpleDateFormat("M月d日", Locale.CHINESE)
                            Text("${df.format(Date(scan.createdAt))} · ${scan.wordCount} 单词 · ${scan.caseStudyCount} Case Study", color = TextMuted, fontSize = 11.sp)
                        }
                        Surface(color = SuccessGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                            Text(if (scan.status == "processed") "已整理" else "待处理", color = if (scan.status == "processed") SuccessGreen else WarningAmber, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, change: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = TextMuted, fontSize = 11.sp)
            Text(value, color = SecondaryLight, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(change, color = SuccessGreen, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun QuickAction(icon: String, label: String, onClick: () -> Unit) {
    Card(
        Modifier.weight(1f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
    ) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
