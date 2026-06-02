package com.speaksnap.english.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speaksnap.english.data.remote.dto.CaseStudyItem
import com.speaksnap.english.data.repository.AIService
import com.speaksnap.english.data.repository.ScanRepository
import com.speaksnap.english.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val content: String, val timestamp: Long = System.currentTimeMillis())

class ConversationViewModel(
    private val aiService: AIService,
    private val scanRepo: ScanRepository
) : ViewModel() {
    val messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("ai", "你好！我是你的 AI 英语老师。\n\n先上传课程材料拍照识别，或直接开始自由对话练习口语！")
    ))
    val inputText = MutableStateFlow("")
    val isLoading = MutableStateFlow(false)

    fun sendMessage() {
        val text = inputText.value.trim()
        if (text.isBlank()) return
        val cur = messages.value.toMutableList()
        cur.add(ChatMessage("user", text))
        messages.value = cur
        inputText.value = ""
        isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = cur.map { it.role to it.content }
                val cs = CaseStudyItem("Free Talk", "Open-ended English conversation", emptyList())
                val result = aiService.generateConversation(cs, history, text)
                val updated = messages.value.toMutableList()
                result.onSuccess { reply -> updated.add(ChatMessage("ai", reply)) }
                    .onFailure { e -> updated.add(ChatMessage("ai", "错误：${e.message}\n请检查设置里的 API Key 是否正确。")) }
                messages.value = updated
            } catch (e: Exception) {
                val updated = messages.value.toMutableList()
                updated.add(ChatMessage("ai", "网络错误：${e.message}"))
                messages.value = updated
            }
            isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    aiService: AIService,
    scanRepo: ScanRepository,
    viewModel: ConversationViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ConversationViewModel(aiService, scanRepo) as T
    })
) {
    val messages by viewModel.messages.collectAsState()
    val input by viewModel.inputText.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Scaffold(topBar = {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet))).padding(horizontal = 20.dp, vertical = 50.dp)) {
            Text("💬 AI 对话练习", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { ChatBubble(it, it.role == "user") }
                if (loading) { item { Text("🤔 AI 正在思考...", color = TextMuted, fontSize = 12.sp) } }
                item { Spacer(Modifier.height(8.dp)) }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp).background(BackgroundDeep, RoundedCornerShape(30.dp)).border(1.dp, BorderDark, RoundedCornerShape(30.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}, Modifier.size(44.dp).clip(CircleShape).background(SurfaceDark)) { Text("🎤", fontSize = 20.sp) }
                OutlinedTextField(value = input, onValueChange = { viewModel.inputText.value = it }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    placeholder = { Text("输入你的回复...", color = TextDim) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent, unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    shape = RoundedCornerShape(30.dp), singleLine = true)
                IconButton(onClick = { viewModel.sendMessage() }, Modifier.size(44.dp).clip(CircleShape).background(PrimaryIndigo)) {
                    Icon(Icons.Default.Send, "Send", tint = TextWhite, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, isUser: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isUser) PrimaryIndigo else SurfaceDark,
            shape = RoundedCornerShape(16.dp).let { if (isUser) it.copy(bottomEnd = RoundedCornerShape(4.dp)) else it.copy(bottomStart = RoundedCornerShape(4.dp)) },
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(if (isUser) "👤 You" else "🤖 AI Teacher", color = if (isUser) TextWhite.copy(alpha = 0.6f) else TextMuted, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(msg.content, color = if (isUser) TextWhite else TextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}
