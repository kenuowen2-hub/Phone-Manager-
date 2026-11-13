package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.security.PinSecurityHelper
import com.example.ui.theme.ThemeConfig
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

enum class LockSchedule {
    ALWAYS,
    GRACE_5_MINUTES,
    NIGHT_HOURS // e.g. 22:00 to 07:00
}

data class SecuritySettingsState(
    val hasPinConfigured: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val lockSchedule: LockSchedule = LockSchedule.ALWAYS,
    val nightStartHour: Int = 22,
    val nightEndHour: Int = 7,
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
    val themeMode: String = "system", // "system", "light", "dark"
    val themeConfig: ThemeConfig = ThemeConfig.DEFAULT
)

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "smart_phone_security_prefs",
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SecuritySettingsState> = _settings.asStateFlow()

    private var lastUnlockTime: Long = 0L

    init {
        // If not initialized, initialize default rules if first run
        if (!prefs.contains("initialized_defaults")) {
            prefs.edit().putBoolean("initialized_defaults", true).apply()
        }
    }

    private fun loadSettings(): SecuritySettingsState {
        val hash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        val scheduleStr = prefs.getString(KEY_LOCK_SCHEDULE, LockSchedule.ALWAYS.name) ?: LockSchedule.ALWAYS.name
        val schedule = try {
            LockSchedule.valueOf(scheduleStr)
        } catch (_: Exception) {
            LockSchedule.ALWAYS
        }

        val themeModeKey = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.storageKey) ?: ThemeMode.SYSTEM.storageKey
        val themePresetId = prefs.getString(KEY_THEME_PRESET, ThemeConfig.PRESET_OCEAN) ?: ThemeConfig.PRESET_OCEAN
        val customPrimary = prefs.getInt(KEY_CUSTOM_PRIMARY_ARGB, ThemeConfig.DEFAULT_CUSTOM_PRIMARY_ARGB)
        val customAccent = prefs.getInt(KEY_CUSTOM_ACCENT_ARGB, ThemeConfig.DEFAULT_CUSTOM_ACCENT_ARGB)
        val themeConfig = ThemeConfig(
            mode = ThemeMode.fromKey(themeModeKey),
            presetId = themePresetId,
            customPrimaryArgb = customPrimary,
            customAccentArgb = customAccent
        )

        return SecuritySettingsState(
            hasPinConfigured = hash.isNotBlank(),
            isAppLockEnabled = prefs.getBoolean(KEY_LOCK_ENABLED, false),
            isBiometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false),
            lockSchedule = schedule,
            nightStartHour = prefs.getInt(KEY_NIGHT_START, 22),
            nightEndHour = prefs.getInt(KEY_NIGHT_END, 7),
            failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            lockoutUntilMillis = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L),
            themeMode = themeModeKey,
            themeConfig = themeConfig
        )
    }

    fun setPin(newPin: String): Boolean {
        if (newPin.length < 4) return false
        val salt = PinSecurityHelper.generateSalt()
        val hash = PinSecurityHelper.hashPin(newPin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_LOCK_ENABLED, true)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        _settings.value = loadSettings()
        return true
    }

    fun verifyPin(pinInput: String): Boolean {
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (now < lockoutUntil) {
            return false
        }

        val salt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        val hash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        val matches = PinSecurityHelper.verifyPin(pinInput, salt, hash)

        if (matches) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            lastUnlockTime = now
            _settings.value = loadSettings()
            return true
        } else {
            val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
            if (attempts >= 5) {
                // 30 seconds lockout cooldown
                editor.putLong(KEY_LOCKOUT_UNTIL, now + 30_000L)
            }
            editor.apply()
            _settings.value = loadSettings()
            return false
        }
    }

    fun recordBiometricSuccess() {
        lastUnlockTime = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        _settings.value = loadSettings()
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
        _settings.value = loadSettings()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _settings.value = loadSettings()
    }

    fun setLockSchedule(schedule: LockSchedule, startHour: Int = 22, endHour: Int = 7) {
        prefs.edit()
            .putString(KEY_LOCK_SCHEDULE, schedule.name)
            .putInt(KEY_NIGHT_START, startHour)
            .putInt(KEY_NIGHT_END, endHour)
            .apply()
        _settings.value = loadSettings()
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, ThemeMode.fromKey(mode).storageKey).apply()
        _settings.value = loadSettings()
    }

    fun saveThemeConfig(config: ThemeConfig) {
        prefs.edit()
            .putString(KEY_THEME_MODE, config.mode.storageKey)
            .putString(KEY_THEME_PRESET, config.presetId)
            .putInt(KEY_CUSTOM_PRIMARY_ARGB, config.customPrimaryArgb)
            .putInt(KEY_CUSTOM_ACCENT_ARGB, config.customAccentArgb)
            .apply()
        _settings.value = loadSettings()
    }

    fun resetThemeConfig(): ThemeConfig {
        val defaultConfig = ThemeConfig.DEFAULT
        saveThemeConfig(defaultConfig)
        return defaultConfig
    }

    fun shouldCurrentlyLock(): Boolean {
        val current = _settings.value
        if (!current.isAppLockEnabled || !current.hasPinConfigured) return false

        val now = System.currentTimeMillis()
        return when (current.lockSchedule) {
            LockSchedule.ALWAYS -> true
            LockSchedule.GRACE_5_MINUTES -> {
                val gracePeriod = 5 * 60 * 1000L
                (now - lastUnlockTime) > gracePeriod
            }
            LockSchedule.NIGHT_HOURS -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (current.nightStartHour > current.nightEndHour) {
                    // overnight e.g. 22 to 7
                    hour >= current.nightStartHour || hour < current.nightEndHour
                } else {
                    hour in current.nightStartHour until current.nightEndHour
                }
            }
        }
    }

    companion object {
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LOCK_SCHEDULE = "lock_schedule"
        private const val KEY_NIGHT_START = "night_start"
        private const val KEY_NIGHT_END = "night_end"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_PRESET = "theme_preset"
        private const val KEY_CUSTOM_PRIMARY_ARGB = "theme_custom_primary_argb"
        private const val KEY_CUSTOM_ACCENT_ARGB = "theme_custom_accent_argb"
    }
}
