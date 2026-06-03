package com.speaksnap.english.ui.flashcards

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speaksnap.english.data.local.entity.VocabularyItem
import com.speaksnap.english.data.repository.AIService
import com.speaksnap.english.data.repository.ScanRepository
import com.speaksnap.english.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FlashCardViewModel(
    private val scanRepo: ScanRepository,
    private val aiService: AIService?
) : ViewModel() {
    val words = MutableStateFlow<List<VocabularyItem>>(emptyList())
    val currentIndex = MutableStateFlow(0)
    val isFlipped = MutableStateFlow(false)
    val showMistakeBook = MutableStateFlow(false)
    val isSpeaking = MutableStateFlow(false)

    fun loadFromScan(scanId: Long) {
        viewModelScope.launch { scanRepo.getWordsByScanId(scanId).collect { words.value = it } }
    }

    fun loadMistakes() {
        viewModelScope.launch { scanRepo.getMistakeWords().collect { if (it.isNotEmpty()) words.value = it } }
    }

    fun flip() { isFlipped.value = !isFlipped.value }
    fun next() { if (currentIndex.value < words.value.size - 1) { currentIndex.value++; isFlipped.value = false } }
    fun prev() { if (currentIndex.value > 0) { currentIndex.value--; isFlipped.value = false } }
    fun markEasy(item: VocabularyItem) { viewModelScope.launch { scanRepo.updateMastery(item.id, 5) }; next() }
    fun markGotIt(item: VocabularyItem) { viewModelScope.launch { scanRepo.updateMastery(item.id, minOf(item.mastery + 1, 4)) }; next() }
    fun markHard(item: VocabularyItem) { viewModelScope.launch { scanRepo.incrementMistake(item.id) }; next() }

    fun speak(word: VocabularyItem, cacheDir: java.io.File) {
        if (isSpeaking.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isSpeaking.value = true
            aiService?.speakWord(word.word, word.example.takeIf { it.isNotBlank() })
                ?.onSuccess { bytes ->
                    val path = aiService.saveSpeech(bytes, cacheDir)
                    MediaPlayer().apply {
                        setDataSource(path)
                        prepare()
                        start()
                        setOnCompletionListener { isSpeaking.value = false; release() }
                    }
                }?.onFailure { isSpeaking.value = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardScreen(
    scanRepo: ScanRepository,
    aiService: AIService? = null,
    viewModel: FlashCardViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FlashCardViewModel(scanRepo, aiService) as T
    })
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.loadMistakes() }
    val words by viewModel.words.collectAsState()
    val index by viewModel.currentIndex.collectAsState()
    val flipped by viewModel.isFlipped.collectAsState()
    val showMistakeBook by viewModel.showMistakeBook.collectAsState()
    val speaking by viewModel.isSpeaking.collectAsState()
    val word = words.getOrNull(index)

    Scaffold(topBar = {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet))).padding(horizontal = 20.dp, vertical = 50.dp)) {
            Column {
                Text("📖 背单词", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (words.isNotEmpty()) Text("${index + 1}/${words.size} · 错题巩固", color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp)
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            // Progress dots
            if (words.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        words.take(20).forEachIndexed { i, _ ->
                            Box(Modifier.padding(2.dp).size(7.dp).clip(CircleShape).background(
                                when { i < index -> SuccessGreen; i == index -> SecondaryIndigo; else -> BorderDark }
                            ))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    // Flashcard
                    Card(Modifier.fillMaxWidth().height(320.dp).clickable { viewModel.flip() },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(20.dp), border = BorderStroke(2.dp, BorderLight)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                if (!flipped) {
                                    Text(word?.word ?: "", color = TextWhite, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                                    if (word?.phonetic?.isNotBlank() == true) Text(word!!.phonetic, color = SecondaryMedium, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                                } else {
                                    Text(word?.meaning?.ifBlank { word?.word } ?: "", color = TextSecondary, fontSize = 18.sp, textAlign = TextAlign.Center)
                                    if (word?.example?.isNotBlank() == true) Text("\"${word!!.example}\"", color = TextMuted, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    // Hint + Speaker
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text(if (!flipped) "👆 点击翻转" else "👆 再次隐藏", color = TextDim, fontSize = 11.sp)
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.size(36.dp).clip(CircleShape).background(if (speaking) SuccessGreen.copy(alpha = 0.2f) else PrimaryIndigo.copy(alpha = 0.15f)).clickable { word?.let { viewModel.speak(it, context.cacheDir) } }, contentAlignment = Alignment.Center) {
                            Text("🔊", fontSize = 16.sp)
                        }
                    }
                }

                // Actions
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.markHard(word!!) }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))) { Text("😣 记不住", color = ErrorRed) }
                        OutlinedButton(onClick = { viewModel.markGotIt(word!!) }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f))) { Text("👍 知道", color = SecondaryIndigo) }
                        OutlinedButton(onClick = { viewModel.markEasy(word!!) }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))) { Text("✅ 太简单", color = SuccessGreen) }
                    }
                }

                // Mistake link
                item {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.showMistakeBook.value = !showMistakeBook }) {
                        Text("📝 错题本 (${words.count { it.mistakeCount > 0 }})", color = TextMuted, fontSize = 12.sp)
                    }
                }

                // Mistake book
                if (showMistakeBook) {
                    items(words.filter { it.mistakeCount > 0 }) { item ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(10.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🔴", fontSize = 18.sp); Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Row { Text(item.word, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); if (item.phonetic.isNotBlank()) Text(item.phonetic, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp)) }
                                    Text("${item.meaning} · 记错${item.mistakeCount}次", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    item {
                        Button(onClick = { viewModel.currentIndex.value = 0; viewModel.showMistakeBook.value = false }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.15f)), shape = RoundedCornerShape(14.dp)) {
                            Text("🔄 复习全部错题", color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                item {
                    Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📖", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("还没有单词", color = TextMuted, fontSize = 14.sp)
                            Text("拍照上传课程后自动生成", color = TextDim, fontSize = 12.sp)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
