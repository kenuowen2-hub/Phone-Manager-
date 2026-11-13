package com.example.ui.screens.privacy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.security.BiometricStatus
import com.example.ui.MainViewModel
import com.example.ui.screens.applock.SetPinDialog
import com.example.ui.theme.CoordinatedThemes
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    viewModel: MainViewModel,
    onOpenThemes: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val securitySettings by viewModel.securitySettings.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val biometricStatus by viewModel.biometricStatus.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }
    var resumeTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDashboard()
                resumeTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val hasMicPermission = remember(resumeTick) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy & Settings", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Privacy Declaration Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("privacy_declaration_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Local-First Privacy Architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Smart Phone Manager keeps core device management strictly on your phone:\n\n" +
                                    "• App Lock Credentials: Your PIN is hashed locally using SHA-256 with a unique random cryptographic salt. It is never transmitted or stored in plain text.\n" +
                                    "• Search Word Filter: Word and phrase matching rules are processed 100% on your device. We do not collect, monitor, or send your search terms to remote servers.\n" +
                                    "• Files & Storage: Smart Phone Manager only inspects files you explicitly choose via Android's Storage Access Framework, and requires individual confirmation before deleting.\n" +
                                    "• Optional Cloud AI Features: Only when you explicitly tap Send in AI Advisor or record an Audio Note are your prompt/audio sent to Firebase AI Logic (gemini-4-carbon / gemini-3.5-transcribe).\n" +
                                    "• Anti-Trapping Policy: Emergency dialers, system settings, package installers, and this manager cannot be locked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Permissions Explained & Live Status
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("permissions_guide_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Permissions & Live Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Permissions are requested only when you enable the matching feature.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Usage Access Row
                        PermissionRow(
                            icon = Icons.Default.Timer,
                            title = "Usage Access (PACKAGE_USAGE_STATS)",
                            statusLabel = if (dashboardState.hasUsageStatsPermission) "Granted" else "Not Granted",
                            isGranted = dashboardState.hasUsageStatsPermission,
                            description = "Used on the Home dashboard for recent app usage and App Lock foreground status.",
                            onOpenSettings = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Biometric Hardware Row
                        PermissionRow(
                            icon = Icons.Default.Fingerprint,
                            title = "Biometric Authentication",
                            statusLabel = if (biometricStatus is BiometricStatus.Available) "Ready" else "Unavailable",
                            isGranted = biometricStatus is BiometricStatus.Available,
                            description = when (val status = biometricStatus) {
                                is BiometricStatus.Available -> "Device biometrics (fingerprint/face) are enrolled and available for App Lock."
                                is BiometricStatus.Unavailable -> status.reason
                            },
                            onOpenSettings = {
                                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Microphone Access Row
                        PermissionRow(
                            icon = Icons.Default.Mic,
                            title = "Microphone (RECORD_AUDIO)",
                            statusLabel = if (hasMicPermission) "Granted" else "Not Granted",
                            isGranted = hasMicPermission,
                            description = "Requested only when you tap Record Audio Note for voice transcription.",
                            onOpenSettings = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Storage SAF
                        PermissionRow(
                            icon = Icons.Default.Storage,
                            title = "Scoped Storage Picker",
                            statusLabel = "Zero Permission",
                            isGranted = true,
                            description = "Android's system file picker lets you choose individual files without granting broad disk permissions.",
                            onOpenSettings = null
                        )
                    }
                }
            }

            // Security PIN Settings Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_settings_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Security Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (securitySettings.hasPinConfigured) "A secure 4-6 digit PIN is active using salted SHA-256 hashing."
                            else "No security PIN configured yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showPinDialog = true },
                            modifier = Modifier.testTag("change_pin_settings_button")
                        ) {
                            Text(if (securitySettings.hasPinConfigured) "Change Security PIN" else "Set Security PIN")
                        }
                    }
                }
            }

            // Easy Disable & Uninstall Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("disable_uninstall_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disable Protection & Uninstall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("App Lock Active", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Turn off app protection anytime without restriction.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = securitySettings.isAppLockEnabled,
                                enabled = securitySettings.hasPinConfigured,
                                onCheckedChange = { viewModel.setAppLockMasterEnabled(it) },
                                modifier = Modifier.testTag("privacy_app_lock_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Smart Phone Manager does not request Device Administrator lockouts. You can revoke permissions or uninstall the app anytime in Android App Info.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("open_app_info_uninstall_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open App Info / Uninstall")
                        }
                    }
                }
            }

            // Themes & Appearance Entry Card
            item {
                val activeConfig = securitySettings.themeConfig
                val activeThemeName = if (activeConfig.isCustom) {
                    "Custom Colors"
                } else {
                    CoordinatedThemes.findById(activeConfig.presetId).name
                }
                val isDarkActive = when (activeConfig.mode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }

                Card(
                    onClick = onOpenThemes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_settings_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Themes & Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "$activeThemeName • ${activeConfig.mode.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(activeConfig.effectivePrimary(isDarkActive))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(activeConfig.effectiveAccent(isDarkActive))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open Themes & Appearance",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Customize light/dark mode, pick a coordinated palette, or choose custom main and accent colors with live preview.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenThemes,
                            modifier = Modifier.testTag("open_themes_screen_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Customize Themes & Colors")
                        }
                    }
                }
            }

            // About Smart Phone Manager Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("About Smart Phone Manager", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Version 1.0.0 • Target SDK 36\nBuilt with Kotlin, Jetpack Compose, Material 3, and Firebase AI Logic.\nCompliant with Google Play Safety & Transparency Policies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

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
}

@Composable
fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    statusLabel: String,
    isGranted: Boolean,
    description: String,
    onOpenSettings: (() -> Unit)?
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isGranted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isGranted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (onOpenSettings != null) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp)
        )
    }
}
