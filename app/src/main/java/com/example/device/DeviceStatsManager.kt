package com.example.device

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Calendar

data class BatteryInfo(
    val levelPercent: Int = 0,
    val isCharging: Boolean = false,
    val chargingSource: String = "Not charging",
    val health: String = "Good",
    val temperatureCelsius: Float = 0f,
    val technology: String = "Li-ion",
    val voltageMv: Int = 0
)

data class StorageInfo(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val usedPercent: Int = 0,
    val totalGbFormatted: String = "0 GB",
    val freeGbFormatted: String = "0 GB",
    val usedGbFormatted: String = "0 GB"
)

data class MemoryInfoData(
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val usedRamBytes: Long = 0L,
    val usedPercent: Int = 0,
    val isLowMemory: Boolean = false
)

data class RecentAppUsageItem(
    val packageName: String,
    val appName: String,
    val foregroundTimeMillis: Long,
    val lastTimeUsed: Long
)

data class DeviceDashboardState(
    val battery: BatteryInfo = BatteryInfo(),
    val storage: StorageInfo = StorageInfo(),
    val memory: MemoryInfoData = MemoryInfoData(),
    val osVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val deviceModel: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
    val securityPatch: String = Build.VERSION.SECURITY_PATCH ?: "Not provided by OEM",
    val hasUsageStatsPermission: Boolean = false,
    val recentAppsUsage: List<RecentAppUsageItem> = emptyList(),
    val usagePermissionExplanation: String = "Android requires explicit user permission to read per-app screen time and usage statistics. Tap below to enable Usage Access in Android Settings."
)

class DeviceStatsManager(private val context: Context) {

    private val _dashboardState = MutableStateFlow(DeviceDashboardState())
    val dashboardState: StateFlow<DeviceDashboardState> = _dashboardState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            updateBatteryInfo(intent)
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialBatteryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                context.applicationContext.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } catch (_: Exception) {
                try {
                    context.applicationContext.registerReceiver(null, filter)
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            try {
                context.applicationContext.registerReceiver(batteryReceiver, filter)
            } catch (_: Exception) {
                try {
                    context.applicationContext.registerReceiver(null, filter)
                } catch (_: Exception) {
                    null
                }
            }
        }
        updateBatteryInfo(initialBatteryIntent)
        refreshAll()
    }

    fun refreshAll() {
        val storage = calculateStorage()
        val mem = calculateMemory()
        val hasUsage = checkUsageStatsPermission()
        val recentApps = if (hasUsage) fetchRecentAppUsage() else emptyList()

        _dashboardState.value = _dashboardState.value.copy(
            storage = storage,
            memory = mem,
            hasUsageStatsPermission = hasUsage,
            recentAppsUsage = recentApps
        )
    }

    private fun updateBatteryInfo(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) ((level.toFloat() / scale) * 100).toInt() else 0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val source = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure Reported"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempCelsius = tempTenths / 10f
        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

        _dashboardState.value = _dashboardState.value.copy(
            battery = BatteryInfo(
                levelPercent = percent,
                isCharging = isCharging,
                chargingSource = source,
                health = health,
                temperatureCelsius = tempCelsius,
                technology = tech,
                voltageMv = voltage
            )
        )
    }

    private fun calculateStorage(): StorageInfo {
        return try {
            val path: File = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes
            val percent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

            StorageInfo(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes,
                usedPercent = percent,
                totalGbFormatted = String.format("%.1f GB", totalBytes / (1024.0 * 1024 * 1024)),
                freeGbFormatted = String.format("%.1f GB", freeBytes / (1024.0 * 1024 * 1024)),
                usedGbFormatted = String.format("%.1f GB", usedBytes / (1024.0 * 1024 * 1024))
            )
        } catch (e: Exception) {
            StorageInfo()
        }
    }

    private fun calculateMemory(): MemoryInfoData {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)

            val total = memInfo.totalMem
            val avail = memInfo.availMem
            val used = total - avail
            val percent = if (total > 0) ((used.toDouble() / total) * 100).toInt() else 0

            MemoryInfoData(
                totalRamBytes = total,
                availableRamBytes = avail,
                usedRamBytes = used,
                usedPercent = percent,
                isLowMemory = memInfo.lowMemory
            )
        } catch (e: Exception) {
            MemoryInfoData()
        }
    }

    fun checkUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    private fun fetchRecentAppUsage(): List<RecentAppUsageItem> {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val startTime = cal.timeInMillis

            val queryUsageStats: List<UsageStats> = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: emptyList()

            val pm = context.packageManager
            queryUsageStats
                .filter { it.totalTimeInForeground > 1000L }
                .sortedByDescending { it.lastTimeUsed }
                .take(15)
                .map { stats ->
                    val appName = try {
                        val appInfo = pm.getApplicationInfo(stats.packageName, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        stats.packageName.substringAfterLast('.')
                    }
                    RecentAppUsageItem(
                        packageName = stats.packageName,
                        appName = appName,
                        foregroundTimeMillis = stats.totalTimeInForeground,
                        lastTimeUsed = stats.lastTimeUsed
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
