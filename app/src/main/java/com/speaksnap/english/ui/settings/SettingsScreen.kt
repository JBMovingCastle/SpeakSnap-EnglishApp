package com.speaksnap.english.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
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
    val provider = MutableStateFlow("deepseek")
    val deepseekKey = MutableStateFlow("")
    val deepseekModel = MutableStateFlow("deepseek-chat")
    val claudeKey = MutableStateFlow("")
    val claudeModel = MutableStateFlow("claude-sonnet-4-20250514")
    val userName = MutableStateFlow("Alex")
    val accent = MutableStateFlow("american")
    val saved = MutableStateFlow(false)

    init {
        viewModelScope.launch { settingsRepo.activeProvider.collect { provider.value = it } }
        viewModelScope.launch { settingsRepo.deepseekApiKey.collect { deepseekKey.value = it } }
        viewModelScope.launch { settingsRepo.deepseekModel.collect { deepseekModel.value = it } }
        viewModelScope.launch { settingsRepo.claudeApiKey.collect { claudeKey.value = it } }
        viewModelScope.launch { settingsRepo.claudeModel.collect { claudeModel.value = it } }
        viewModelScope.launch { settingsRepo.userName.collect { userName.value = it } }
        viewModelScope.launch { settingsRepo.accent.collect { accent.value = it } }
    }

    fun save() {
        viewModelScope.launch {
            settingsRepo.setProvider(provider.value)
            settingsRepo.setDeepSeekApiKey(deepseekKey.value)
            settingsRepo.setDeepSeekModel(deepseekModel.value)
            settingsRepo.setClaudeApiKey(claudeKey.value)
            settingsRepo.setClaudeModel(claudeModel.value)
            settingsRepo.setUserName(userName.value)
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
    val provider by viewModel.provider.collectAsState()
    val deepseekKey by viewModel.deepseekKey.collectAsState()
    val deepseekModel by viewModel.deepseekModel.collectAsState()
    val claudeKey by viewModel.claudeKey.collectAsState()
    val claudeModel by viewModel.claudeModel.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) {
        if (saved) { kotlinx.coroutines.delay(2000); viewModel.saved.value = false }
    }

    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet))).padding(horizontal = 20.dp, vertical = 50.dp)) {
                Column {
                    Text("⚙️ 设置", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("配置 AI 接口和偏好", color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ---- Provider Selector ----
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🤖 AI 服务商", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("选择你要用的大模型", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                "deepseek" to "🦈 DeepSeek",
                                "claude" to "🧠 Claude"
                            ).forEach { (p, label) ->
                                FilterChip(
                                    selected = provider == p,
                                    onClick = { viewModel.provider.value = p },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryIndigo, selectedLabelColor = TextWhite)
                                )
                            }
                        }
                    }
                }
            }

            // ---- DeepSeek Config ----
            if (provider == "deepseek") {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (provider == "deepseek") PrimaryIndigo else BorderDark)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("🦈 DeepSeek API Key", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("获取地址：platform.deepseek.com/api_keys", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = deepseekKey, onValueChange = { viewModel.deepseekKey.value = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("sk-...", color = TextDim) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderDark),
                                shape = RoundedCornerShape(12.dp), singleLine = true
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("模型", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "deepseek-chat" to "V3 (快)",
                                    "deepseek-reasoner" to "R1 (深思考)"
                                ).forEach { (m, label) ->
                                    FilterChip(
                                        selected = deepseekModel == m,
                                        onClick = { viewModel.deepseekModel.value = m },
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryIndigo, selectedLabelColor = TextWhite)
                                    )
                                }
                            }
                            Text("💡 DeepSeek V3 支持图片识别，速度快。R1 推理能力强但不支持图片。", color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }

            // ---- Claude Config ----
            if (provider == "claude") {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (provider == "claude") PrimaryViolet else BorderDark)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("🧠 Claude API Key", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("获取地址：console.anthropic.com", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = claudeKey, onValueChange = { viewModel.claudeKey.value = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("sk-ant-api03-...", color = TextDim) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark),
                                shape = RoundedCornerShape(12.dp), singleLine = true
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("模型", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "claude-sonnet-4-20250514" to "Sonnet 4",
                                    "claude-opus-4-20250514" to "Opus 4",
                                    "claude-haiku-4-5-20251001" to "Haiku 4.5"
                                ).forEach { (m, label) ->
                                    FilterChip(
                                        selected = claudeModel == m,
                                        onClick = { viewModel.claudeModel.value = m },
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryViolet, selectedLabelColor = TextWhite)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- User Name ----
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("👤 你的名字", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userName, onValueChange = { viewModel.userName.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderDark),
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                    }
                }
            }

            // ---- Save ----
            item {
                Button(onClick = { viewModel.save() }, Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo), shape = RoundedCornerShape(14.dp)) {
                    Text(if (saved) "✅ 已保存" else "💾 保存设置", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ---- About ----
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ℹ️ 关于 SpeakSnap", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("🏰 New Oriental · 早日退休", color = TextSecondary, fontSize = 13.sp)
                        Text("版本 1.0.0 (2026.06)", color = TextMuted, fontSize = 11.sp)
                        Text("支持 DeepSeek & Claude · 默认 DeepSeek", color = TextDim, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("💡 API Key 加密存储在本地\n💡 DeepSeek V3: ¥1/百万token，性价比极高\n💡 R1: ¥4/百万token，深度推理", color = TextDim, fontSize = 11.sp, lineHeight = 18.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
