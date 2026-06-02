# SpeakSnap — 拍照英语口语教练 🏰

> **New Oriental · 早日退休** — 拍照学英语，AI 口语教练

[![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)](https://github.com/JBMovingCastle/SpeakSnap-EnglishApp)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Claude AI](https://img.shields.io/badge/AI-Claude%20API-d97706)](https://anthropic.com)

## 核心功能

| 功能 | 描述 |
|------|------|
| 📸 **拍照 OCR** | 拍摄教材/笔记，Claude Vision 自动提取单词、短语、语法点、Case Study |
| 📖 **记忆卡片** | Anki 风格翻转卡片，间隔重复 + 错题本追踪 |
| 💬 **AI 对话** | 基于提取内容的场景对话练习，AI 实时纠正 |
| 📅 **学习计划** | 自动生成 5 天课程计划（词汇→短语→对话→复习→综合） |
| 📊 **学习统计** | 连续打卡、单词掌握率、进步追踪 |

## 技术栈

- **UI**: Jetpack Compose + Material3 (暗色紫靛主题)
- **架构**: MVVM + Repository + Room
- **AI**: Anthropic Claude API (Vision + Sonnet)
- **网络**: Retrofit + OkHttp + Gson
- **存储**: Room + DataStore Preferences
- **相机**: CameraX

## 项目结构

```
com.speaksnap.english/
├── data/
│   ├── local/         → Room 数据库 (4 实体 + 4 DAO)
│   ├── remote/        → Claude API 服务 + DTOs
│   └── repository/    → ClaudeRepo, ScanRepo, SettingsRepo
├── ui/
│   ├── home/          → 首页 Dashboard
│   ├── upload/        → 拍照 + AI 处理
│   ├── result/        → AI 整理结果
│   ├── flashcards/    → 记忆卡片 + 错题本
│   ├── conversation/  → AI 对话练习
│   ├── plan/          → 5 天学习计划
│   └── settings/      → API Key / 偏好
└── MainActivity.kt    → Bottom Nav 导航入口
```

## 构建

```bash
# 安装依赖并构建 Debug APK
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

需要 Android Studio Hedgehog+ 和 JDK 17。

## 配置

1. 安装 APK 到手机
2. 进入「设置」→ 输入 Anthropic API Key (从 [console.anthropic.com](https://console.anthropic.com) 获取)
3. 选择 AI 模型和语音偏好
4. 开始拍照学习！

## 预览

基于 HTML 设计稿开发，完整的暗色主题界面，适配小米 15 Pro (1440x3200)。

---

**Made with Claude Code** 🤖
