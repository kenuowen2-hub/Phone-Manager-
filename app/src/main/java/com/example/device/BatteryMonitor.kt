package com.example.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryInfo(
    val levelPercent: Int = 0,
    val isCharging: Boolean = false,
    val chargingSource: String = "Not charging",
    val health: String = "Good",
    val temperatureCelsius: Float = 0f,
    val technology: String = "Li-ion",
    val voltageMv: Int = 0
)

class BatteryMonitor(private val context: Context) {

    private val appContext = context.applicationContext
    private val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private var isRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            updateFromIntent(intent)
        }
    }

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val initialIntent = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appContext.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    appContext.registerReceiver(batteryReceiver, filter)
                }
            } catch (_: Exception) {
                try {
                    appContext.registerReceiver(null, filter)
                } catch (_: Exception) {
                    null
                }
            }
            isRegistered = true
            updateFromIntent(initialIntent)
        } else {
            refresh()
        }
    }

    fun stopMonitoring() {
        if (isRegistered) {
            try {
                appContext.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {
            }
            isRegistered = false
        }
    }

    fun refresh() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = try {
            appContext.registerReceiver(null, filter)
        } catch (_: Exception) {
            null
        }
        updateFromIntent(stickyIntent)
    }

    private fun updateFromIntent(intent: Intent?) {
        val managerCapacity = try {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 }
        } catch (_: Exception) {
            null
        }

        val managerCharging = try {
            batteryManager?.isCharging
        } catch (_: Exception) {
            null
        }

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val intentPercent = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale) * 100).toInt().coerceIn(0, 100)
        } else {
            null
        }

        val percent = intentPercent ?: managerCapacity ?: 0

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = if (intent != null && status != -1) {
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        } else {
            managerCharging ?: false
        }

        val chargePlug = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val source = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> when (status) {
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                else -> if (isCharging) "Charging" else "On Battery"
            }
        }

        val healthCode = intent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_GOOD
        ) ?: BatteryManager.BATTERY_HEALTH_GOOD

        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure Reported"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempTenths / 10f
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() } ?: "Li-ion"
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        _batteryInfo.value = BatteryInfo(
            levelPercent = percent,
            isCharging = isCharging,
            chargingSource = source,
            health = health,
            temperatureCelsius = tempCelsius,
            technology = tech,
            voltageMv = voltage
        )
    }
}
