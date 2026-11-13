package com.example

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BlockedRule
import com.example.data.local.SecurityPreferences
import com.example.device.BatteryMonitor
import com.example.device.InstalledAppsManager
import com.example.device.RamMonitor
import com.example.device.StorageMonitor
import com.example.filter.SearchFilterEngine
import com.example.security.BiometricHelper
import com.example.security.BiometricStatus
import com.example.security.PinSecurityHelper
import com.example.ui.theme.CoordinatedThemes
import com.example.ui.theme.ThemeColorEngine
import com.example.ui.theme.ThemeConfig
import com.example.ui.theme.ThemeMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Phone Manager", appName)
    }

    @Test
    fun `battery monitor tracks level and charging status from battery broadcast`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 76)
            putExtra(BatteryManager.EXTRA_SCALE, 100)
            putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
            putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_USB)
            putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            putExtra(BatteryManager.EXTRA_TEMPERATURE, 315)
            putExtra(BatteryManager.EXTRA_VOLTAGE, 4120)
        }
        @Suppress("DEPRECATION")
        context.sendStickyBroadcast(batteryIntent)

        val monitor = BatteryMonitor(context)
        val info = monitor.batteryInfo.value
        assertEquals(76, info.levelPercent)
        assertTrue(info.isCharging)
        assertEquals("USB Cable", info.chargingSource)
        assertEquals("Good", info.health)
        assertEquals(31.5f, info.temperatureCelsius, 0.01f)
        assertEquals(4120, info.voltageMv)
        monitor.stopMonitoring()
    }

    @Test
    fun `storage monitor reports non-negative disk space metrics`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storageMonitor = StorageMonitor(context)
        val info = storageMonitor.refresh()
        assertTrue(info.totalBytes >= 0L)
        assertTrue(info.freeBytes >= 0L)
        assertTrue(info.usedBytes >= 0L)
        assertTrue(info.usedPercent in 0..100)
        assertTrue(info.freeGbFormatted.isNotBlank())
        assertTrue(info.usedGbFormatted.isNotBlank())
    }

    @Test
    fun `ram monitor tracks available vs total memory via ActivityManager MemoryInfo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ramMonitor = RamMonitor(context)
        val info = ramMonitor.refresh()
        assertTrue(info.totalRamBytes >= 0L)
        assertTrue(info.availableRamBytes >= 0L)
        assertTrue(info.availableRamBytes <= info.totalRamBytes)
        assertTrue(info.availableFormatted.isNotBlank())
        assertTrue(info.totalFormatted.isNotBlank())
    }

    @Test
    fun `pin security helper generates salt and verifies correctly`() {
        val salt = PinSecurityHelper.generateSalt()
        assertTrue(salt.isNotBlank())

        val pin = "1234"
        val hash = PinSecurityHelper.hashPin(pin, salt)
        assertTrue(hash.isNotBlank())

        assertTrue(PinSecurityHelper.verifyPin("1234", salt, hash))
        assertFalse(PinSecurityHelper.verifyPin("0000", salt, hash))
        assertFalse(PinSecurityHelper.verifyPin("12345", salt, hash))
    }

    @Test
    fun `search filter engine blocks whole words and phrases accurately`() {
        val rules = listOf(
            BlockedRule(id = 1, pattern = "phishing", isWholeWord = true, description = "Phishing test"),
            BlockedRule(id = 2, pattern = "malware download", isWholeWord = false, description = "Malware test")
        )

        val res1 = SearchFilterEngine.checkQuery("look out for phishing attacks", rules)
        assertTrue(res1.isBlocked)
        assertEquals("phishing", res1.matchedRule?.pattern)

        val res3 = SearchFilterEngine.checkQuery("best recipes for dinner", rules)
        assertFalse(res3.isBlocked)

        val res4 = SearchFilterEngine.checkQuery("dangerous malware download link", rules)
        assertTrue(res4.isBlocked)
        assertEquals("malware download", res4.matchedRule?.pattern)
    }

    @Test
    fun `installed apps manager protects system critical apps from lockout`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = InstalledAppsManager(context)

        assertTrue(manager.isPackageCritical(context.packageName))
        assertTrue(manager.isPackageCritical("com.android.dialer"))
        assertTrue(manager.isPackageCritical("com.google.android.dialer"))
        assertTrue(manager.isPackageCritical("com.android.emergency"))
        assertTrue(manager.isPackageCritical("com.android.settings"))
        assertTrue(manager.isPackageCritical("com.android.packageinstaller"))
        assertFalse(manager.isPackageCritical("com.example.game.puzzle"))
    }

    @Test
    fun `search filter engine extracts queries from search URLs and respects word boundaries`() {
        val rules = listOf(
            BlockedRule(id = 1, pattern = "scam", isWholeWord = true, description = "Whole word rule"),
            BlockedRule(id = 2, pattern = "unsafe link", isWholeWord = false, description = "Phrase rule")
        )

        // Whole-word "scam" should not block "scamper"
        assertFalse(SearchFilterEngine.checkQuery("scamper away", rules).isBlocked)
        assertTrue(SearchFilterEngine.checkQuery("avoid this scam today", rules).isBlocked)

        // Extract query from URL
        val extracted = SearchFilterEngine.extractSearchQueryFromUrl("https://html.duckduckgo.com/html/?q=avoid+this+scam")
        assertEquals("avoid this scam", extracted)
        assertTrue(SearchFilterEngine.checkQuery(extracted ?: "", rules).isBlocked)

        assertNull(SearchFilterEngine.extractSearchQueryFromUrl("https://example.com/about"))
    }

    @Test
    fun `biometric helper returns valid status without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val status = BiometricHelper.checkStatus(context)
        when (status) {
            is BiometricStatus.Available -> assertTrue(true)
            is BiometricStatus.Unavailable -> assertTrue(status.reason.isNotBlank())
        }
    }

    @Test
    fun `security preferences enforces lockout after five failed PIN attempts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SecurityPreferences(context)
        assertTrue(prefs.setPin("4321"))
        assertTrue(prefs.verifyPin("4321"))

        repeat(5) {
            assertFalse(prefs.verifyPin("9999"))
        }

        // Even correct PIN is rejected during active lockout
        assertTrue(prefs.settings.value.lockoutUntilMillis > System.currentTimeMillis())
        assertFalse(prefs.verifyPin("4321"))
    }

    @Test
    fun `theme color engine guarantees accessible contrast for all presets and extreme custom colors`() {
        // Check all coordinated presets in both light and dark modes
        for (preset in CoordinatedThemes.presets) {
            for (isDark in listOf(false, true)) {
                val scheme = ThemeColorEngine.buildColorScheme(
                    ThemeConfig(mode = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT, presetId = preset.id),
                    isDark = isDark
                )
                assertTrue(ThemeColorEngine.contrastRatio(scheme.onPrimary, scheme.primary) >= 4.5)
                assertTrue(ThemeColorEngine.contrastRatio(scheme.primary, scheme.surface) >= 4.5)
                assertTrue(ThemeColorEngine.contrastRatio(scheme.onSecondary, scheme.secondary) >= 4.5)
                assertTrue(ThemeColorEngine.contrastRatio(scheme.secondary, scheme.surface) >= 4.5)
                assertTrue(ThemeColorEngine.contrastRatio(scheme.onSurface, scheme.surface) >= 4.5)
                assertTrue(ThemeColorEngine.contrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5)
            }
        }

        // Check extreme custom colors (yellow, pure white, pure black, neon cyan)
        val extremes = listOf(Color.Yellow, Color.White, Color.Black, Color.Cyan)
        for (mainColor in extremes) {
            for (accentColor in extremes) {
                for (isDark in listOf(false, true)) {
                    val customConfig = ThemeConfig(
                        mode = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT,
                        presetId = ThemeConfig.PRESET_CUSTOM,
                        customPrimaryArgb = mainColor.toArgb(),
                        customAccentArgb = accentColor.toArgb()
                    )
                    val scheme = ThemeColorEngine.buildColorScheme(customConfig, isDark = isDark)
                    assertTrue(ThemeColorEngine.contrastRatio(scheme.onPrimary, scheme.primary) >= 4.5)
                    assertTrue(ThemeColorEngine.contrastRatio(scheme.primary, scheme.surface) >= 4.5)
                    assertTrue(ThemeColorEngine.contrastRatio(scheme.onSecondary, scheme.secondary) >= 4.5)
                    assertTrue(ThemeColorEngine.contrastRatio(scheme.secondary, scheme.surface) >= 4.5)
                    assertTrue(ThemeColorEngine.contrastRatio(scheme.onSurface, scheme.surface) >= 4.5)
                }
            }
        }
    }

    @Test
    fun `security preferences persists and resets theme configuration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SecurityPreferences(context)

        val custom = ThemeConfig(
            mode = ThemeMode.DARK,
            presetId = ThemeConfig.PRESET_CUSTOM,
            customPrimaryArgb = 0xFF7C3AED.toInt(),
            customAccentArgb = 0xFF059669.toInt()
        )
        prefs.saveThemeConfig(custom)

        val reloadedPrefs = SecurityPreferences(context)
        assertEquals(custom, reloadedPrefs.settings.value.themeConfig)
        assertEquals("dark", reloadedPrefs.settings.value.themeMode)

        val reset = reloadedPrefs.resetThemeConfig()
        assertEquals(ThemeConfig.DEFAULT, reset)
        assertEquals(ThemeConfig.DEFAULT, SecurityPreferences(context).settings.value.themeConfig)
    }

    @Test
    fun `theme color engine parses and formats hex colors accurately`() {
        val parsed = ThemeColorEngine.parseHexColor("#0284c7")
        assertEquals(Color(0xFF0284C7), parsed)
        assertEquals("#0284C7", ThemeColorEngine.colorToHex(parsed!!))
        assertNull(ThemeColorEngine.parseHexColor("invalid"))
    }
}
