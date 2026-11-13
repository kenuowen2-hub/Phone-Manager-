package com.example.device

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class MemoryInfoData(
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val usedRamBytes: Long = 0L,
    val usedPercent: Int = 0,
    val isLowMemory: Boolean = false,
    val availableFormatted: String = "0 MB",
    val totalFormatted: String = "0 MB",
    val usedFormatted: String = "0 MB"
)

class RamMonitor(private val context: Context) {

    private val appContext = context.applicationContext

    private val _memoryInfo = MutableStateFlow(MemoryInfoData())
    val memoryInfo: StateFlow<MemoryInfoData> = _memoryInfo.asStateFlow()

    init {
        refresh()
    }

    fun refresh(): MemoryInfoData {
        val info = queryMemoryInfo()
        _memoryInfo.value = info
        return info
    }

    private fun queryMemoryInfo(): MemoryInfoData {
        return try {
            val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return MemoryInfoData()
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)

            val total = memInfo.totalMem.coerceAtLeast(0L)
            val avail = memInfo.availMem.coerceIn(0L, total.coerceAtLeast(0L))
            val used = (total - avail).coerceAtLeast(0L)
            val percent = if (total > 0L) {
                ((used.toDouble() / total) * 100).toInt().coerceIn(0, 100)
            } else 0

            MemoryInfoData(
                totalRamBytes = total,
                availableRamBytes = avail,
                usedRamBytes = used,
                usedPercent = percent,
                isLowMemory = memInfo.lowMemory,
                availableFormatted = formatRam(avail),
                totalFormatted = formatRam(total),
                usedFormatted = formatRam(used)
            )
        } catch (_: Exception) {
            MemoryInfoData()
        }
    }

    private fun formatRam(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(Locale.US, "%.2f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format(Locale.US, "%.0f MB", mb)
        }
    }
}
