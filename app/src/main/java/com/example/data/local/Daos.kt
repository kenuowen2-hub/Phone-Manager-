package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedRuleDao {
    @Query("SELECT * FROM blocked_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<BlockedRule>>

    @Query("SELECT * FROM blocked_rules")
    suspend fun getAllRulesDirect(): List<BlockedRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: BlockedRule): Long

    @Update
    suspend fun updateRule(rule: BlockedRule)

    @Delete
    suspend fun deleteRule(rule: BlockedRule)

    @Query("UPDATE blocked_rules SET matchCount = matchCount + 1 WHERE id = :id")
    suspend fun incrementMatchCount(id: Long)
}

@Dao
interface SearchBlockedLogDao {
    @Query("SELECT * FROM search_blocked_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<SearchBlockedLog>>

    @Insert
    suspend fun insertLog(log: SearchBlockedLog)

    @Query("DELETE FROM search_blocked_logs")
    suspend fun clearAll()
}

@Dao
interface AppLockConfigDao {
    @Query("SELECT * FROM app_lock_configs ORDER BY appName ASC")
    fun getAllLockedApps(): Flow<List<AppLockConfig>>

    @Query("SELECT * FROM app_lock_configs WHERE packageName = :pkg LIMIT 1")
    suspend fun getConfigForPackage(pkg: String): AppLockConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLock(config: AppLockConfig)

    @Query("DELETE FROM app_lock_configs WHERE packageName = :pkg")
    suspend fun removeLock(pkg: String)
}

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcription_history ORDER BY timestamp DESC")
    fun getAllTranscriptions(): Flow<List<TranscriptionItem>>

    @Insert
    suspend fun insertTranscription(item: TranscriptionItem): Long

    @Delete
    suspend fun deleteTranscription(item: TranscriptionItem)

    @Query("DELETE FROM transcription_history")
    suspend fun clearAll()
}
