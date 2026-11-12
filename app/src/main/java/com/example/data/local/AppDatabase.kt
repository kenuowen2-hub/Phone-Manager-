package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BlockedRule::class,
        SearchBlockedLog::class,
        AppLockConfig::class,
        TranscriptionItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedRuleDao(): BlockedRuleDao
    abstract fun searchBlockedLogDao(): SearchBlockedLogDao
    abstract fun appLockConfigDao(): AppLockConfigDao
    abstract fun transcriptionDao(): TranscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_phone_manager.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
