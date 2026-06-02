package com.speaksnap.english.ui.upload

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speaksnap.english.data.repository.AIService
import com.speaksnap.english.data.repository.ScanRepository
import com.speaksnap.english.data.repository.SettingsRepository
import com.speaksnap.english.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

enum class UploadState { IDLE, CAPTURED, PROCESSING, DONE, ERROR }

class UploadViewModel(
    private val aiService: AIService,
    private val scanRepo: ScanRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {
    val state = MutableStateFlow(UploadState.IDLE)
    val photoUri = MutableStateFlow<Uri?>(null)
    val photoPath = MutableStateFlow<String?>(null)
    val progress = MutableStateFlow(0f)
    val progressText = MutableStateFlow("")
    val errorMessage = MutableStateFlow<String?>(null)
    val manualText = MutableStateFlow("")
    val manualTitle = MutableStateFlow("")
    val showManualInput = MutableStateFlow(false)

    fun onPhotoTaken(context: android.content.Context, uri: Uri) {
        photoUri.value = uri
        photoPath.value = getRealPathFromUri(context, uri)
        state.value = UploadState.CAPTURED
    }

    fun startProcessing() {
        viewModelScope.launch {
            state.value = UploadState.PROCESSING
            progress.value = 0.15f
            progressText.value = "OCR 文字识别中..."

            try {
                val imagePath = photoPath.value ?: run {
                    state.value = UploadState.ERROR; errorMessage.value = "未找到图片"; return@launch
                }

                progress.value = 0.3f
                progressText.value = "AI 提取单词中..."

                val result = aiService.analyzeImage(imagePath)
                result.onSuccess { content ->
                    progress.value = 0.65f
                    progressText.value = "整理短语和语法..."
                    val title = content.title.ifBlank { "课程 ${Date()}" }
                    val scanId = scanRepo.createScan(title, photoPath.value)
                    progress.value = 0.85f
                    progressText.value = "分析 Case Study..."
                    scanRepo.updateScanWithResult(scanId, "Extracted", content)
                    progress.value = 1f
                    progressText.value = "整理完成 ✅"
                    state.value = UploadState.DONE
                }.onFailure { e ->
                    state.value = UploadState.ERROR
                    errorMessage.value = e.message ?: "AI 处理失败"
                }
            } catch (e: Exception) {
                state.value = UploadState.ERROR
                errorMessage.value = e.message ?: "未知错误"
            }
        }
    }

    fun processManualText() {
        viewModelScope.launch {
            state.value = UploadState.PROCESSING
            progressText.value = "AI 正在整理笔记..."

            try {
                val result = aiService.analyzeText(manualText.value, manualTitle.value.ifBlank { "手动输入" })
                result.onSuccess { content ->
                    val scanId = scanRepo.createScan(manualTitle.value.ifBlank { "手动笔记" }, null)
                    scanRepo.updateScanWithResult(scanId, manualText.value, content)
                    state.value = UploadState.DONE
                    showManualInput.value = false
                    manualText.value = ""
                    manualTitle.value = ""
                }.onFailure { e ->
                    state.value = UploadState.ERROR
                    errorMessage.value = e.message ?: "处理失败"
                }
            } catch (e: Exception) {
                state.value = UploadState.ERROR
                errorMessage.value = e.message
            }
        }
    }

    fun reset() { state.value = UploadState.IDLE; progress.value = 0f; errorMessage.value = null }

    private fun getRealPathFromUri(context: android.content.Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.MediaStore.Images.ImageColumns.DATA)
                if (idx >= 0) it.getString(idx) else null
            } else null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    aiService: AIService,
    scanRepo: ScanRepository,
    settingsRepo: SettingsRepository,
    onNavigateToResult: (Long) -> Unit,
    viewModel: UploadViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UploadViewModel(aiService, scanRepo, settingsRepo) as T
    })
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val progressText by viewModel.progressText.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()
    val manualText by viewModel.manualText.collectAsState()
    val manualTitle by viewModel.manualTitle.collectAsState()
    val showManual by viewModel.showManualInput.collectAsState()

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraImageUri?.let { viewModel.onPhotoTaken(context, it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "speaksnap_${System.currentTimeMillis()}.jpg")
            file.parentFile?.mkdirs()
            cameraImageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraImageUri?.let { cameraLauncher.launch(it) }
        }
    }

    LaunchedEffect(state) {
        if (state == UploadState.DONE) {
            kotlinx.coroutines.delay(500)
            viewModel.reset()
        }
    }

    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet))).padding(horizontal = 20.dp, vertical = 50.dp)) {
                Column {
                    Text("📸 上传课程内容", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("拍照或输入笔记，AI 帮你整理", color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            item {
                Card(
                    Modifier.fillMaxWidth().clickable {
                        val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (perm == PackageManager.PERMISSION_GRANTED) {
                            val file = File(context.cacheDir, "speaksnap_${System.currentTimeMillis()}.jpg")
                            file.parentFile?.mkdirs()
                            cameraImageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraImageUri?.let { cameraLauncher.launch(it) }
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, BorderLight)
                ) {
                    Column(Modifier.padding(40.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📸", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("点击拍照教材/笔记", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("AI 自动识别单词、短语、语法点", color = TextMuted, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(Modifier.weight(1f).clickable { viewModel.showManualInput.value = true }, colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, BorderDark)) {
                        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📝", fontSize = 30.sp); Text("手动输入笔记", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, BorderDark)) {
                        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎙️", fontSize = 30.sp); Text("上传录音", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Processing
            if (state == UploadState.PROCESSING) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, BorderLight)) {
                        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚙️", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("AI 正在整理...", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(progressText, color = TextMuted, fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)), color = PrimaryIndigo, trackColor = SurfaceDark)
                            Spacer(Modifier.height(8.dp))
                            Text("${(progress * 100).toInt()}%", color = TextDim, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Error
            if (state == UploadState.ERROR) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("❌ 处理失败", color = ErrorRed, fontWeight = FontWeight.Bold)
                            Text(errorMsg ?: "未知错误", color = TextSecondary, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.reset() }) { Text("重试") }
                        }
                    }
                }
            }

            // Manual input
            if (showManual) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("📝 手动输入笔记", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = manualTitle, onValueChange = { viewModel.manualTitle.value = it }, label = { Text("课程名称") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderDark))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = manualText, onValueChange = { viewModel.manualText.value = it }, label = { Text("输入笔记内容...") }, modifier = Modifier.fillMaxWidth().height(150.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderDark))
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.processManualText() }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo), shape = RoundedCornerShape(12.dp)) { Text("保存并整理") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.showManualInput.value = false }, Modifier.fillMaxWidth()) { Text("取消", color = TextMuted) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
