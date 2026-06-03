package com.speaksnap.english.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speaksnap.english.data.local.entity.ScanRecord
import com.speaksnap.english.data.local.entity.VocabularyItem
import com.speaksnap.english.data.repository.ScanRepository
import com.speaksnap.english.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ResultViewModel(private val scanRepo: ScanRepository) : ViewModel() {
    val scan = MutableStateFlow<ScanRecord?>(null)
    val words = MutableStateFlow<List<VocabularyItem>>(emptyList())
    val phrases = MutableStateFlow<List<VocabularyItem>>(emptyList())
    val grammarPoints = MutableStateFlow<List<VocabularyItem>>(emptyList())
    val caseStudies = MutableStateFlow<List<VocabularyItem>>(emptyList())

    fun load(scanId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            scan.value = scanRepo.getScanById(scanId)
        }
        viewModelScope.launch {
            scanRepo.getWordsByScanId(scanId).collect { words.value = it }
        }
        viewModelScope.launch {
            scanRepo.getPhrasesByScanId(scanId).collect { phrases.value = it }
        }
        viewModelScope.launch {
            scanRepo.getVocabularyByScanId(scanId).collect { all ->
                grammarPoints.value = all.filter { it.type == "grammar" }
                caseStudies.value = all.filter { it.type == "case_study" }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIResultScreen(
    scanId: Long,
    scanRepo: ScanRepository,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToConversation: () -> Unit,
    onNavigateToPlan: () -> Unit,
    viewModel: ResultViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ResultViewModel(scanRepo) as T
    })
) {
    LaunchedEffect(scanId) { viewModel.load(scanId) }
    val scan by viewModel.scan.collectAsState()
    val words by viewModel.words.collectAsState()
    val phrases by viewModel.phrases.collectAsState()
    val grammarPoints by viewModel.grammarPoints.collectAsState()
    val caseStudies by viewModel.caseStudies.collectAsState()

    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet))).padding(horizontal = 20.dp, vertical = 50.dp)) {
                Column {
                    Text("📋 AI 整理结果", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    scan?.let { Text(it.title + " · ${it.createdAt}", color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp) }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            // Overview card
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderLight)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("📊 内容概览", color = SecondaryLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tag("📖 ${words.size} 单词")
                            Tag("💬 ${phrases.size} 短语")
                            Tag("📐 ${grammarPoints.size} 语法点")
                            Tag("🎯 ${caseStudies.size} Case Study")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Words
            if (words.isNotEmpty()) {
                item {
                    SectionHeader("📖 核心词汇 (${words.size})")
                    Spacer(Modifier.height(8.dp))
                    Column {
                        words.forEach { w ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, BorderDark)) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(w.word, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            if (w.phonetic.isNotBlank()) Text(w.phonetic, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 6.dp))
                                        }
                                        if (w.meaning.isNotBlank()) Text(w.meaning, color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Phrases
            if (phrases.isNotEmpty()) {
                item {
                    SectionHeader("💬 关键短语 (${phrases.size})")
                    Spacer(Modifier.height(8.dp))
                    phrases.forEach { p ->
                        Text("• ${p.word} — ${p.meaning}", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Grammar
            if (grammarPoints.isNotEmpty()) {
                item {
                    SectionHeader("📐 语法要点 (${grammarPoints.size})")
                    Spacer(Modifier.height(8.dp))
                    grammarPoints.forEachIndexed { i, g ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text("${i + 1}️⃣ ${g.word}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (g.meaning.isNotBlank()) Text(g.meaning, color = TextMuted, fontSize = 12.sp)
                            if (g.example.isNotBlank()) Text(g.example, color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Case Studies
            if (caseStudies.isNotEmpty()) {
                item {
                    SectionHeader("🎯 Case Study (${caseStudies.size})")
                    Spacer(Modifier.height(8.dp))
                }
                items(caseStudies) { c ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, BorderDark)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(c.word, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (c.meaning.isNotBlank()) Text(c.meaning, color = TextMuted, fontSize = 11.sp)
                            if (c.example.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Surface(color = PrimaryIndigo.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.clickable { onNavigateToConversation() }) {
                                    Text("💬 以此场景开始 AI 对话", color = SecondaryIndigo, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Action buttons
            item {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onNavigateToFlashcards, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo), shape = RoundedCornerShape(14.dp)) {
                        Text("📖 开始背单词", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onNavigateToPlan, Modifier.weight(1f), border = BorderStroke(1.dp, BorderDark), shape = RoundedCornerShape(14.dp)) {
                        Text("📅 加入学习计划", color = SecondaryIndigo, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun Tag(text: String) {
    Surface(color = PrimaryIndigo.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Text(text, color = SecondaryIndigo, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
