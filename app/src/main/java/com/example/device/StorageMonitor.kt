package com.example.device

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class StorageInfo(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val usedPercent: Int = 0,
    val totalGbFormatted: String = "0.0 GB",
    val freeGbFormatted: String = "0.0 GB",
    val usedGbFormatted: String = "0.0 GB",
    val sourceApi: String = "StorageStatsManager"
)

class StorageMonitor(private val context: Context) {

    private val appContext = context.applicationContext

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    init {
        refresh()
    }

    fun refresh(): StorageInfo {
        val info = queryStorageInfo()
        _storageInfo.value = info
        return info
    }

    private fun queryStorageInfo(): StorageInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val statsManager = appContext.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                if (statsManager != null) {
                    val totalBytes = statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                    val freeBytes = statsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                    if (totalBytes > 0L) {
                        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
                        val percent = ((usedBytes.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
                        return StorageInfo(
                            totalBytes = totalBytes,
                            freeBytes = freeBytes,
                            usedBytes = usedBytes,
                            usedPercent = percent,
                            totalGbFormatted = formatBytes(totalBytes),
                            freeGbFormatted = formatBytes(freeBytes),
                            usedGbFormatted = formatBytes(usedBytes),
                            sourceApi = "StorageStatsManager"
                        )
                    }
                }
            } catch (_: Exception) {
                // Fall back to StatFs if StorageStatsManager is unavailable or restricted
            }
        }

        return queryViaStatFs()
    }

    private fun queryViaStatFs(): StorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val percent = if (totalBytes > 0L) {
                ((usedBytes.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
            } else 0

            StorageInfo(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes,
                usedPercent = percent,
                totalGbFormatted = formatBytes(totalBytes),
                freeGbFormatted = formatBytes(freeBytes),
                usedGbFormatted = formatBytes(usedBytes),
                sourceApi = "StatFs"
            )
        } catch (_: Exception) {
            StorageInfo(sourceApi = "Unavailable")
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(Locale.US, "%.1f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format(Locale.US, "%.0f MB", mb)
        }
    }
}
