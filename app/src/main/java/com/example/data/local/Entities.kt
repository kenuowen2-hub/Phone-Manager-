package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_rules")
data class BlockedRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val isWholeWord: Boolean = true,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val matchCount: Int = 0
)

@Entity(tableName = "search_blocked_logs")
data class SearchBlockedLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val matchedRule: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_lock_configs")
data class AppLockConfig(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isLocked: Boolean = true,
    val lockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transcription_history")
data class TranscriptionItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transcribedText: String,
    val durationSeconds: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
