package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val isCriticalProtected: Boolean, // Emergency, dialer, settings, installer, self -> cannot lock
    val nonLockableReason: String? = null,
    val apkSizeBytes: Long = 0L,
    val firstInstallTime: Long = 0L,
    val isLocked: Boolean = false
)

enum class AppSortOption {
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    INSTALL_TIME_DESC
}

class InstalledAppsManager(private val context: Context) {

    private val criticalPackages = setOf(
        context.packageName,
        "com.android.settings",
        "com.google.android.settings",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.server.telecom",
        "com.android.phone",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.emergency",
        "com.google.android.apps.safetyhub"
    )

    suspend fun getInstalledApps(lockedPackageNames: Set<String>): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        val seen = mutableSetOf<String>()
        val result = mutableListOf<AppItem>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (seen.add(pkg)) {
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val pkgInfo = try {
                        pm.getPackageInfo(pkg, 0)
                    } catch (_: Exception) {
                        null
                    }

                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isCritical = isPackageCritical(pkg)
                    val reason = if (isCritical) {
                        getCriticalReason(pkg)
                    } else null

                    val apkSize = try {
                        val file = File(appInfo.sourceDir)
                        if (file.exists()) file.length() else 0L
                    } catch (_: Exception) {
                        0L
                    }

                    val installTime = pkgInfo?.firstInstallTime ?: 0L
                    val versionName = pkgInfo?.versionName ?: "1.0"

                    result.add(
                        AppItem(
                            packageName = pkg,
                            appName = label,
                            versionName = versionName,
                            isSystemApp = isSystem,
                            isCriticalProtected = isCritical,
                            nonLockableReason = reason,
                            apkSizeBytes = apkSize,
                            firstInstallTime = installTime,
                            isLocked = if (isCritical) false else lockedPackageNames.contains(pkg)
                        )
                    )
                } catch (e: Exception) {
                    // Skip unreadable package
                }
            }
        }

        result.sortedBy { it.appName.lowercase() }
    }

    fun isPackageCritical(packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (criticalPackages.contains(packageName)) return true
        val lower = packageName.lowercase()
        if (lower.contains("dialer") || lower.contains("emergency") || lower.contains("packageinstaller") || lower.contains("settings")) {
            return true
        }
        return false
    }

    private fun getCriticalReason(packageName: String): String {
        return when {
            packageName == context.packageName -> "This Manager cannot lock itself to prevent user lockout."
            packageName.contains("dialer") || packageName.contains("emergency") || packageName.contains("phone") ->
                "Emergency calling & dialers are never locked for user safety."
            packageName.contains("settings") ->
                "Android System Settings must remain accessible to prevent entrapment."
            packageName.contains("packageinstaller") ->
                "Package manager must remain accessible to allow uninstalling apps."
            else -> "System-critical component protected by Android safety policy."
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
