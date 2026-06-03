# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目：SpeakSnap — 拍照英语口语教练 (New Oriental · 早日退休)

### 技术栈
- **平台**: Android (Kotlin), minSdk 26, targetSdk 35
- **UI**: Jetpack Compose + Material3
- **架构**: MVVM + Repository + Room
- **AI 服务**: Anthropic Claude API (Vision OCR, Content Extraction, Conversation)
- **网络**: Retrofit + OkHttp + Gson
- **本地存储**: Room (SQLite) + DataStore Preferences
- **相机**: CameraX
- **图片加载**: Coil
- **导航**: Navigation Compose

### 项目结构
```
com.speaksnap.english/
├── data/
│   ├── local/          # Room DB, Entities, DAOs
│   ├── remote/         # ClaudeApiService, DTOs, NetworkModule
│   └── repository/     # ClaudeRepository, ScanRepository, SettingsRepository
├── ui/
│   ├── theme/          # SpeakSnap dark theme (purple/indigo)
│   ├── navigation/     # NavGraph + Screen routes
│   ├── home/           # Dashboard with stats, plans, recent courses
│   ├── upload/         # Camera + manual input + AI processing
│   ├── result/         # AI extracted content display
│   ├── flashcards/     # Anki-style flashcards with flip + TTS
│   ├── conversation/   # AI conversation practice
│   ├── plan/           # 5-day learning plan
│   └── settings/       # API key, model, accent preferences
└── SpeakSnapApp.kt / MainActivity.kt
```

### 数据流
1. User takes photo → CameraX captures image
2. Image compressed → base64 → Claude Vision API
3. Claude extracts → words, phrases, grammar, case studies
4. Save to Room → ScanRecord + VocabularyItems
5. Auto-generate → 5-day LearningPlan
6. Practice → Flashcards (spaced repetition) + AI Conversation

### 开发规范
- 代码风格：Kotlin 官方风格
- AI 交互：API 调用必须封装在 Repository 中
- 安全：API Key 存储在 DataStore，严禁硬编码
- 语言：代码注释英文，UI 字符串中文
- 主题：深色主题 (#0F0F1A bg, #4F46E5 primary, #7C3AED accent)

### 关键文件
- `app/build.gradle.kts` — 依赖配置
- `data/repository/ClaudeRepository.kt` — 所有 Claude API 交互 + Prompt
- `data/repository/ScanRepository.kt` — Room 数据操作 + 学习计划生成
- `ui/navigation/NavGraph.kt` — 路由图
- `MainActivity.kt` — 入口 + BottomNav

### 构建
```bash
./gradlew assembleDebug    # 构建 debug APK
./gradlew installDebug     # 安装到连接的设备
```
