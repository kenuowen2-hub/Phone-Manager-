package com.example.ui.screens.storage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.storage.SharedFileItem
import com.example.ui.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dashboardState by viewModel.dashboardState.collectAsState()
    val storage = dashboardState.storage
    val sharedFiles by viewModel.sharedFiles.collectAsState()
    val cacheSize by viewModel.appCacheSizeBytes.collectAsState()

    var fileToDelete by remember { mutableStateOf<SharedFileItem?>(null) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDashboard()
                viewModel.refreshAppCache()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // SAF Document Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addSharedFiles(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Storage",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Storage & Files", fontWeight = FontWeight.Bold)
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

            // Overall Device Storage Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("storage_overview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Internal Device Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${storage.usedPercent}% Used",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { storage.usedPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Used Space", style = MaterialTheme.typography.labelSmall)
                                Text(storage.usedGbFormatted, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Free Space", style = MaterialTheme.typography.labelSmall)
                                Text(storage.freeGbFormatted, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Capacity", style = MaterialTheme.typography.labelSmall)
                                Text(storage.totalGbFormatted, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // App Temporary Files & Honest Cache Policy Card
            item {
                val cacheFormatted = if (cacheSize > 1024 * 1024) {
                    String.format(Locale.US, "%.2f MB", cacheSize / (1024.0 * 1024))
                } else {
                    String.format(Locale.US, "%.1f KB", cacheSize / 1024.0)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cache_cleaner_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manager Temporary Files", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Smart Phone Manager cache: $cacheFormatted\n\nAndroid security policy strictly sandboxes each app's private cache. No non-system app can delete or clear other apps' private caches without root or system OS privileges. You can safely clear this manager's temporary cache files below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showClearCacheConfirm = true },
                            modifier = Modifier.testTag("clear_cache_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clean Manager Cache")
                        }
                    }
                }
            }

            // Shared Files Review Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shared_files_section_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("User Selected Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.testTag("select_files_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pick Files")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Choose files using the Android Storage Access Framework to inspect and delete them with individual confirmation. Smart Phone Manager will never delete files automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (sharedFiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No files currently selected. Tap 'Pick Files' to review storage items.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                sharedFiles.forEach { item ->
                                    SharedFileRow(
                                        item = item,
                                        onDeleteRequest = { fileToDelete = item },
                                        onRemoveFromList = { viewModel.removeFileFromList(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // File Deletion Confirmation Dialog
    if (fileToDelete != null) {
        val file = fileToDelete!!
        val sizeFormatted = String.format(Locale.US, "%.2f MB", file.sizeBytes / (1024.0 * 1024))
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Confirm File Deletion") },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${file.name}\" ($sizeFormatted)?\n\nThis action cannot be undone. Smart Phone Manager requires your explicit confirmation for every file deletion.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSharedFile(file) { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "File permanently deleted.", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Could not delete file. Storage provider may not permit delete access.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_file_button")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clean Cache Confirm Dialog
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clean Temporary Files?") },
            text = {
                Text(
                    "This will delete temporary cached images, audio buffers, and logs generated by Smart Phone Manager. Other apps' files are unaffected.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAppCache()
                        showClearCacheConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_clean_cache_button")
                ) {
                    Text("Clean Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SharedFileRow(
    item: SharedFileItem,
    onDeleteRequest: () -> Unit,
    onRemoveFromList: () -> Unit
) {
    val sizeMb = String.format(Locale.US, "%.2f MB", item.sizeBytes / (1024.0 * 1024))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    "$sizeMb • ${item.mimeType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onRemoveFromList,
                modifier = Modifier.testTag("remove_file_from_list_${item.name}")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove from list", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(
                onClick = onDeleteRequest,
                modifier = Modifier.testTag("delete_file_${item.name}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
