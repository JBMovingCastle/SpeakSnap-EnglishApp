package com.speaksnap.english.data.local.dao

import androidx.room.*
import com.speaksnap.english.data.local.entity.LearningPlan
import com.speaksnap.english.data.local.entity.ScanRecord
import com.speaksnap.english.data.local.entity.UserStats
import com.speaksnap.english.data.local.entity.VocabularyItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {
    @Query("SELECT * FROM scan_records ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: String): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE id = :id")
    suspend fun getById(id: Long): ScanRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ScanRecord): Long

    @Update
    suspend fun update(record: ScanRecord)

    @Delete
    suspend fun delete(record: ScanRecord)

    @Query("SELECT COUNT(*) FROM scan_records")
    suspend fun count(): Int
}

@Dao
interface VocabularyItemDao {
    @Query("SELECT * FROM vocabulary_items WHERE scanId = :scanId ORDER BY type, id")
    fun getByScanId(scanId: Long): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary_items WHERE scanId = :scanId AND type = :type")
    fun getByScanIdAndType(scanId: Long, type: String): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary_items WHERE id = :id")
    suspend fun getById(id: Long): VocabularyItem?

    @Query("SELECT * FROM vocabulary_items WHERE mastery < 3 ORDER BY mistakeCount DESC, lastReviewed ASC LIMIT :limit")
    fun getWeakItems(limit: Int = 20): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary_items WHERE type = 'word' ORDER BY mistakeCount DESC LIMIT :limit")
    fun getMistakeWords(limit: Int = 50): Flow<List<VocabularyItem>>

    @Query("SELECT COUNT(*) FROM vocabulary_items WHERE scanId = :scanId")
    suspend fun countByScanId(scanId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VocabularyItem>): List<Long>

    @Update
    suspend fun update(item: VocabularyItem)

    @Query("UPDATE vocabulary_items SET mastery = :mastery, lastReviewed = :now WHERE id = :id")
    suspend fun updateMastery(id: Long, mastery: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE vocabulary_items SET mistakeCount = mistakeCount + 1 WHERE id = :id")
    suspend fun incrementMistake(id: Long)

    @Delete
    suspend fun delete(item: VocabularyItem)
}

@Dao
interface LearningPlanDao {
    @Query("SELECT * FROM learning_plans WHERE scanId = :scanId ORDER BY dayNumber")
    fun getByScanId(scanId: Long): Flow<List<LearningPlan>>

    @Query("SELECT * FROM learning_plans WHERE scanId = :scanId AND dayNumber = :day")
    fun getByScanIdAndDay(scanId: Long, day: Int): Flow<List<LearningPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<LearningPlan>)

    @Update
    suspend fun update(plan: LearningPlan)

    @Query("UPDATE learning_plans SET isCompleted = :done, completedAt = :now WHERE id = :id")
    suspend fun markCompleted(id: Long, done: Boolean = true, now: Long = System.currentTimeMillis())
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun get(): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UserStats)

    @Query("UPDATE user_stats SET streakDays = :streak, totalStudyDays = totalStudyDays + 1, lastStudyDate = :today")
    suspend fun updateStreak(streak: Int, today: Long = System.currentTimeMillis())
}
