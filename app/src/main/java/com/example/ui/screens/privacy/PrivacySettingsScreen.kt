package com.example.ui.screens.privacy

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.screens.applock.SetPinDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val securitySettings by viewModel.securitySettings.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }

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
                            "Smart Phone Manager is designed with strict on-device data retention:\n\n" +
                                    "• App Lock Credentials: Your PIN is hashed locally using SHA-256 with a unique random cryptographic salt. It is never transmitted or stored in plain text.\n" +
                                    "• Search Word Filter: Word and phrase matching rules are processed 100% on your device. We do not collect, monitor, or send your search terms to remote servers.\n" +
                                    "• Files & Storage: Smart Phone Manager only views files you explicitly choose to share via Android's Storage Access Framework. We never automatically delete files.\n" +
                                    "• Anti-Trapping Policy: System emergency dialers, settings, and package installers are strictly un-lockable to prevent entrapment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Permissions Explained & Management
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("permissions_guide_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Permissions & Android System Access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Usage Access Row
                        PermissionRow(
                            icon = Icons.Default.Timer,
                            title = "Usage Access (PACKAGE_USAGE_STATS)",
                            description = "Required to calculate recent screen time and detect foreground apps for the App Lock feature.",
                            onOpenSettings = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
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
                            description = "Used exclusively when you tap the mic to record audio for transcription with gemini-3.5-transcribe.",
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
                            title = "Scoped Storage & Photo Picker",
                            description = "Zero-permission Android picker allows you to select files securely without broad disk access.",
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
                            if (securitySettings.hasPinConfigured) "A secure 4-6 digit PIN is currently active with SHA-256 salted hash."
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

            // Theme Preferences Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_settings_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("App Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = securitySettings.themeMode == "system",
                                onClick = { viewModel.setThemeMode("system") },
                                label = { Text("System Default") }
                            )
                            FilterChip(
                                selected = securitySettings.themeMode == "light",
                                onClick = { viewModel.setThemeMode("light") },
                                label = { Text("Light") }
                            )
                            FilterChip(
                                selected = securitySettings.themeMode == "dark",
                                onClick = { viewModel.setThemeMode("dark") },
                                label = { Text("Dark") }
                            )
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
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open Settings", tint = MaterialTheme.colorScheme.primary)
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
