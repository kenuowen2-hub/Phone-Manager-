package com.example.device

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val batteryMonitor = BatteryMonitor(context)
    val storageMonitor = StorageMonitor(context)
    val ramMonitor = RamMonitor(context)

    private val _dashboardState = MutableStateFlow(DeviceDashboardState())
    val dashboardState: StateFlow<DeviceDashboardState> = _dashboardState.asStateFlow()

    init {
        scope.launch {
            batteryMonitor.batteryInfo.collect { battery ->
                _dashboardState.value = _dashboardState.value.copy(battery = battery)
            }
        }
        scope.launch {
            storageMonitor.storageInfo.collect { storage ->
                _dashboardState.value = _dashboardState.value.copy(storage = storage)
            }
        }
        scope.launch {
            ramMonitor.memoryInfo.collect { memory ->
                _dashboardState.value = _dashboardState.value.copy(memory = memory)
            }
        }
        refreshAll()
    }

    fun refreshAll() {
        batteryMonitor.refresh()
        val storage = storageMonitor.refresh()
        val mem = ramMonitor.refresh()
        val hasUsage = checkUsageStatsPermission()
        val recentApps = if (hasUsage) fetchRecentAppUsage() else emptyList()

        _dashboardState.value = _dashboardState.value.copy(
            battery = batteryMonitor.batteryInfo.value,
            storage = storage,
            memory = mem,
            hasUsageStatsPermission = hasUsage,
            recentAppsUsage = recentApps
        )
    }

    fun stop() {
        batteryMonitor.stopMonitoring()
        scope.cancel()
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
