package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.ai.AiAdvisorScreen
import com.example.ui.screens.applock.AppLockScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.filter.SearchFilterScreen
import com.example.ui.screens.privacy.PrivacySettingsScreen
import com.example.ui.screens.storage.StorageScreen

enum class ScreenTab {
    DASHBOARD,
    APP_LOCK,
    SEARCH_FILTER,
    STORAGE,
    AI_ADVISOR,
    PRIVACY
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(ScreenTab.DASHBOARD) }

    // Back handling: pop back to Dashboard before closing app
    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        currentTab = ScreenTab.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.DASHBOARD,
                    onClick = { currentTab = ScreenTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.APP_LOCK,
                    onClick = { currentTab = ScreenTab.APP_LOCK },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "App Lock") },
                    label = { Text("App Lock") },
                    modifier = Modifier.testTag("nav_tab_app_lock")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.SEARCH_FILTER,
                    onClick = { currentTab = ScreenTab.SEARCH_FILTER },
                    icon = { Icon(Icons.Default.FilterAlt, contentDescription = "Filter") },
                    label = { Text("Filter") },
                    modifier = Modifier.testTag("nav_tab_filter")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.STORAGE,
                    onClick = { currentTab = ScreenTab.STORAGE },
                    icon = { Icon(Icons.Default.Storage, contentDescription = "Storage") },
                    label = { Text("Storage") },
                    modifier = Modifier.testTag("nav_tab_storage")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.AI_ADVISOR,
                    onClick = { currentTab = ScreenTab.AI_ADVISOR },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Advisor") },
                    label = { Text("AI Advisor") },
                    modifier = Modifier.testTag("nav_tab_ai_advisor")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.PRIVACY,
                    onClick = { currentTab = ScreenTab.PRIVACY },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Privacy") },
                    label = { Text("Privacy") },
                    modifier = Modifier.testTag("nav_tab_privacy")
                )
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAdvisor = { currentTab = ScreenTab.AI_ADVISOR }
                )
                ScreenTab.APP_LOCK -> AppLockScreen(viewModel = viewModel)
                ScreenTab.SEARCH_FILTER -> SearchFilterScreen(viewModel = viewModel)
                ScreenTab.STORAGE -> StorageScreen(viewModel = viewModel)
                ScreenTab.AI_ADVISOR -> AiAdvisorScreen(viewModel = viewModel)
                ScreenTab.PRIVACY -> PrivacySettingsScreen(viewModel = viewModel)
            }
        }
    }
}
