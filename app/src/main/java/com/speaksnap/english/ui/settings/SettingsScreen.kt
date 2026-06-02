package com.speaksnap.english.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speaksnap.english.data.repository.SettingsRepository
import com.speaksnap.english.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepo: SettingsRepository) : ViewModel() {
    val apiKey = MutableStateFlow("")
    val userName = MutableStateFlow("Alex")
    val model = MutableStateFlow("claude-sonnet-4-20250514")
    val accent = MutableStateFlow("american")
    val saved = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            settingsRepo.apiKey.collect { apiKey.value = it }
        }
        viewModelScope.launch {
            settingsRepo.userName.collect { userName.value = it }
        }
        viewModelScope.launch {
            settingsRepo.model.collect { model.value = it }
        }
        viewModelScope.launch {
            settingsRepo.accent.collect { accent.value = it }
        }
    }

    fun save() {
        viewModelScope.launch {
            settingsRepo.setApiKey(apiKey.value)
            settingsRepo.setUserName(userName.value)
            settingsRepo.setModel(model.value)
            settingsRepo.setAccent(accent.value)
            saved.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    viewModel: SettingsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(settingsRepo) as T
    })
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val model by viewModel.model.collectAsState()
    val accent by viewModel.accent.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) {
        if (saved) {
            kotlinx.coroutines.delay(2000)
            viewModel.saved.value = false
        }
    }

    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet))).padding(horizontal = 20.dp, vertical = 50.dp)) {
                Column {
                    Text("⚙️ 设置", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("管理你的 API Key 和偏好", color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // API Key
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🔑 Anthropic API Key", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("需要 Claude API Key 才能使用 AI 功能。获取地址：console.anthropic.com", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(
                            value = apiKey, onValueChange = { viewModel.apiKey.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("sk-ant-api03-...", color = TextDim) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderDark),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Model
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🤖 AI 模型", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("claude-sonnet-4-20250514" to "Sonnet 4", "claude-opus-4-20250514" to "Opus 4", "claude-haiku-4-5-20251001" to "Haiku 4.5").forEach { (m, label) ->
                                FilterChip(
                                    selected = model == m,
                                    onClick = { viewModel.model.value = m },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryIndigo, selectedLabelColor = TextWhite)
                                )
                            }
                        }
                    }
                }
            }

            // Accent
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🎤 语音偏好", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("american" to "🇺🇸 美式", "british" to "🇬🇧 英式", "business" to "📊 商务").forEach { (a, label) ->
                                FilterChip(
                                    selected = accent == a,
                                    onClick = { viewModel.accent.value = a },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryIndigo, selectedLabelColor = TextWhite)
                                )
                            }
                        }
                    }
                }
            }

            // User name
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("👤 你的名字", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userName, onValueChange = { viewModel.userName.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderDark),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Save button
            item {
                Button(onClick = { viewModel.save() }, Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo), shape = RoundedCornerShape(14.dp)) {
                    Text(if (saved) "✅ 已保存" else "💾 保存设置", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // About
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ℹ️ 关于 SpeakSnap", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("🏰 New Oriental · 早日退休", color = TextSecondary, fontSize = 13.sp)
                        Text("版本 1.0.0 (2026.06)", color = TextMuted, fontSize = 11.sp)
                        Text("拍照学英语 · AI 口语教练", color = TextDim, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("💡 技术支持：Claude API (Anthropic)\n💡 数据存储在本地，不会上传到第三方", color = TextDim, fontSize = 11.sp, lineHeight = 18.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
