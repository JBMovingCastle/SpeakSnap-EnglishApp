package com.speaksnap.english.data.repository

import com.speaksnap.english.data.local.AppDatabase
import com.speaksnap.english.data.local.entity.LearningPlan
import com.speaksnap.english.data.local.entity.ScanRecord
import com.speaksnap.english.data.local.entity.VocabularyItem
import com.speaksnap.english.data.remote.dto.*
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val db: AppDatabase) {

    // --- Scan Records ---
    fun getAllScans(): Flow<List<ScanRecord>> = db.scanRecordDao().getAll()

    fun getPendingScans(): Flow<List<ScanRecord>> = db.scanRecordDao().getByStatus("draft")

    suspend fun getScanById(id: Long): ScanRecord? = db.scanRecordDao().getById(id)

    suspend fun createScan(title: String, imageUri: String?): Long {
        val record = ScanRecord(title = title, imageUri = imageUri)
        return db.scanRecordDao().insert(record)
    }

    suspend fun updateScanWithResult(scanId: Long, extractedText: String, content: ExtractedContent) {
        val scan = db.scanRecordDao().getById(scanId) ?: return
        db.scanRecordDao().update(
            scan.copy(
                extractedText = extractedText,
                wordCount = content.words.size,
                phraseCount = content.phrases.size,
                grammarCount = content.grammarPoints.size,
                caseStudyCount = content.caseStudies.size,
                status = "processed"
            )
        )
        // Insert vocabulary items
        val items = mutableListOf<VocabularyItem>()
        content.words.forEach { w ->
            items.add(VocabularyItem(scanId = scanId, word = w.word, phonetic = w.phonetic, meaning = w.meaning, example = w.example, type = "word"))
        }
        content.phrases.forEach { p ->
            items.add(VocabularyItem(scanId = scanId, word = p.phrase, meaning = p.meaning, example = p.usage, type = "phrase"))
        }
        content.grammarPoints.forEach { g ->
            items.add(VocabularyItem(scanId = scanId, word = g.title, meaning = g.explanation, example = g.example, type = "grammar"))
        }
        content.caseStudies.forEach { c ->
            items.add(VocabularyItem(scanId = scanId, word = c.title, meaning = c.scenario, example = c.starterDialogue, type = "case_study"))
        }
        if (items.isNotEmpty()) {
            db.vocabularyItemDao().insertAll(items)
        }
        // Generate 5-day learning plan
        generateLearningPlan(scanId, content)
    }

    private suspend fun generateLearningPlan(scanId: Long, content: ExtractedContent) {
        val plans = listOf(
            LearningPlan(scanId = scanId, dayNumber = 1, title = "Day 1 · 词汇积累", description = "学习 ${content.words.take(7).joinToString(", ") { it.word }} 等 ${content.words.size} 个单词", taskType = "word", estimatedMinutes = 25),
            LearningPlan(scanId = scanId, dayNumber = 2, title = "Day 2 · 短语与语法", description = "${content.phrases.size} 个短语跟读 + ${content.grammarPoints.size} 个语法点", taskType = "phrase", estimatedMinutes = 30),
            LearningPlan(scanId = scanId, dayNumber = 3, title = "Day 3 · AI 对话实战", description = "基于 ${content.caseStudies.firstOrNull()?.title ?: "课程内容"} 进行对话练习", taskType = "dialogue", estimatedMinutes = 35),
            LearningPlan(scanId = scanId, dayNumber = 4, title = "Day 4 · 巩固复习", description = "${content.words.size + content.phrases.size} 个词汇短语总复习，错题本重点突破", taskType = "review", estimatedMinutes = 30),
            LearningPlan(scanId = scanId, dayNumber = 5, title = "Day 5 · 综合演练", description = "Case Study 完整演练，最终测试", taskType = "dialogue", estimatedMinutes = 40),
        )
        db.learningPlanDao().insertAll(plans)
    }

    // --- Vocabulary ---
    fun getVocabularyByScanId(scanId: Long): Flow<List<VocabularyItem>> = db.vocabularyItemDao().getByScanId(scanId)

    fun getWordsByScanId(scanId: Long): Flow<List<VocabularyItem>> = db.vocabularyItemDao().getByScanIdAndType(scanId, "word")

    fun getPhrasesByScanId(scanId: Long): Flow<List<VocabularyItem>> = db.vocabularyItemDao().getByScanIdAndType(scanId, "phrase")

    fun getWeakItems(limit: Int = 20): Flow<List<VocabularyItem>> = db.vocabularyItemDao().getWeakItems(limit)

    fun getMistakeWords(): Flow<List<VocabularyItem>> = db.vocabularyItemDao().getMistakeWords()

    suspend fun updateMastery(id: Long, mastery: Int) = db.vocabularyItemDao().updateMastery(id, mastery)

    suspend fun incrementMistake(id: Long) = db.vocabularyItemDao().incrementMistake(id)

    suspend fun updateVocabulary(item: VocabularyItem) = db.vocabularyItemDao().update(item)

    // --- Learning Plans ---
    fun getPlansByScanId(scanId: Long): Flow<List<LearningPlan>> = db.learningPlanDao().getByScanId(scanId)

    suspend fun markPlanCompleted(id: Long) = db.learningPlanDao().markCompleted(id)
}
