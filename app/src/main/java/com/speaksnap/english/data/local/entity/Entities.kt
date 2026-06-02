package com.speaksnap.english.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val imageUri: String? = null,
    val extractedText: String = "",
    val wordCount: Int = 0,
    val phraseCount: Int = 0,
    val grammarCount: Int = 0,
    val caseStudyCount: Int = 0,
    val status: String = "draft", // draft, processed, archived
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vocabulary_items",
    foreignKeys = [
        ForeignKey(
            entity = ScanRecord::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scanId")]
)
data class VocabularyItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanId: Long,
    val word: String,
    val phonetic: String = "",
    val meaning: String = "",
    val example: String = "",
    val type: String = "word", // word, phrase, grammar, case_study
    val mastery: Int = 0, // 0=not learned, 1-5 spaced repetition
    val mistakeCount: Int = 0,
    val lastReviewed: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "learning_plans",
    foreignKeys = [
        ForeignKey(
            entity = ScanRecord::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scanId")]
)
data class LearningPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanId: Long,
    val weekNumber: Int = 1,
    val dayNumber: Int, // 1-5
    val title: String,
    val description: String = "",
    val taskType: String = "word", // word, phrase, dialogue, review
    val estimatedMinutes: Int = 25,
    val isCompleted: Boolean = false,
    val completedAt: Long = 0
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val streakDays: Int = 0,
    val totalWords: Int = 0,
    val totalPhrases: Int = 0,
    val totalConversations: Int = 0,
    val accuracyRate: Float = 0f,
    val lastStudyDate: Long = 0,
    val totalStudyDays: Int = 0
)
