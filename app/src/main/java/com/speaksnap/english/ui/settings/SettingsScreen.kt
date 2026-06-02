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

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {
    // OCR
    val ocrProvider = MutableStateFlow("tongyi"); val ocrKey = MutableStateFlow("")
    // Text
    val textProvider = MutableStateFlow("deepseek"); val textKey = MutableStateFlow("")
    // Conversation
    val convoProvider = MutableStateFlow("doubao"); val convoKey = MutableStateFlow("")
    // Extra
    val doubaoKey = MutableStateFlow(""); val claudeKey = MutableStateFlow("")
    // General
    val userName = MutableStateFlow("Alex"); val saved = MutableStateFlow(false)

    init {
        viewModelScope.launch { repo.ocrProvider.collect { ocrProvider.value = it } }
        viewModelScope.launch { repo.ocrKey.collect { ocrKey.value = it } }
        viewModelScope.launch { repo.textProvider.collect { textProvider.value = it } }
        viewModelScope.launch { repo.textKey.collect { textKey.value = it } }
        viewModelScope.launch { repo.convoProvider.collect { convoProvider.value = it } }
        viewModelScope.launch { repo.convoKey.collect { convoKey.value = it } }
        viewModelScope.launch { repo.doubaoKey.collect { doubaoKey.value = it } }
        viewModelScope.launch { repo.claudeKey.collect { claudeKey.value = it } }
        viewModelScope.launch { repo.userName.collect { userName.value = it } }
    }

    fun save() = viewModelScope.launch {
        repo.setOcrProvider(ocrProvider.value); repo.setOcrKey(ocrKey.value)
        repo.setTextProvider(textProvider.value); repo.setTextKey(textKey.value)
        repo.setConvoProvider(convoProvider.value); repo.setConvoKey(convoKey.value)
        repo.setDoubaoKey(doubaoKey.value); repo.setClaudeKey(claudeKey.value)
        repo.setUserName(userName.value)
        saved.value = true
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
    val ocrProv by viewModel.ocrProvider.collectAsState(); val ocrK by viewModel.ocrKey.collectAsState()
    val txtProv by viewModel.textProvider.collectAsState(); val txtK by viewModel.textKey.collectAsState()
    val cnvProv by viewModel.convoProvider.collectAsState(); val cnvK by viewModel.convoKey.collectAsState()
    val dbKey by viewModel.doubaoKey.collectAsState(); val clKey by viewModel.claudeKey.collectAsState()
    val userName by viewModel.userName.collectAsState(); val saved by viewModel.saved.collectAsState()
    LaunchedEffect(saved) { if (saved) { kotlinx.coroutines.delay(2000); viewModel.saved.value = false } }

    val providers = listOf(
        "tongyi" to "☁️ 通义千问",
        "deepseek" to "🦈 DeepSeek",
        "doubao" to "🫘 豆包",
        "claude" to "🧠 Claude"
    )

    Scaffold(topBar = {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet))).padding(horizontal = 20.dp, vertical = 50.dp)) {
            Column {
                Text("⚙️ 多模型配置", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("每个任务独立选择服务商", color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp)
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ========== 📸 OCR 图片识别 ==========
            item {
                ProviderCard("📸 拍照OCR · 图片识别", "推荐：阿里通义千问 Qwen-VL-Max", ocrProv, ocrK,
                    onProviderChange = { viewModel.ocrProvider.value = it },
                    onKeyChange = { viewModel.ocrKey.value = it },
                    providers, PrimaryIndigo
                )
            }

            // ========== 📖 文本分析 ==========
            item {
                ProviderCard("📖 文本分析 · 内容提取", "推荐：DeepSeek V3 · ¥1/百万token", txtProv, txtK,
                    onProviderChange = { viewModel.textProvider.value = it },
                    onKeyChange = { viewModel.textKey.value = it },
                    providers, SuccessGreen
                )
            }

            // ========== 💬 AI 对话 ==========
            item {
                ProviderCard("💬 AI 对话 · 语音练习", "推荐：豆包 Doubao · 支持语音TTS", cnvProv, cnvK,
                    onProviderChange = { viewModel.convoProvider.value = it },
                    onKeyChange = { viewModel.convoKey.value = it },
                    providers, WarningAmber
                )
            }

            // ========== 备用 Key ==========
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🔑 备用 API Key", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("当以上配置无效时自动使用", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                        OutlinedTextField(value = dbKey, onValueChange = { viewModel.doubaoKey.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("🫘 豆包 API Key") },
                            placeholder = { Text("从火山方舟控制台获取", color = TextDim) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = clKey, onValueChange = { viewModel.claudeKey.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("🧠 Claude API Key") },
                            placeholder = { Text("sk-ant-api03-...", color = TextDim) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                    }
                }
            }

            // ========== 用户名 ==========
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("👤 你的名字", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = userName, onValueChange = { viewModel.userName.value = it }, modifier = Modifier.fillMaxWidth(), colors = fieldColors(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    }
                }
            }

            // Save
            item {
                Button(onClick = { viewModel.save() }, Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo), shape = RoundedCornerShape(14.dp)) {
                    Text(if (saved) "✅ 已保存" else "💾 保存所有配置", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Key 获取指引
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🔗 API Key 获取地址", color = TextWhite, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "☁️ 通义千问" to "https://bailian.console.aliyun.com",
                            "🦈 DeepSeek" to "https://platform.deepseek.com/api_keys",
                            "🫘 豆包" to "https://console.volcengine.com/ark",
                            "🧠 Claude" to "https://console.anthropic.com"
                        ).forEach { (name, url) ->
                            Text("$name: $url", color = TextDim, fontSize = 11.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }

            // About
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ℹ️ SpeakSnap v1.0.0", color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("🏰 New Oriental · 早日退休", color = TextSecondary, fontSize = 13.sp)
                        Text("拍照→OCR→卡片→对话，一条龙学英语", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderDark)

@Composable
fun ProviderCard(
    title: String, hint: String, provider: String, key: String,
    onProviderChange: (String) -> Unit, onKeyChange: (String) -> Unit,
    providers: List<Pair<String, String>>, accent: androidx.compose.ui.graphics.Color
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(hint, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

            // Provider chips
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                providers.forEach { (p, label) ->
                    FilterChip(selected = provider == p, onClick = { onProviderChange(p) }, label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = TextWhite))
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = key, onValueChange = onKeyChange, modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") }, placeholder = { Text("输入 $title 的 Key", color = TextDim) },
                visualTransformation = PasswordVisualTransformation(),
                colors = fieldColors(), shape = RoundedCornerShape(12.dp), singleLine = true
            )
        }
    }
}
