package com.example.ui.screens.filter

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.BlockedRule
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterScreen(viewModel: MainViewModel) {
    val blockedRules by viewModel.blockedRules.collectAsState()
    val blockedLogs by viewModel.blockedLogs.collectAsState()
    val activeUrl by viewModel.currentActiveUrl.collectAsState()
    val filterResult by viewModel.lastFilterResult.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Safe Search & Browser, 1 = Rule Manager, 2 = Blocked Logs
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<BlockedRule?>(null) }
    var searchInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = "Search Filter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Word Filter", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = { showAddRuleDialog = true },
                    modifier = Modifier.testTag("add_rule_fab"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Filter Rule")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Safe Browser") },
                    icon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Rules (${blockedRules.size})") },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Audit Log (${blockedLogs.size})") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTabIndex) {
                0 -> SafeBrowserTab(
                    searchInput = searchInput,
                    onSearchInputChange = { searchInput = it },
                    onExecuteSearch = { viewModel.executeSearchQuery(it) },
                    activeUrl = activeUrl,
                    filterResult = filterResult,
                    onDismissAlert = { viewModel.dismissFilterResult() }
                )
                1 -> RuleManagerTab(
                    rules = blockedRules,
                    onEditRule = { ruleToEdit = it },
                    onDeleteRule = { viewModel.deleteBlockedRule(it) }
                )
                2 -> BlockedLogTab(
                    logs = blockedLogs,
                    onClearLogs = { viewModel.clearBlockedLogs() }
                )
            }
        }
    }

    // Add Rule Dialog
    if (showAddRuleDialog) {
        RuleEditorDialog(
            existingRule = null,
            onDismiss = { showAddRuleDialog = false },
            onSave = { pattern, isWholeWord, description ->
                viewModel.addBlockedRule(pattern, isWholeWord, description)
                showAddRuleDialog = false
            }
        )
    }

    // Edit Rule Dialog
    if (ruleToEdit != null) {
        RuleEditorDialog(
            existingRule = ruleToEdit,
            onDismiss = { ruleToEdit = null },
            onSave = { pattern, isWholeWord, description ->
                viewModel.updateBlockedRule(
                    ruleToEdit!!.copy(
                        pattern = pattern,
                        isWholeWord = isWholeWord,
                        description = description
                    )
                )
                ruleToEdit = null
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafeBrowserTab(
    searchInput: String,
    onSearchInputChange: (String) -> Unit,
    onExecuteSearch: (String) -> Unit,
    activeUrl: String,
    filterResult: com.example.filter.FilterCheckResult?,
    onDismissAlert: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = onSearchInputChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("inapp_search_input"),
                placeholder = { Text("Search safely or enter terms...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = { onSearchInputChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onExecuteSearch(searchInput) }),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onExecuteSearch(searchInput) },
                modifier = Modifier.testTag("check_and_search_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Search")
            }
        }

        // Privacy Guarantee Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                "Searches are evaluated 100% locally on-device before loading. Filtering applies to this in-app browser; Android policy isolates third-party browsers.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Blocked Alert Banner
        if (filterResult != null && filterResult.isBlocked) {
            val rule = filterResult.matchedRule
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("search_blocked_alert_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "Blocked",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Search Blocked by Rule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        IconButton(onClick = onDismissAlert) {
                            Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your search query matched blocked pattern: \"${rule?.pattern}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Matching Type: ${if (rule?.isWholeWord == true) "Whole Word Match" else "Phrase Substring Match"}\nReason: ${rule?.description?.ifBlank { "Flagged for safety" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // WebView Embedded Browser
        var lastLoadedUrl by remember { mutableStateOf("") }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        loadUrl(activeUrl)
                        lastLoadedUrl = activeUrl
                    }
                },
                update = { webView ->
                    if (activeUrl.isNotBlank() && activeUrl != lastLoadedUrl) {
                        lastLoadedUrl = activeUrl
                        webView.loadUrl(activeUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("inapp_browser_webview")
            )
        }
    }
}

@Composable
fun RuleManagerTab(
    rules: List<BlockedRule>,
    onEditRule: (BlockedRule) -> Unit,
    onDeleteRule: (BlockedRule) -> Unit
) {
    if (rules.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text("No blocked rules defined.", style = MaterialTheme.typography.titleMedium)
                Text("Tap the + button below to add words or phrases.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Active Filtering Rules (${rules.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    onEdit = { onEditRule(rule) },
                    onDelete = { onDeleteRule(rule) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun RuleCard(
    rule: BlockedRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rule_card_${rule.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rule.pattern,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (rule.isWholeWord) "Whole Word" else "Phrase",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (rule.description.isNotBlank()) {
                    Text(
                        rule.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (rule.matchCount > 0) {
                    Text(
                        "Blocked ${rule.matchCount} times",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_rule_${rule.id}")) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Rule", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_rule_${rule.id}")) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun BlockedLogTab(
    logs: List<com.example.data.local.SearchBlockedLog>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Blocked Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (logs.isNotEmpty()) {
                TextButton(onClick = onClearLogs, modifier = Modifier.testTag("clear_logs_button")) {
                    Text("Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No search violations logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val formatter = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Query: \"${log.query}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    formatter.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "Matched Rule: ${log.matchedRule}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RuleEditorDialog(
    existingRule: BlockedRule?,
    onDismiss: () -> Unit,
    onSave: (pattern: String, isWholeWord: Boolean, description: String) -> Unit
) {
    var pattern by remember { mutableStateOf(existingRule?.pattern ?: "") }
    var isWholeWord by remember { mutableStateOf(existingRule?.isWholeWord ?: true) }
    var description by remember { mutableStateOf(existingRule?.description ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRule == null) "Add Blocked Rule" else "Edit Blocked Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Word or Phrase to Block") },
                    placeholder = { Text("e.g. adult content, scam") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_pattern_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Match Whole Word", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (isWholeWord) "Matches exact word boundaries" else "Matches any occurrence within a phrase",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isWholeWord,
                        onCheckedChange = { isWholeWord = it },
                        modifier = Modifier.testTag("whole_word_switch")
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Reason / Label (Optional)") },
                    placeholder = { Text("e.g. Parental control keyword") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_description_input"),
                    singleLine = true
                )

                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pattern.trim().isBlank()) {
                        errorMessage = "Pattern cannot be empty."
                    } else {
                        onSave(pattern.trim(), isWholeWord, description.trim())
                    }
                },
                modifier = Modifier.testTag("save_rule_button")
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
