package com.speaksnap.english.ui.flashcards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speaksnap.english.data.local.entity.VocabularyItem
import com.speaksnap.english.data.repository.ScanRepository
import com.speaksnap.english.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FlashCardViewModel(private val scanRepo: ScanRepository) : ViewModel() {
    val words = MutableStateFlow<List<VocabularyItem>>(emptyList())
    val currentIndex = MutableStateFlow(0)
    val isFlipped = MutableStateFlow(false)
    val showMistakeBook = MutableStateFlow(false)

    val currentWord: StateFlow<VocabularyItem?> = androidx.compose.runtime.derivedStateOf {
        words.value.getOrNull(currentIndex.value)
    } as StateFlow<VocabularyItem?>

    fun loadWords() {
        viewModelScope.launch {
            scanRepo.getMistakeWords().collect { mistakes ->
                // Prefer mistake words, then all words
                if (mistakes.isNotEmpty()) words.value = mistakes
                else {
                    // Load all words from all scans
                }
            }
        }
    }

    fun loadFromScan(scanId: Long) {
        viewModelScope.launch {
            scanRepo.getWordsByScanId(scanId).collect { words.value = it }
        }
    }

    fun flip() { isFlipped.value = !isFlipped.value }

    fun next() {
        if (currentIndex.value < words.value.size - 1) {
            currentIndex.value++
            isFlipped.value = false
        }
    }

    fun prev() {
        if (currentIndex.value > 0) {
            currentIndex.value--
            isFlipped.value = false
        }
    }

    fun markEasy(item: VocabularyItem) {
        viewModelScope.launch { scanRepo.updateMastery(item.id, 5) }
        next()
    }

    fun markGotIt(item: VocabularyItem) {
        viewModelScope.launch { scanRepo.updateMastery(item.id, minOf(item.mastery + 1, 4)) }
        next()
    }

    fun markHard(item: VocabularyItem) {
        viewModelScope.launch { scanRepo.incrementMistake(item.id) }
        next()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardScreen(
    scanRepo: ScanRepository,
    viewModel: FlashCardViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FlashCardViewModel(scanRepo) as T
    })
) {
    LaunchedEffect(Unit) { viewModel.loadWords() }
    val words by viewModel.words.collectAsState()
    val index by viewModel.currentIndex.collectAsState()
    val flipped by viewModel.isFlipped.collectAsState()
    val showMistakeBook by viewModel.showMistakeBook.collectAsState()
    val word = words.getOrNull(index)

    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet))).padding(horizontal = 20.dp, vertical = 50.dp)) {
                Column {
                    Text("📖 背单词", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${index + 1}/${words.size} · 错误本巩固", color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            // Progress dots
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    words.forEachIndexed { i, _ ->
                        Box(Modifier.padding(2.dp).size(8.dp).clip(CircleShape).background(
                            when {
                                i < index -> SuccessGreen
                                i == index -> SecondaryIndigo
                                else -> BorderDark
                            }
                        ))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Flashcard
            if (word != null) {
                item {
                    Card(
                        Modifier.fillMaxWidth().height(300.dp).clickable { viewModel.flip() },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(2.dp, BorderLight)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (!flipped) {
                                // Front: word + phonetic
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(word.word, color = TextWhite, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                                    if (word.phonetic.isNotBlank()) Text(word.phonetic, color = SecondaryMedium, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                                }
                            } else {
                                // Back: meaning + example
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                                    Text(word.meaning.ifBlank { word.word }, color = TextSecondary, fontSize = 18.sp, textAlign = TextAlign.Center)
                                    if (word.example.isNotBlank()) Text("\"${word.example}\"", color = TextMuted, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    Text(if (!flipped) "👆 点击翻转 · 查看释义" else "👆 再次点击隐藏释义", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }

                // Action buttons
                item {
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { viewModel.markHard(word) }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))) {
                            Text("😣 记不住", color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { viewModel.markGotIt(word) }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f))) {
                            Text("👍 知道了", color = SecondaryIndigo, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { viewModel.markEasy(word) }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))) {
                            Text("✅ 太简单", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Mistake book link
                item {
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.showMistakeBook.value = !showMistakeBook }) {
                        Text("📝 错题本 (${words.count { it.mistakeCount > 0 }} 个)", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }

            // Mistake book
            if (showMistakeBook) {
                items(words.filter { it.mistakeCount > 0 }) { item ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔴", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(item.word, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    if (item.phonetic.isNotBlank()) Text(item.phonetic, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                                }
                                Text("${item.meaning} · 记错${item.mistakeCount}次", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
                item {
                    Button(onClick = { viewModel.currentIndex.value = 0 }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.15f)), shape = RoundedCornerShape(14.dp)) {
                        Text("🔄 复习全部错题 (${words.count { it.mistakeCount > 0 }} 个)", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
