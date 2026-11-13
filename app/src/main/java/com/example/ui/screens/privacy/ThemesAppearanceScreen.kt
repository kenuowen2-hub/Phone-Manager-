package com.example.ui.screens.privacy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.theme.CoordinatedThemes
import com.example.ui.theme.SmartPhoneManagerTheme
import com.example.ui.theme.ThemeColorEngine
import com.example.ui.theme.ThemeConfig
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemesAppearanceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val securitySettings by viewModel.securitySettings.collectAsState()
    val savedConfig = securitySettings.themeConfig
    var draftConfig by remember(savedConfig) { mutableStateOf(savedConfig) }
    val hasPendingChanges = draftConfig != savedConfig

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val systemDark = isSystemInDarkTheme()
    val previewIsDark = when (draftConfig.mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Themes & Appearance", fontWeight = FontWeight.Bold)
                        if (hasPendingChanges) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Previewing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("themes_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val reset = viewModel.resetThemeConfig()
                            draftConfig = reset
                            scope.launch {
                                snackbarHostState.showSnackbar("Reset to default Ocean Guardian theme.")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("theme_reset_default_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Default")
                    }

                    Button(
                        onClick = {
                            viewModel.saveThemeConfig(draftConfig)
                            scope.launch {
                                snackbarHostState.showSnackbar("Theme saved and applied.")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("theme_apply_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Theme")
                    }
                }
            }
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

            // 1. Live Preview Before Applying
            item {
                LiveThemePreviewCard(
                    draftConfig = draftConfig,
                    previewIsDark = previewIsDark
                )
            }

            // 2. Display Mode (System default, Light, Dark)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_mode_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Appearance Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose how Smart Phone Manager adapts to your lighting preference.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                val icon = when (mode) {
                                    ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                }
                                FilterChip(
                                    selected = draftConfig.mode == mode,
                                    onClick = { draftConfig = draftConfig.copy(mode = mode) },
                                    label = { Text(mode.label) },
                                    leadingIcon = {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.testTag("theme_mode_${mode.storageKey}")
                                )
                            }
                        }
                    }
                }
            }

            // 3. Coordinated Color Themes
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coordinated_themes_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Coordinated Color Themes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select a curated palette or customize your own main and accent colors below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoordinatedThemes.presets.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { preset ->
                                        PresetThemeTile(
                                            preset = preset,
                                            isDark = previewIsDark,
                                            selected = draftConfig.presetId == preset.id,
                                            onClick = {
                                                draftConfig = draftConfig.copy(
                                                    presetId = preset.id,
                                                    customPrimaryArgb = (if (previewIsDark) preset.darkPrimary else preset.lightPrimary).toArgb(),
                                                    customAccentArgb = (if (previewIsDark) preset.darkAccent else preset.lightAccent).toArgb()
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("theme_preset_${preset.id}")
                                        )
                                    }
                                }
                            }

                            // Custom Palette Option Tile
                            val isCustom = draftConfig.isCustom
                            Card(
                                onClick = {
                                    draftConfig = draftConfig.copy(presetId = ThemeConfig.PRESET_CUSTOM)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("theme_preset_custom"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCustom) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                border = if (isCustom) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        DualColorSwatch(
                                            primary = Color(draftConfig.customPrimaryArgb),
                                            accent = Color(draftConfig.customAccentArgb)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Custom Colors", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Pick any main & accent color with automatic contrast tuning",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isCustom) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Custom Color Picker for Main & Accent Colors
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_color_picker_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Custom Main & Accent Picker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Drag across the color spectrum or enter hex codes. Text and icons automatically maintain accessible WCAG contrast.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        CustomColorPickerSection(
                            mainColor = Color(draftConfig.customPrimaryArgb),
                            accentColor = Color(draftConfig.customAccentArgb),
                            onMainColorChange = { picked ->
                                draftConfig = draftConfig.copy(
                                    presetId = ThemeConfig.PRESET_CUSTOM,
                                    customPrimaryArgb = picked.toArgb()
                                )
                            },
                            onAccentColorChange = { picked ->
                                draftConfig = draftConfig.copy(
                                    presetId = ThemeConfig.PRESET_CUSTOM,
                                    customAccentArgb = picked.toArgb()
                                )
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LiveThemePreviewCard(
    draftConfig: ThemeConfig,
    previewIsDark: Boolean
) {
    SmartPhoneManagerTheme(
        themeConfig = draftConfig,
        applySystemBars = false
    ) {
        val scheme = MaterialTheme.colorScheme
        val buttonContrast = ThemeColorEngine.contrastRatio(scheme.onPrimary, scheme.primary)
        val surfaceContrast = ThemeColorEngine.contrastRatio(scheme.primary, scheme.surface)
        val bodyContrast = ThemeColorEngine.contrastRatio(scheme.onSurface, scheme.surface)
        val minContrast = minOf(buttonContrast, surfaceContrast, bodyContrast)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("theme_live_preview_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.background),
            border = BorderStroke(1.dp, scheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Preview (${if (previewIsDark) "Dark" else "Light"})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onBackground
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = scheme.secondaryContainer
                    ) {
                        Text(
                            text = String.format(Locale.US, "Contrast %.1f:1 AA", minContrast),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Sample Surface Card inside preview
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Device Protection Card",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = "Buttons, cards, menus & dialogs preview",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = true, onCheckedChange = {})
                        }

                        LinearProgressIndicator(
                            progress = { 0.72f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = scheme.secondary,
                            trackColor = scheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Primary Action")
                            }
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Secondary", color = scheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetThemeTile(
    preset: ThemePreset,
    isDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = if (isDark) preset.darkPrimary else preset.lightPrimary
    val accent = if (isDark) preset.darkAccent else preset.lightAccent

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DualColorSwatch(primary = primary, accent = accent)
                if (selected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DualColorSwatch(
    primary: Color,
    accent: Color
) {
    Box(modifier = Modifier.width(38.dp).height(24.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(primary)
                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(x = 14.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(accent)
                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}
