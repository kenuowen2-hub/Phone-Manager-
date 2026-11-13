package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.screens.ai.AiAdvisorScreen
import com.example.ui.screens.applock.AppLockScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.filter.SearchFilterScreen
import com.example.ui.screens.privacy.PrivacySettingsScreen
import com.example.ui.screens.privacy.ThemesAppearanceScreen
import com.example.ui.screens.storage.StorageScreen

enum class ScreenTab(
    val label: String,
    val contentDesc: String,
    val icon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Home", "Dashboard", Icons.Default.Dashboard, "nav_tab_dashboard"),
    APP_LOCK("Apps", "App Lock", Icons.Default.Shield, "nav_tab_app_lock"),
    SEARCH_FILTER("Filter", "Filter", Icons.Default.FilterAlt, "nav_tab_filter"),
    STORAGE("Files", "Storage", Icons.Default.Storage, "nav_tab_storage"),
    AI_ADVISOR("AI", "AI Advisor", Icons.Default.AutoAwesome, "nav_tab_ai_advisor"),
    PRIVACY("Privacy", "Privacy", Icons.Default.Security, "nav_tab_privacy")
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(ScreenTab.DASHBOARD) }
    var showThemesScreen by rememberSaveable { mutableStateOf(false) }

    // Back handling: close sub-screen first, then pop back to Dashboard before closing app
    BackHandler(enabled = showThemesScreen || currentTab != ScreenTab.DASHBOARD) {
        if (showThemesScreen) {
            showThemesScreen = false
        } else {
            currentTab = ScreenTab.DASHBOARD
        }
    }

    if (showThemesScreen && currentTab == ScreenTab.PRIVACY) {
        ThemesAppearanceScreen(
            viewModel = viewModel,
            onBack = { showThemesScreen = false }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .testTag("nav_rail")
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    ScreenTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab,
                            onClick = {
                                showThemesScreen = false
                                currentTab = tab
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.contentDesc) },
                            label = {
                                Text(
                                    text = tab.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ScreenTabContent(
                        currentTab = currentTab,
                        viewModel = viewModel,
                        onNavigateToAdvisor = { currentTab = ScreenTab.AI_ADVISOR },
                        onOpenThemes = { showThemesScreen = true }
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                        ScreenTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = {
                                    showThemesScreen = false
                                    currentTab = tab
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.contentDesc) },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenTabContent(
                        currentTab = currentTab,
                        viewModel = viewModel,
                        onNavigateToAdvisor = { currentTab = ScreenTab.AI_ADVISOR },
                        onOpenThemes = { showThemesScreen = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenTabContent(
    currentTab: ScreenTab,
    viewModel: MainViewModel,
    onNavigateToAdvisor: () -> Unit,
    onOpenThemes: () -> Unit
) {
    when (currentTab) {
        ScreenTab.DASHBOARD -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToAdvisor = onNavigateToAdvisor
        )
        ScreenTab.APP_LOCK -> AppLockScreen(viewModel = viewModel)
        ScreenTab.SEARCH_FILTER -> SearchFilterScreen(viewModel = viewModel)
        ScreenTab.STORAGE -> StorageScreen(viewModel = viewModel)
        ScreenTab.AI_ADVISOR -> AiAdvisorScreen(viewModel = viewModel)
        ScreenTab.PRIVACY -> PrivacySettingsScreen(
            viewModel = viewModel,
            onOpenThemes = onOpenThemes
        )
    }
}
