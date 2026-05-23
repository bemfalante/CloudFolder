package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptography.EncryptionHelper
import com.example.data.database.FolderEntity
import com.example.data.database.MediaObjectEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = viewModel()
) {
    // Collecting states reactively
    val passphrase by viewModel.passphrase.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFolderId by viewModel.activeFolderId.collectAsState()
    val selectedMediaItem by viewModel.selectedMediaItem.collectAsState()

    val showCreateFolderDialog by viewModel.showCreateFolderDialog.collectAsState()
    val showCreateFileDialog by viewModel.showCreateFileDialog.collectAsState()
    val showOtaSimulationDialog by viewModel.showOtaSimulationDialog.collectAsState()

    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val otaLogs by viewModel.otaLogs.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val latencyMs by viewModel.latencyMs.collectAsState()

    val currentFolders by viewModel.currentFolders.collectAsState()
    val currentMediaObjects by viewModel.currentMediaObjects.collectAsState()
    val folderNavigationPath by viewModel.folderNavigationPath.collectAsState()
    val activeSecretKey by viewModel.activeSecretKey.collectAsState()

    // Sheet states for adding items
    var folderNameInput by remember { mutableStateOf("") }
    var fileNameInput by remember { mutableStateOf("") }
    var fileContentInput by remember { mutableStateOf("") }
    var fileTagInput by remember { mutableStateOf("Personal") }
    var fileMimeInput by remember { mutableStateOf("text/plain") }

    // OTA Simulation form states
    var otaChannelInput by remember { mutableStateOf("Admin Intel") }
    var otaPassphraseInput by remember { mutableStateOf("admin123") }

    // UI state toggles
    var showKeyHex by remember { mutableStateOf(false) }
    var showLogsConsole by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Security Shield",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                "CryptaSync Folders",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "E2EE Cloud & Over-The-Air Portal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Cloud Status Indicator
                    IconButton(
                        onClick = { viewModel.toggleCloudConnection() },
                        modifier = Modifier.testTag("cloud_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isCloudConnected) Icons.Filled.CloudQueue else Icons.Filled.CloudOff,
                            contentDescription = "Cloud Status",
                            tint = if (isCloudConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                    if (isCloudConnected) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "${latencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // OTA Update Simulation Button
                SmallFloatingActionButton(
                    onClick = { viewModel.showOtaSimulationDialog.value = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.testTag("ota_simulation_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RssFeed,
                        contentDescription = "OTA Update Stream"
                    )
                }

                // Add Folder / Item FAB group
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.showCreateFolderDialog.value = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.testTag("add_folder_fab")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = "Add Folder")
                            Spacer(Modifier.width(4.dp))
                            Text("Folder", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    FloatingActionButton(
                        onClick = { viewModel.showCreateFileDialog.value = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("add_file_fab")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add Media")
                            Spacer(Modifier.width(4.dp))
                            Text("Media", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // System Sync progress bar
            syncProgress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // E2EE Credentials Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Key,
                                        contentDescription = "Vault Key",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "End-to-End Key Envelope",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                AssistChip(
                                    onClick = { showKeyHex = !showKeyHex },
                                    label = { Text(if (showKeyHex) "Hide Hex" else "Show Derived Hex") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.DeveloperMode,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }

                            Text(
                                "Your local passphrase encrypts media headers before storing or uploading. Only devices with this exact password can read the vault contents.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Passphrase input field
                            OutlinedTextField(
                                value = passphrase,
                                onValueChange = { viewModel.updatePassphrase(it) },
                                label = { Text("Vault Passphrase") },
                                leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("passphrase_input"),
                                visualTransformation = PasswordVisualTransformation()
                            )

                            // Hex representation console
                            AnimatedVisibility(
                                visible = showKeyHex,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        "DERIVED AES-256 SYMMETRIC MASTER KEY:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF81C784),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "0x${EncryptionHelper.keyToHex(activeSecretKey)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF66BB6A),
                                        fontFamily = FontFamily.Monospace,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2
                                    )
                                }
                            }

                            // Tool Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.rebuildSeedData() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Re-Seed Files", fontSize = 12.sp)
                                }

                                TextButton(
                                    onClick = { viewModel.clearDatabase() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear Storage", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Global Search & Dynamic Navigation Breadcrumbs
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setQuery(it) },
                            placeholder = { Text("Search files & folders globally...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setQuery("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_field")
                        )

                        // Breadcrumb Trail Row
                        if (searchQuery.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.selectFolder(null) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FolderZip,
                                            contentDescription = "Root",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Root Vault",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    folderNavigationPath.forEach { folder ->
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
                                            contentDescription = "Chevron Separator",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            folder.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .clickable { viewModel.selectFolder(folder.id) }
                                                .padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Showing results matching: \"$searchQuery\"",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }

                // HIERARCHICAL FOLDERS GRID SECTION
                if (searchQuery.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Directories (${currentFolders.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (activeFolderId != null) {
                                TextButton(
                                    onClick = { 
                                        val parent = folderNavigationPath.getOrNull(folderNavigationPath.size - 2)?.id
                                        viewModel.selectFolder(parent)
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go up", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Go Up", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (currentFolders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.FolderOpen,
                                        contentDescription = "No Folders",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        "No sub-folders in this partition.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            // Using standard row/column to wrap grid elements nicely inside a single LazyColumn context
                            Column {
                                currentFolders.chunked(2).forEach { rowFolders ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowFolders.forEach { folder ->
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { viewModel.selectFolder(folder.id) }
                                                    .testTag("folder_${folder.name}"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(12.dp)
                                                        .fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Folder,
                                                            contentDescription = "Folder Icon",
                                                            tint = Color(0xFFFFC107), // Amber yellow folder coloring
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            folder.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.deleteFolder(folder.id) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Close,
                                                            contentDescription = "Remove Folder",
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        // Fill empty spot if odd row counts
                                        if (rowFolders.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // DECRYPTED MEDIA OBJECTS SECTION
                item {
                    Text(
                        "Secure Media Objects (${currentMediaObjects.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (currentMediaObjects.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Outlined.EnhancedEncryption,
                                        contentDescription = "No data",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "Envelope Store is Empty",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "Add secure items or simulate an OTA push.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(currentMediaObjects, key = { it.id }) { item ->
                        val isDecryptionSuccess = remember(item, passphrase) {
                            val decrypted = viewModel.decryptMediaItem(item)
                            !decrypted.startsWith("[Decryption Error")
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectMedia(item) }
                                .testTag("media_${item.name}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Content Icon according to state / decryption status
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isDecryptionSuccess) MaterialTheme.colorScheme.primaryContainer 
                                            else MaterialTheme.colorScheme.errorContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            !isDecryptionSuccess -> Icons.Filled.Lock
                                            item.isOtaReceived -> Icons.Filled.RssFeed
                                            item.mimeType.startsWith("image") -> Icons.Filled.Image
                                            item.mimeType.startsWith("audio") -> Icons.Filled.Audiotrack
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = "Media Type Indicator",
                                        tint = if (isDecryptionSuccess) MaterialTheme.colorScheme.primary 
                                               else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.isOtaReceived) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFECE0FD)
                                            ) {
                                                Text(
                                                    "OTA",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF673AB7),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "${item.fileSize} Bytes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(item.infoTag, fontSize = 10.sp) },
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // Synced Cloud State Icons
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (item.remoteSynced) Icons.Filled.CloudDone else Icons.Filled.CloudQueue,
                                        contentDescription = "Cloud Status",
                                        tint = if (item.remoteSynced) Color(0xFF4CAF50) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteMediaObject(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove Media",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            // DYNAMIC BLACK TERMINAL - CONSOLE FEED
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogsConsole = !showLogsConsole }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00FF00), RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "LIVE OTA TERMINAL MONITOR",
                            color = Color(0xFFE0E0E0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.clearDatabase(); viewModel.rebuildSeedData() },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Clear logs", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (showLogsConsole) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                            contentDescription = "Toggle logs",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showLogsConsole,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFF080808))
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        if (otaLogs.isEmpty()) {
                            Text(
                                "No transaction streams detected yet.",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                reverseLayout = false
                            ) {
                                items(otaLogs) { log ->
                                    Text(
                                        text = log,
                                        color = if (log.contains("Success", ignoreCase = true) || log.contains("Complete", ignoreCase = true)) {
                                            Color(0xFF00FF00)
                                        } else if (log.contains("Error", ignoreCase = true) || log.contains("Mismatch", ignoreCase = true)) {
                                            Color(0xFFFF3D00)
                                        } else if (log.contains("OTA", ignoreCase = true)) {
                                            Color(0xFFB388FF)
                                        } else {
                                            Color(0xFF81C784)
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 1: CREATE DIRECTORY
    // ==========================================
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showCreateFolderDialog.value = false; folderNameInput = "" },
            title = { Text("Create Directory Partition") },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    placeholder = { Text("e.g., Cryptographic Reports") },
                    modifier = Modifier.fillMaxWidth().testTag("folder_name_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            viewModel.createFolder(folderNameInput)
                        }
                        viewModel.showCreateFolderDialog.value = false
                        folderNameInput = ""
                    },
                    modifier = Modifier.testTag("confirm_create_folder")
                ) {
                    Text("Initialize Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showCreateFolderDialog.value = false; folderNameInput = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOG 2: INSERT/ENCRYPT NEW HARD FILE
    // ==========================================
    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showCreateFileDialog.value = false },
            title = { Text("Add E2EE Media Object") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = fileNameInput,
                        onValueChange = { fileNameInput = it },
                        label = { Text("Physical Asset Name") },
                        singleLine = true,
                        placeholder = { Text("credentials.txt") },
                        modifier = Modifier.fillMaxWidth().testTag("file_name_field")
                    )

                    OutlinedTextField(
                        value = fileContentInput,
                        onValueChange = { fileContentInput = it },
                        label = { Text("Raw Unencrypted Content") },
                        minLines = 3,
                        placeholder = { Text("Secret parameters, passphrases, notes...") },
                        modifier = Modifier.fillMaxWidth().testTag("file_content_field")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = fileTagInput,
                            onValueChange = { fileTagInput = it },
                            label = { Text("Security Tag") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = fileMimeInput,
                            onValueChange = { fileMimeInput = it },
                            label = { Text("MIME Block Type") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileNameInput.isNotBlank() && fileContentInput.isNotBlank()) {
                            viewModel.createMediaObject(
                                name = fileNameInput,
                                content = fileContentInput,
                                mimeType = fileMimeInput,
                                tag = fileTagInput
                            )
                        }
                        viewModel.showCreateFileDialog.value = false
                        fileNameInput = ""
                        fileContentInput = ""
                    },
                    modifier = Modifier.testTag("confirm_create_file")
                ) {
                    Text("Secure & Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showCreateFileDialog.value = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // ==========================================
    // DIALOG 3: OTA STREAM SIMULATION CONFIG
    // ==========================================
    if (showOtaSimulationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showOtaSimulationDialog.value = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.RssFeed, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Over-The-Air Broadcast")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Broadcast simulated E2EE files from the cloud over the air. Choose a feed and specify the encryption key representing the dynamic sender node.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("1. Select OTA Broadcast Stream Channel:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Admin Intel", "Strategic Briefings", "General Drop").forEach { channel ->
                            val isSelected = otaChannelInput == channel
                            FilterChip(
                                selected = isSelected,
                                onClick = { otaChannelInput = channel },
                                label = { Text(channel, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = otaPassphraseInput,
                        onValueChange = { otaPassphraseInput = it },
                        label = { Text("Sender Encryption Key Passphrase") },
                        singleLine = true,
                        placeholder = { Text("admin123") },
                        modifier = Modifier.fillMaxWidth().testTag("ota_passphrase_field"),
                        supportingText = {
                            Text(
                                "If the sender key matches your active Vault passphrase ('$passphrase'), decryption will occur dynamically! Otherwise files arrive locked in base64 format.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.triggerOtaSimulation(otaChannelInput, otaPassphraseInput)
                        viewModel.showOtaSimulationDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                    modifier = Modifier.testTag("confirm_ota_trigger")
                ) {
                    Text("Launch Over-the-Air Feed", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showOtaSimulationDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOG 4: ACTIVE SECURE ENVELOPE INSPECTOR
    // ==========================================
    selectedMediaItem?.let { item ->
        val decryptedText = remember(item, passphrase) {
            viewModel.decryptMediaItem(item)
        }
        val isDecryptionSuccess = !decryptedText.startsWith("[Decryption Error")

        AlertDialog(
            onDismissRequest = { viewModel.selectMedia(null) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDecryptionSuccess) Icons.Filled.SafetyCheck else Icons.Filled.LockPerson,
                        contentDescription = "Secure Block Status",
                        tint = if (isDecryptionSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Quick Information Attributes grid
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("MIME Payload", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(item.mimeType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Digital Size", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("${item.fileSize} Bytes", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Raw Base64 Envelope Card
                    Text("Raw Encrypted Cryptographic Envelope (Base64)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    item.encryptedContentBase64,
                                    color = Color(0xFFFFB74D),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }

                    // Decrypted Text Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dynamic Decrypted Contents", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (isDecryptionSuccess) "E2E OK" else "LOCKED",
                            color = if (isDecryptionSuccess) Color(0xFF4CAF50) else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                if (isDecryptionSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    decryptedText,
                                    color = if (isDecryptionSuccess) MaterialTheme.colorScheme.onSurface 
                                           else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = if (isDecryptionSuccess) FontFamily.SansSerif else FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.selectMedia(null) },
                    modifier = Modifier.testTag("close_inspector")
                ) {
                    Text("Close Panel")
                }
            }
        )
    }
}

// Simple extension helper to retrieve correct color backgrounds
@Composable
fun androidx.compose.material3.ColorScheme.surfaceColorAtElevation(
    elevation: androidx.compose.ui.unit.Dp
): Color {
    return this.surfaceVariant.copy(alpha = 0.5f)
}
