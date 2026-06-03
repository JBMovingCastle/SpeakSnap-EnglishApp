package com.speaksnap.english.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.speaksnap.english.data.local.dao.LearningPlanDao
import com.speaksnap.english.data.local.dao.ScanRecordDao
import com.speaksnap.english.data.local.dao.UserStatsDao
import com.speaksnap.english.data.local.dao.VocabularyItemDao
import com.speaksnap.english.data.local.entity.LearningPlan
import com.speaksnap.english.data.local.entity.ScanRecord
import com.speaksnap.english.data.local.entity.UserStats
import com.speaksnap.english.data.local.entity.VocabularyItem

@Database(
    entities = [ScanRecord::class, VocabularyItem::class, LearningPlan::class, UserStats::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanRecordDao(): ScanRecordDao
    abstract fun vocabularyItemDao(): VocabularyItemDao
    abstract fun learningPlanDao(): LearningPlanDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speaksnap.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
