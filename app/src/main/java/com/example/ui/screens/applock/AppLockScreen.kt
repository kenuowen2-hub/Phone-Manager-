package com.example.ui.screens.applock

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.LockSchedule
import com.example.device.AppItem
import com.example.device.AppSortOption
import com.example.security.BiometricHelper
import com.example.security.BiometricStatus
import com.example.ui.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val installedApps by viewModel.installedApps.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()
    val securitySettings by viewModel.securitySettings.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val biometricStatus by viewModel.biometricStatus.collectAsState()
    val searchQuery by viewModel.appSearchQuery.collectAsState()
    val sortOption by viewModel.appSortOption.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showPermissionInfoDialog by remember { mutableStateOf(false) }
    var showEnableLockPromptDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "user", "system", "locked"

    // App Lock Gate dialog state
    val isGateOpen by viewModel.isLockGateOpen.collectAsState()
    val targetApp by viewModel.testLockApp.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDashboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Filter apps
    val filteredApps = remember(installedApps, searchQuery, selectedFilter, sortOption) {
        var list = installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
        list = when (selectedFilter) {
            "user" -> list.filter { !it.isSystemApp }
            "system" -> list.filter { it.isSystemApp }
            "locked" -> list.filter { it.isLocked }
            else -> list
        }
        when (sortOption) {
            AppSortOption.NAME_ASC -> list.sortedBy { it.appName.lowercase() }
            AppSortOption.NAME_DESC -> list.sortedByDescending { it.appName.lowercase() }
            AppSortOption.SIZE_DESC -> list.sortedByDescending { it.apkSizeBytes }
            AppSortOption.INSTALL_TIME_DESC -> list.sortedByDescending { it.firstInstallTime }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "App Lock",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App Manager & Lock", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPermissionInfoDialog = true },
                        modifier = Modifier.testTag("app_lock_info_button")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Android Permissions Guide")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Master Controls Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("app_lock_controls_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "App Lock Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (securitySettings.hasPinConfigured) {
                                    if (securitySettings.isAppLockEnabled) "Protection is Active" else "Protection is Disabled"
                                } else "No PIN set yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (securitySettings.isAppLockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = securitySettings.isAppLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !securitySettings.hasPinConfigured) {
                                    showPinDialog = true
                                } else if (enabled && !dashboardState.hasUsageStatsPermission) {
                                    showEnableLockPromptDialog = true
                                } else {
                                    viewModel.setAppLockMasterEnabled(enabled)
                                }
                            },
                            modifier = Modifier.testTag("master_lock_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPinDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_pin_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (securitySettings.hasPinConfigured) "Change PIN" else "Set PIN")
                        }

                        OutlinedButton(
                            onClick = { showScheduleDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_button")
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                when (securitySettings.lockSchedule) {
                                    LockSchedule.ALWAYS -> "Always"
                                    LockSchedule.GRACE_5_MINUTES -> "5m Grace"
                                    LockSchedule.NIGHT_HOURS -> "Night"
                                }
                            )
                        }
                    }

                    if (securitySettings.hasPinConfigured) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Biometric Unlock", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                if (biometricStatus is BiometricStatus.Unavailable) {
                                    Text(
                                        (biometricStatus as BiometricStatus.Unavailable).reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = securitySettings.isBiometricEnabled && biometricStatus is BiometricStatus.Available,
                                enabled = biometricStatus is BiometricStatus.Available,
                                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                                modifier = Modifier.testTag("biometric_switch")
                            )
                        }
                    }
                }
            }

            // Search & Sort bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setAppSearchQuery(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_apps_input"),
                    placeholder = { Text("Search installed apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("sort_apps_button")
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Apps")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Name (A to Z)") },
                            onClick = {
                                viewModel.setAppSortOption(AppSortOption.NAME_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (Z to A)") },
                            onClick = {
                                viewModel.setAppSortOption(AppSortOption.NAME_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("App Size (Largest)") },
                            onClick = {
                                viewModel.setAppSortOption(AppSortOption.SIZE_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Install Date (Newest)") },
                            onClick = {
                                viewModel.setAppSortOption(AppSortOption.INSTALL_TIME_DESC)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text("All (${installedApps.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "user",
                        onClick = { selectedFilter = "user" },
                        label = { Text("User Apps") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "system",
                        onClick = { selectedFilter = "system" },
                        label = { Text("System Apps") }
                    )
                }
                item {
                    val lockedCount = installedApps.count { it.isLocked }
                    FilterChip(
                        selected = selectedFilter == "locked",
                        onClick = { selectedFilter = "locked" },
                        label = { Text("Locked ($lockedCount)") }
                    )
                }
            }

            // App List
            if (isLoading && installedApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Loading installed apps...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No apps match your filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "enforcement_status_banner") {
                        LockEnforcementStatusCard(
                            hasUsageStatsPermission = dashboardState.hasUsageStatsPermission,
                            biometricStatus = biometricStatus,
                            onOpenUsageSettings = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        )
                    }

                    items(filteredApps, key = { it.packageName }) { app ->
                        AppRowItem(
                            app = app,
                            onToggleLock = {
                                if (!securitySettings.hasPinConfigured) {
                                    showPinDialog = true
                                } else {
                                    viewModel.toggleAppLock(app)
                                }
                            },
                            onTestLaunch = {
                                viewModel.openLockGateForApp(app)
                            },
                            getIcon = { viewModel.appsManager.getAppIcon(app.packageName) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Set / Change PIN Dialog
    if (showPinDialog) {
        SetPinDialog(
            hasExistingPin = securitySettings.hasPinConfigured,
            onVerifyExistingPin = { currentPin -> viewModel.verifyPin(currentPin) },
            onDismiss = { showPinDialog = false },
            onConfirmPin = { pin ->
                val success = viewModel.setPin(pin)
                if (success) {
                    showPinDialog = false
                }
            }
        )
    }

    // Pre-Enable Permission & Limitation Disclosure Dialog
    if (showEnableLockPromptDialog) {
        AlertDialog(
            onDismissRequest = { showEnableLockPromptDialog = false },
            icon = {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Enable App Lock & Permissions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Before turning on App Lock, review how Android handles app protection:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "• Usage Access Permission: Needed only to inspect recent foreground app activity.\n\n" +
                                "• Android Platform Limitation: Without invasive Accessibility or System Overlay privileges, third-party apps cannot intercept taps from the Android home launcher.\n\n" +
                                "• Built-in Fallback (Secure App Launcher): Even without Usage Access, tapping the Launch (▶) icon next to any protected app here enforces your PIN or Biometric gate before opening it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAppLockMasterEnabled(true)
                        showEnableLockPromptDialog = false
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.testTag("grant_usage_and_enable_button")
                ) {
                    Text("Grant Usage Access")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setAppLockMasterEnabled(true)
                        showEnableLockPromptDialog = false
                    },
                    modifier = Modifier.testTag("enable_secure_launcher_only_button")
                ) {
                    Text("Use Secure Launcher")
                }
            }
        )
    }

    // Lock Schedule Dialog
    if (showScheduleDialog) {
        LockScheduleDialog(
            currentSchedule = securitySettings.lockSchedule,
            onDismiss = { showScheduleDialog = false },
            onSelect = { sched ->
                viewModel.setLockSchedule(sched)
                showScheduleDialog = false
            }
        )
    }

    // Android Permissions Explanation Dialog
    if (showPermissionInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Android App Lock & Safety")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "How App Locking Works on Android:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "1. Detection: Detecting when an app opens in the foreground requires Android Usage Access or Accessibility service.\n\n" +
                                "2. Safety Guarantee: To prevent trapping users, Smart Phone Manager strictly never locks emergency dialers, Android system settings, the package installer, or this manager itself. You can always disable or uninstall apps freely.\n\n" +
                                "3. Secure Credentials: Your PIN is hashed using SHA-256 with a unique cryptographic salt. It is never stored in plain text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        showPermissionInfoDialog = false
                    }
                ) {
                    Text("Open Usage Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Interactive Lock Gate Dialog
    if (isGateOpen && targetApp != null) {
        LockGateDialog(
            app = targetApp!!,
            securitySettings = securitySettings,
            biometricStatus = biometricStatus,
            onVerifyPin = { pin -> viewModel.verifyPin(pin) },
            onBiometricUnlock = { viewModel.recordBiometricSuccess() },
            onUnlocked = { viewModel.onLockGateUnlocked() },
            onDismiss = { viewModel.dismissLockGate() }
        )
    }
}

@Composable
fun LockEnforcementStatusCard(
    hasUsageStatsPermission: Boolean,
    biometricStatus: BiometricStatus,
    onOpenUsageSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lock_enforcement_status_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasUsageStatsPermission) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (hasUsageStatsPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasUsageStatsPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasUsageStatsPermission) "Secure App Launcher Active" else "Usage Access Not Granted",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (biometricStatus is BiometricStatus.Available) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        text = if (biometricStatus is BiometricStatus.Available) "Biometrics Ready" else "PIN Only",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Standard Android apps cannot block launches directly from the system home launcher without Accessibility or Overlay services. Launch protected apps from this list (tap ▶) to require PIN or Biometric verification.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasUsageStatsPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenUsageSettings,
                    modifier = Modifier.testTag("open_usage_settings_inline_button")
                ) {
                    Text("Grant Usage Access in Settings")
                }
            }
        }
    }
}

@Composable
fun AppRowItem(
    app: AppItem,
    onToggleLock: () -> Unit,
    onTestLaunch: () -> Unit,
    getIcon: () -> android.graphics.drawable.Drawable?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_item_${app.packageName}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isLocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            val drawable = remember(app.packageName) { getIcon() }
            val bitmap = remember(drawable) {
                drawable?.let {
                    try {
                        it.toBitmap(96, 96).asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                val sizeMb = if (app.apkSizeBytes > 0) String.format(Locale.US, "%.1f MB", app.apkSizeBytes / (1024.0 * 1024)) else "System"
                Text(
                    "${app.packageName} • $sizeMb",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (app.isCriticalProtected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        app.nonLockableReason ?: "Safety Protected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (app.isCriticalProtected) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Protected",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTestLaunch,
                        modifier = Modifier.testTag("test_launch_${app.packageName}")
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Test Lock Gate",
                            tint = if (app.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = app.isLocked,
                        onCheckedChange = { onToggleLock() },
                        modifier = Modifier.testTag("toggle_lock_${app.packageName}")
                    )
                }
            }
        }
    }
}

@Composable
fun SetPinDialog(
    hasExistingPin: Boolean,
    onVerifyExistingPin: ((String) -> Boolean)? = null,
    onDismiss: () -> Unit,
    onConfirmPin: (String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (hasExistingPin) "Change Security PIN" else "Set New Security PIN")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter a 4 to 6 digit security PIN. Your PIN is protected with salted SHA-256 cryptographic hashing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasExistingPin && onVerifyExistingPin != null) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("Current Security PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("current_pin_input_field"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text(if (hasExistingPin) "New Security PIN (4-6 digits)" else "Security PIN (4-6 digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Confirm Security PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_pin_input_field"),
                    singleLine = true
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasExistingPin && onVerifyExistingPin != null && !onVerifyExistingPin(currentPin)) {
                        errorMessage = "Current PIN is incorrect."
                    } else if (pin.length < 4) {
                        errorMessage = "PIN must be at least 4 digits."
                    } else if (pin != confirmPin) {
                        errorMessage = "PINs do not match."
                    } else {
                        onConfirmPin(pin)
                    }
                },
                modifier = Modifier.testTag("save_pin_button")
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LockScheduleDialog(
    currentSchedule: LockSchedule,
    onDismiss: () -> Unit,
    onSelect: (LockSchedule) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Lock Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleOptionRow(
                    title = "Always Lock",
                    subtitle = "App locks immediately upon exiting.",
                    selected = currentSchedule == LockSchedule.ALWAYS,
                    onClick = { onSelect(LockSchedule.ALWAYS) }
                )
                ScheduleOptionRow(
                    title = "5-Minute Grace Period",
                    subtitle = "Remains unlocked for 5 minutes after verification.",
                    selected = currentSchedule == LockSchedule.GRACE_5_MINUTES,
                    onClick = { onSelect(LockSchedule.GRACE_5_MINUTES) }
                )
                ScheduleOptionRow(
                    title = "Night Schedule (22:00 - 07:00)",
                    subtitle = "Locks apps only during nighttime hours.",
                    selected = currentSchedule == LockSchedule.NIGHT_HOURS,
                    onClick = { onSelect(LockSchedule.NIGHT_HOURS) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ScheduleOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun LockGateDialog(
    app: AppItem,
    securitySettings: com.example.data.local.SecuritySettingsState,
    biometricStatus: BiometricStatus,
    onVerifyPin: (String) -> Boolean,
    onBiometricUnlock: () -> Unit,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    val isLockedOut = remember(securitySettings.lockoutUntilMillis) {
        System.currentTimeMillis() < securitySettings.lockoutUntilMillis
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("App Locked: ${app.appName}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This app is protected by Smart Phone Manager. Verify your security PIN or device biometrics to unlock and launch.",
                    style = MaterialTheme.typography.bodySmall
                )

                if (isLockedOut) {
                    val remainingSec = ((securitySettings.lockoutUntilMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(1)
                    Text(
                        "Too many failed attempts. Locked out for $remainingSec seconds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text("Security PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gate_pin_input"),
                        singleLine = true
                    )

                    if (errorText.isNotBlank()) {
                        Text(errorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    if (securitySettings.isBiometricEnabled) {
                        when (biometricStatus) {
                            is BiometricStatus.Available -> {
                                OutlinedButton(
                                    onClick = {
                                        BiometricHelper.authenticate(
                                            context = context,
                                            title = "Unlock ${app.appName}",
                                            subtitle = "Verify your identity to launch this protected app",
                                            onSuccess = {
                                                onBiometricUnlock()
                                                onUnlocked()
                                            },
                                            onError = { err ->
                                                errorText = err
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gate_biometric_button")
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unlock with Biometrics")
                                }
                            }
                            is BiometricStatus.Unavailable -> {
                                Text(
                                    text = "Biometric prompt unavailable: ${biometricStatus.reason}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isLockedOut) {
                Button(
                    onClick = {
                        val valid = onVerifyPin(pinInput)
                        if (valid) {
                            onUnlocked()
                        } else {
                            errorText = "Incorrect PIN. Try again."
                            pinInput = ""
                        }
                    },
                    modifier = Modifier.testTag("gate_unlock_button")
                ) {
                    Text("Unlock & Launch")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
