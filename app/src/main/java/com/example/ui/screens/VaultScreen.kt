package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.VaultFileEntity
import com.example.data.model.VaultRepositoryEntity
import com.example.data.pref.CodePulsePrefs
import com.example.data.repository.CodePulseRepository
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VaultScreen(
    repository: CodePulseRepository,
    prefs: CodePulsePrefs
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe local repository caches
    val repositories by repository.getAllVaultRepositoriesFlow().collectAsState(initial = emptyList())

    // UI state
    var selectedRepoId by remember { mutableStateOf<Int?>(null) }
    var currentFolderPath by remember { mutableStateOf("") } // e.g. "" or "Trees" or "Trees/BST"
    var selectedFileId by remember { mutableStateOf<String?>(null) } // target file pathID

    // Dialogs
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var showRenameRepoDialog by remember { mutableStateOf<VaultRepositoryEntity?>(null) }
    var showPatConfigDialog by remember { mutableStateOf(false) }

    // PAT management
    var personalAccessToken by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        prefs.githubToken.collect { personalAccessToken = it }
    }

    // Determine currently selected Repo & File
    val activeRepo = repositories.find { it.id == selectedRepoId }
    val repoFiles by remember(selectedRepoId) {
        if (selectedRepoId != null) {
            repository.getVaultFilesFlow(selectedRepoId!!)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // Determine files in the current directory level
    val currentLevelFiles = remember(repoFiles, currentFolderPath) {
        repoFiles.filter { file ->
            file.parentPath == currentFolderPath
        }
    }

    // Determine open code file if present
    val activeCodeFile = repoFiles.find { it.pathId == selectedFileId }

    // Navigation back handlers
    val handleBackNavigation = {
        if (selectedFileId != null) {
            selectedFileId = null
        } else if (currentFolderPath.isNotEmpty()) {
            if (currentFolderPath.contains('/')) {
                currentFolderPath = currentFolderPath.substringBeforeLast('/')
            } else {
                currentFolderPath = ""
            }
        } else if (selectedRepoId != null) {
            selectedRepoId = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C)) // slate cyber theme dark canvas
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            VaultHeader(
                activeRepoName = activeRepo?.displayName ?: "Knowledge Vault",
                activeRepoPath = activeRepo?.let { "${it.owner}/${it.repo}" },
                currentFolderPath = currentFolderPath,
                activeFileName = activeCodeFile?.name,
                onBackClicked = { handleBackNavigation() },
                showBackButton = selectedRepoId != null,
                onConfigPatClicked = { showPatConfigDialog = true }
            )

            HorizontalDivider(color = Color(0xFF30363D))

            // Body
            Box(modifier = Modifier.weight(1f)) {
                if (selectedRepoId == null) {
                    // 1. Dashboard connected list
                    VaultDashboard(
                        repositories = repositories,
                        onRepoClick = { repoId ->
                            selectedRepoId = repoId
                            currentFolderPath = ""
                            selectedFileId = null
                        },
                        onAddClicked = { showAddRepoDialog = true },
                        onRenameClicked = { showRenameRepoDialog = it },
                        onToggleFavorite = { repo ->
                            coroutineScope.launch {
                                repository.updateVaultRepository(repo.copy(isFavorite = !repo.isFavorite))
                            }
                        },
                        onSyncClicked = { repo ->
                            coroutineScope.launch {
                                try {
                                    repository.syncVaultRepository(
                                        repoId = repo.id,
                                        owner = repo.owner,
                                        repo = repo.repo,
                                        token = personalAccessToken.ifBlank { null }
                                    )
                                    Toast.makeText(context, "Syndication complete: ${repo.displayName}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Sync Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onDeleteClicked = { repo ->
                            coroutineScope.launch {
                                repository.deleteVaultRepository(repo)
                                Toast.makeText(context, "Deleted vault repository", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMoveUp = { repo ->
                            val index = repositories.indexOf(repo)
                            if (index > 0) {
                                coroutineScope.launch {
                                    val adjacent = repositories[index - 1]
                                    val temp = repo.orderIndex
                                    repository.updateVaultRepositories(
                                        listOf(
                                            repo.copy(orderIndex = adjacent.orderIndex),
                                            adjacent.copy(orderIndex = temp)
                                        )
                                    )
                                }
                            }
                        },
                        onMoveDown = { repo ->
                            val index = repositories.indexOf(repo)
                            if (index >= 0 && index < repositories.size - 1) {
                                coroutineScope.launch {
                                    val adjacent = repositories[index + 1]
                                    val temp = repo.orderIndex
                                    repository.updateVaultRepositories(
                                        listOf(
                                            repo.copy(orderIndex = adjacent.orderIndex),
                                            adjacent.copy(orderIndex = temp)
                                        )
                                    )
                                }
                            }
                        }
                    )
                } else if (activeCodeFile != null) {
                    // 2. Multi-format Document and Code Previewer
                    VaultFilePreviewer(
                        file = activeCodeFile,
                        token = personalAccessToken.ifBlank { null },
                        onCacheClicked = {
                            coroutineScope.launch {
                                repository.syncVaultFileContent(activeCodeFile, personalAccessToken.ifBlank { null })
                                Toast.makeText(context, "Caching complete", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    // 3. Explorer Dashboard of active Repo
                    VaultExplorer(
                        repo = activeRepo!!,
                        currentPath = currentFolderPath,
                        repoFiles = repoFiles,
                        onFolderClick = { folder ->
                            currentFolderPath = folder
                        },
                        onFileClick = { fileId ->
                            selectedFileId = fileId
                        },
                        onBreadcrumbClick = { path ->
                            currentFolderPath = path
                            selectedFileId = null
                        }
                    )
                }
            }
        }

        // Floating Action Button on main vault page
        if (selectedRepoId == null) {
            FloatingActionButton(
                onClick = { showAddRepoDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_vault_repo_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Repository")
            }
        }

        // Dialogs
        if (showAddRepoDialog) {
            AddRepoDialog(
                onDismiss = { showAddRepoDialog = false },
                onAddConfigured = { owner, repo, displayName ->
                    coroutineScope.launch {
                        try {
                            val nextIndex = (repositories.maxOfOrNull { it.orderIndex } ?: -1) + 1
                            val newRepo = VaultRepositoryEntity(
                                owner = owner,
                                repo = repo,
                                displayName = displayName,
                                syncStatus = "IDLE",
                                lastSyncTime = 0L,
                                orderIndex = nextIndex
                            )
                            val longId = repository.insertVaultRepository(newRepo)
                            showAddRepoDialog = false

                            // auto trigger background syndication
                            Toast.makeText(context, "Adding vault repo... Sync starting.", Toast.LENGTH_SHORT).show()
                            repository.syncVaultRepository(
                                repoId = longId.toInt(),
                                owner = owner,
                                repo = repo,
                                token = personalAccessToken.ifBlank { null }
                            )
                            Toast.makeText(context, "Succeeded! Repository ready offline", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        if (showRenameRepoDialog != null) {
            RenameRepoDialog(
                repo = showRenameRepoDialog!!,
                onDismiss = { showRenameRepoDialog = null },
                onRename = { newName ->
                    coroutineScope.launch {
                        repository.updateVaultRepository(showRenameRepoDialog!!.copy(displayName = newName))
                        showRenameRepoDialog = null
                    }
                }
            )
        }

        if (showPatConfigDialog) {
            PatConfigDialog(
                initialToken = personalAccessToken,
                onDismiss = { showPatConfigDialog = false },
                onSave = { token ->
                    coroutineScope.launch {
                        prefs.setGithubToken(token)
                        showPatConfigDialog = false
                        Toast.makeText(context, "Credentials stored locally", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// ---------------- COMPONENTS ----------------

@Composable
fun VaultHeader(
    activeRepoName: String,
    activeRepoPath: String?,
    currentFolderPath: String,
    activeFileName: String?,
    onBackClicked: () -> Unit,
    showBackButton: Boolean,
    onConfigPatClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (activeFileName != null) activeFileName else activeRepoName,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (activeFileName != null) {
                    "Workspace / ${activeRepoPath ?: ""} / $currentFolderPath"
                } else if (activeRepoPath != null) {
                    if (currentFolderPath.isEmpty()) activeRepoPath else "$activeRepoPath / $currentFolderPath"
                } else {
                    "Multi-repository developer vault"
                },
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onConfigPatClicked) {
            Icon(
                imageVector = Icons.Filled.VpnKey,
                contentDescription = "Configure PAT Key",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun VaultDashboard(
    repositories: List<VaultRepositoryEntity>,
    onRepoClick: (Int) -> Unit,
    onAddClicked: () -> Unit,
    onRenameClicked: (VaultRepositoryEntity) -> Unit,
    onToggleFavorite: (VaultRepositoryEntity) -> Unit,
    onSyncClicked: (VaultRepositoryEntity) -> Unit,
    onDeleteClicked: (VaultRepositoryEntity) -> Unit,
    onMoveUp: (VaultRepositoryEntity) -> Unit,
    onMoveDown: (VaultRepositoryEntity) -> Unit
) {
    if (repositories.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.FolderZip,
                contentDescription = "Files list empty",
                tint = Color.LightGray.copy(alpha = 0.4f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Empty Vault Workspace",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add directories, system-design notes, or LeetCode backups to build your localized developer hub.",
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddClicked,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Connect GitHub Repo", color = Color.White)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${repositories.size} connected libraries",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )

                    Row {
                        TextButton(onClick = onAddClicked) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Repo", fontSize = 13.sp)
                        }
                    }
                }
            }

            items(repositories, key = { it.id }) { repo ->
                VaultRepoCard(
                    repo = repo,
                    onClick = { onRepoClick(repo.id) },
                    onRename = { onRenameClicked(repo) },
                    onToggleFavorite = { onToggleFavorite(repo) },
                    onSync = { onSyncClicked(repo) },
                    onDelete = { onDeleteClicked(repo) },
                    onMoveUp = { onMoveUp(repo) },
                    onMoveDown = { onMoveDown(repo) },
                    hasAdjacentUp = repositories.firstOrNull()?.id != repo.id,
                    hasAdjacentDown = repositories.lastOrNull()?.id != repo.id
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun VaultRepoCard(
    repo: VaultRepositoryEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    hasAdjacentUp: Boolean,
    hasAdjacentDown: Boolean
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Header + Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = repo.displayName,
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (repo.isFavorite) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favorite star",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "${repo.owner}/${repo.repo}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (repo.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Toggle Favorite",
                            tint = if (repo.isFavorite) Color(0xFFFFD700) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Menu Ops",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Color(0xFF161B22)).border(1.dp, Color(0xFF30363D))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Synchronize", color = Color.White) },
                                onClick = {
                                    menuExpanded = false
                                    onSync()
                                },
                                leadingIcon = { Icon(Icons.Filled.Sync, null, tint = Color.LightGray) }
                            )
                            DropdownMenuItem(
                                text = { Text("Local Rename", color = Color.White) },
                                onClick = {
                                    menuExpanded = false
                                    onRename()
                                },
                                leadingIcon = { Icon(Icons.Filled.Edit, null, tint = Color.LightGray) }
                            )
                            HorizontalDivider(color = Color(0xFF30363D))
                            if (hasAdjacentUp) {
                                DropdownMenuItem(
                                    text = { Text("Move Up", color = Color.White) },
                                    onClick = {
                                        menuExpanded = false
                                        onMoveUp()
                                    },
                                    leadingIcon = { Icon(Icons.Filled.ArrowUpward, null, tint = Color.LightGray) }
                                )
                            }
                            if (hasAdjacentDown) {
                                DropdownMenuItem(
                                    text = { Text("Move Down", color = Color.White) },
                                    onClick = {
                                        menuExpanded = false
                                        onMoveDown()
                                    },
                                    leadingIcon = { Icon(Icons.Filled.ArrowDownward, null, tint = Color.LightGray) }
                                )
                            }
                            HorizontalDivider(color = Color(0xFF30363D))
                            DropdownMenuItem(
                                text = { Text("Delete Local Cache", color = Color.Red) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }

            // Row 2: Description
            if (repo.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = repo.description,
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 3: Tag Chips
            val tags = remember(repo.topicsJson) { parseTags(repo.topicsJson) }
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tags) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF21262D))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = tag, color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF21262D))
            Spacer(modifier = Modifier.height(12.dp))

            // Row 4: Metrics details in columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "FILES & FOLDERS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "📁 ${repo.folderCount} dirs  •  📄 ${repo.totalFiles} files",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "DISK ESTIMATE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatSize(repo.sizeEstimate),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last Synced Date / Sync Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (repo.syncStatus) {
                        "SYNCING" -> Color(0xFF58A6FF)
                        "SUCCESS" -> Color(0xFF3FB950)
                        "FAILED" -> Color(0xFFF85149)
                        else -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (repo.syncStatus) {
                            "SYNCING" -> "Synchronizing..."
                            "SUCCESS" -> "Offline Ready"
                            "FAILED" -> "Credentials/Access Failed"
                            else -> "Idle & Unsynced"
                        },
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = if (repo.lastSyncTime > 0) {
                        "Synced: " + formatEpoch(repo.lastSyncTime)
                    } else "Never synced",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            // Language Distribution mini gauge
            val langMap = remember(repo.languagesJson) { parseLanguages(repo.languagesJson) }
            if (langMap.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LanguageProgressBar(langs = langMap, mostUsed = repo.mostUsedLanguage)
            }
        }
    }
}

@Composable
fun LanguageProgressBar(
    langs: Map<String, Long>,
    mostUsed: String
) {
    val totalSum = langs.values.sum().toFloat()
    if (totalSum <= 0f) return

    val colors = listOf(
        Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFF3B82F6), Color(0xFFEF4444), Color(0xFFEC4899)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Most used language: $mostUsed",
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = "${langs.size} types index",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Progress row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF21262D))
        ) {
            var colorIdx = 0
            langs.entries.sortedByDescending { it.value }.forEach { entry ->
                val weight = entry.value / totalSum
                if (weight > 0.01) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weight)
                            .background(colors[colorIdx % colors.size])
                    )
                    colorIdx++
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Language lists row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            var colorIdx = 0
            langs.entries.sortedByDescending { it.value }.take(4).forEach { entry ->
                val pct = (entry.value / totalSum * 100).toInt()
                if (pct > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors[colorIdx % colors.size])
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${entry.key} ($pct%)",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    colorIdx++
                }
            }
        }
    }
}

// ---------------- EXPLORER DASHBOARD LAYER ----------
@Composable
fun VaultExplorer(
    repo: VaultRepositoryEntity,
    currentPath: String,
    repoFiles: List<VaultFileEntity>,
    onFolderClick: (String) -> Unit,
    onFileClick: (String) -> Unit,
    onBreadcrumbClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedExtension by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf("NAME") } // "NAME", "SIZE", "DATE", "TYPE"

    // Process list dynamically in UI
    val availableExtensions = remember(repoFiles) {
        repoFiles
            .filter { it.type == "file" && it.extension.isNotBlank() }
            .map { it.extension.lowercase() }
            .distinct()
            .sorted()
    }

    val filteredAndSortedFiles = remember(repoFiles, currentPath, searchQuery, selectedExtension, sortOption) {
        // 1. Initial filter based on search query vs folder depth browsing
        var list = if (searchQuery.isNotBlank()) {
            repoFiles.filter { file ->
                file.type == "file" && (
                    file.fileName.contains(searchQuery, ignoreCase = true) ||
                    file.path.contains(searchQuery, ignoreCase = true)
                )
            }
        } else {
            repoFiles.filter { file ->
                file.parentPath == currentPath
            }
        }

        // 2. Filter by selected extension if active
        if (selectedExtension != null) {
            list = list.filter { file ->
                file.type == "file" && file.extension.equals(selectedExtension, ignoreCase = true)
            }
        }

        // 3. Sort standard items
        when (sortOption) {
            "NAME" -> list.sortedWith(compareBy<VaultFileEntity> { it.type != "dir" }.thenBy { it.fileName.lowercase() })
            "SIZE" -> list.sortedWith(compareBy<VaultFileEntity> { it.type != "dir" }.thenByDescending { it.size })
            "DATE" -> list.sortedWith(compareBy<VaultFileEntity> { it.type != "dir" }.thenByDescending { it.lastModified })
            "TYPE" -> list.sortedWith(compareBy<VaultFileEntity> { it.type != "dir" }.thenBy { it.extension })
            else -> list
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Vault repo quick dashboard metadata
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Repository Workspace Summary",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (repo.isPrivate) "Private" else "Public",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sync and browse PDF, Word DOCX, Markdown notebooks, and source codes fully cached on SQLite locally for offline studies.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("LAST COMMIT", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text(repo.lastCommitDate, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("TOTAL ELEMENTS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text("${repo.totalFiles} files, ${repo.folderCount} folders", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("DEFAULT INDEX", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text(repo.defaultBranch, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search & Filter Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F141C))
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search files across entire repo...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF30363D)
                ),
                leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("document_library_search_input")
            )

            // Dynamic Extension Filter Row
            if (availableExtensions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedExtension == null) MaterialTheme.colorScheme.primary else Color(0xFF21262D))
                                .clickable { selectedExtension = null }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "All formats",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(availableExtensions) { ext ->
                        val isSelected = selectedExtension == ext
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF21262D))
                                .clickable { selectedExtension = ext }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = ".${ext.uppercase()}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Sorting Panel Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort by: ", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))

                val sortOptions = listOf("NAME" to "Name", "SIZE" to "Size", "DATE" to "Date", "TYPE" to "Type")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sortOptions) { (opt, label) ->
                        val isSelected = sortOption == opt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFF21262D))
                                .clickable { sortOption = opt }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Breadcrumbs Row (Only if we aren't searching, to preserve folder layout hierarchy)
        if (searchQuery.isBlank()) {
            VaultBreadcrumbs(currentPath = currentPath, onBreadcrumbClick = onBreadcrumbClick)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Global Search Results for \"$searchQuery\"",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(color = Color(0xFF30363D))

        // Files lists
        if (filteredAndSortedFiles.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Inventory,
                    contentDescription = "Empty",
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("No files matched filters", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Try revising your search string, checking specific format filters or sync the repository metadata.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                items(filteredAndSortedFiles, key = { it.id }) { file ->
                    VaultFileRow(
                        file = file,
                        onClick = {
                            if (file.type == "dir") {
                                onFolderClick(file.path)
                            } else {
                                onFileClick(file.id)
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFF21262D))
                }
            }
        }
    }
}

@Composable
fun VaultBreadcrumbs(
    currentPath: String,
    onBreadcrumbClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F141C))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable { onBreadcrumbClick("") },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Home, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ROOT", color = if (currentPath.isEmpty()) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (currentPath.isNotEmpty()) {
            val pieces = currentPath.split('/')
            var cumulative = ""
            pieces.forEach { piece ->
                cumulative = if (cumulative.isEmpty()) piece else "$cumulative/$piece"
                val thisPath = cumulative

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp).padding(horizontal = 2.dp)
                )

                Text(
                    text = piece,
                    color = if (cumulative == currentPath) Color.White else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBreadcrumbClick(thisPath) }
                )
            }
        }
    }
}

@Composable
fun getFileIconAndColor(extension: String, isFolder: Boolean): Pair<ImageVector, Color> {
    if (isFolder) {
        return Icons.Filled.Folder to Color(0xFFE2B93B)
    }
    return when (extension.lowercase()) {
        "pdf" -> Icons.Filled.Book to Color(0xFFF43F5E)
        "docx" -> Icons.Filled.Description to Color(0xFF2563EB)
        "md" -> Icons.Filled.MenuBook to Color(0xFF0EA5E9)
        "txt" -> Icons.Filled.Feed to Color(0xFF64748B)
        "kt", "java", "cpp", "py", "js", "ts", "sql", "json", "xml", "yaml" -> Icons.Filled.Code to Color(0xFF10B981)
        else -> Icons.Filled.Description to Color(0xFF8B949E)
    }
}

@Composable
fun VaultFileRow(
    file: VaultFileEntity,
    onClick: () -> Unit
) {
    val (icon, tintColor) = getFileIconAndColor(file.extension, file.type == "dir")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.fileName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (file.type == "file") {
                Text(
                    text = "Size: ${formatSize(file.size)}  •  ${if (file.codeContent != null) "Downloaded" else "Cloud Stream"}",
                    color = if (file.codeContent != null) Color(0xFF3FB950) else Color(0xFFE2B93B),
                    fontSize = 11.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun PdfPreviewer(base64Data: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmaps by remember(base64Data) { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    var errorState by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(base64Data) {
        try {
            val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val tempFile = java.io.File(context.cacheDir, "pdf_preview_temp.pdf")
            tempFile.writeBytes(bytes)

            val pfd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            val loadedBitmaps = mutableListOf<android.graphics.Bitmap>()

            // Render up to first 10 pages for document index studies
            val maxPages = minOf(pageCount, 10)
            for (i in 0 until maxPages) {
                val page = renderer.openPage(i)
                val widthVal = page.width * 2
                val heightVal = page.height * 2
                val bitmap = android.graphics.Bitmap.createBitmap(widthVal, heightVal, android.graphics.Bitmap.Config.ARGB_8888)

                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                loadedBitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
            pfd.close()
            bitmaps = loadedBitmaps
        } catch (e: Exception) {
            e.printStackTrace()
            errorState = e.message ?: "Failed to render PDF"
        }
    }

    if (errorState != null) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Error, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("PDF Render Issue", color = Color.White, fontWeight = FontWeight.Bold)
            Text(errorState ?: "", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 12.sp)
        }
    } else if (bitmaps.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F141C))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(bitmaps) { bitmap ->
                Card(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF Page",
                        modifier = Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                    )
                }
            }
        }
    }
}

fun extractDocxText(base64Data: String): String {
    return try {
        val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        val zipStream = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes))
        var entry = zipStream.nextEntry
        var documentXml: String? = null
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                documentXml = zipStream.bufferedReader().use { it.readText() }
                break
            }
            entry = zipStream.nextEntry
        }
        zipStream.close()

        if (documentXml != null) {
            val ParagraphRegex = Regex("<w:p[ >].*?</w:p>")
            val TextRegex = Regex("<w:t[ >](.*?)</w:t>")

            val paragraphs = ParagraphRegex.findAll(documentXml).map { pResult ->
                val runText = TextRegex.findAll(pResult.value).map { tResult ->
                    val value = tResult.groupValues[1]
                    value.replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                }.joinToString("")
                runText
            }.filter { it.isNotBlank() }.toList()

            if (paragraphs.isNotEmpty()) {
                paragraphs.joinToString("\n\n")
            } else {
                documentXml.replace(Regex("<.*?>"), " ").trim()
            }
        } else {
            "No word/document.xml entry found inside Word Document ZIP."
        }
    } catch (e: Exception) {
        "Failed to convert DOCX text: ${e.message}"
    }
}

fun formatBoldItalicText(text: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end != -1) {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                builder.append(text.substring(i + 2, end))
                builder.pop()
                i = end + 2
                continue
            }
        } else if (text.startsWith("*", i)) {
            val end = text.indexOf("*", i + 1)
            if (end != -1) {
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(text.substring(i + 1, end))
                builder.pop()
                i = end + 1
                continue
            }
        }
        builder.append(text[i])
        i++
    }
    return builder.toAnnotatedString()
}

@Composable
fun MarkdownRenderer(text: String, modifier: Modifier = Modifier) {
    val lines = text.split('\n')
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.substring(2),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.substring(3),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.substring(4),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("* ") || line.startsWith("- ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text("• ", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Text(
                            text = if (line.startsWith("* ")) line.substring(2) else line.substring(2),
                            color = Color(0xFFE6EDF0),
                            fontSize = 14.sp
                        )
                    }
                }
                line.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF21262D))
                            .border(BorderStroke(1.dp, Color(0xFF30363D)), RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = line.substring(2),
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            style = androidx.compose.ui.text.TextStyle(fontStyle = FontStyle.Italic)
                        )
                    }
                }
                else -> {
                    if (line.isNotBlank()) {
                        Text(
                            text = formatBoldItalicText(line),
                            color = Color(0xFFE6EDF0),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

fun highlightCode(code: String, extension: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(code)

    val commentRegex = Regex("//.*")
    commentRegex.findAll(code).forEach { result ->
        builder.addStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF8B949E), fontStyle = FontStyle.Italic), result.range.first, result.range.last + 1)
    }

    val stringRegex = Regex("\".*?\"")
    stringRegex.findAll(code).forEach { result ->
        builder.addStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFA5D6FF)), result.range.first, result.range.last + 1)
    }

    val keywords = setOf(
        "class", "interface", "struct", "fun", "function", "def", "import", "package", "return", "if", "else",
        "while", "for", "in", "public", "private", "protected", "void", "val", "var", "const", "let", "null", "true", "false"
    )
    val wordRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
    wordRegex.findAll(code).forEach { result ->
        if (result.value in keywords) {
            builder.addStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFFF7B72), fontWeight = FontWeight.Bold), result.range.first, result.range.last + 1)
        }
    }

    return builder.toAnnotatedString()
}

@Composable
fun VaultFilePreviewer(
    file: VaultFileEntity,
    token: String?,
    onCacheClicked: () -> Unit
) {
    val context = LocalContext.current
    val codeContent = file.codeContent
    val extension = file.extension.lowercase()

    var readerTextSize by remember { mutableStateOf(14.sp) }
    var selectSerifFont by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("OFFLINE STORAGE STATUS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (codeContent != null) "100% Offline Complete" else "Cloud Stream Required",
                        color = if (codeContent != null) Color(0xFF3FB950) else Color(0xFFE2B93B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (codeContent != null && (extension == "txt" || extension == "md" || extension == "docx")) {
                        IconButton(onClick = {
                            readerTextSize = when (readerTextSize) {
                                12.sp -> 14.sp
                                14.sp -> 18.sp
                                18.sp -> 22.sp
                                else -> 12.sp
                            }
                        }) {
                            Icon(Icons.Filled.TextFormat, "Text Size", tint = Color.LightGray)
                        }

                        IconButton(onClick = { selectSerifFont = !selectSerifFont }) {
                            Icon(
                                imageVector = if (selectSerifFont) Icons.Filled.FontDownload else Icons.Outlined.FontDownload,
                                contentDescription = "Font Type",
                                tint = Color.LightGray
                            )
                        }
                    }

                    if (codeContent != null && extension != "pdf") {
                        IconButton(
                            onClick = {
                                val textToCopy = if (extension == "docx") extractDocxText(codeContent) else codeContent
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Vault File Content", textToCopy)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy", tint = Color.LightGray)
                        }
                    }

                    Button(
                        onClick = onCacheClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (codeContent != null) Color(0xFF21262D) else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (codeContent != null) "Re-Sync" else "Download", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF30363D))

        if (codeContent == null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Uncached File",
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text("Content not cached offline", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "To read documents and code lists fully offline, you can retrieve the file text directly using your token.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onCacheClicked) {
                    Text("Download Content Cache")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                when (extension) {
                    "pdf" -> {
                        PdfPreviewer(base64Data = codeContent, modifier = Modifier.fillMaxSize())
                    }
                    "docx" -> {
                        val parsedText = remember(codeContent) { extractDocxText(codeContent) }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = parsedText,
                                fontSize = readerTextSize,
                                fontFamily = if (selectSerifFont) FontFamily.Serif else FontFamily.SansSerif,
                                color = Color(0xFFE6EDF0),
                                lineHeight = (readerTextSize.value * 1.5).sp
                            )
                        }
                    }
                    "md" -> {
                        MarkdownRenderer(text = codeContent, modifier = Modifier.fillMaxSize())
                    }
                    "txt" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = codeContent,
                                fontSize = readerTextSize,
                                fontFamily = if (selectSerifFont) FontFamily.Serif else FontFamily.SansSerif,
                                color = Color(0xFFE6EDF0),
                                lineHeight = (readerTextSize.value * 1.5).sp
                            )
                        }
                    }
                    else -> {
                        val highlighted = remember(codeContent) { highlightCode(codeContent, extension) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .background(Color(0xFF0F141C))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = highlighted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- DIALOGS ----------------

@Composable
fun AddRepoDialog(
    onDismiss: () -> Unit,
    onAddConfigured: (String, String, String) -> Unit
) {
    var owner by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Connect Repository",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Syndicate directories, notes, and algorithms directly onto your local SQLite vault offline.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it.trim() },
                    label = { Text("Repository Owner (Login)") },
                    placeholder = { Text("e.g. octocat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_vault_owner_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it.trim() },
                    label = { Text("Repository Name") },
                    placeholder = { Text("e.g. dsa-notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_vault_repo_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Local Alias (Display Name)") },
                    placeholder = { Text("e.g. Data Structures Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_vault_display_input")
                )

                if (errorMsg.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (owner.isBlank() || repo.isBlank()) {
                                errorMsg = "Owner and Repository details are mandatory"
                            } else {
                                val resolvedAlias = displayName.ifBlank { repo }
                                onAddConfigured(owner, repo, resolvedAlias)
                            }
                        }
                    ) {
                        Text(" Syndicate ", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RenameRepoDialog(
    repo: VaultRepositoryEntity,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var text by remember { mutableStateOf(repo.displayName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Locally Rename Alias", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_vault_display_input")
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { if (text.isNotBlank()) onRename(text) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun PatConfigDialog(
    initialToken: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialToken) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Set GitHub Access Token", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Necessary to authenticate searches, rate limiters, or private repositories in your Knowledge Vault securely.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.trim() },
                    placeholder = { Text("Paste Personal Access Token (PAT)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("vault_pat_input")
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { onSave(text) }) { Text("Save Key") }
                }
            }
        }
    }
}

// ---------------- LOCAL HELPER PARSERS ----------------

fun parseTags(json: String): List<String> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        json.replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    } catch (e: Exception) {
        emptyList()
    }
}

fun parseLanguages(json: String): Map<String, Long> {
    if (json.isBlank() || json == "{}") return emptyMap()
    val map = mutableMapOf<String, Long>()
    try {
        val clean = json.replace("{", "").replace("}", "").replace("\"", "")
        val pairs = clean.split(",")
        for (pair in pairs) {
            val parts = pair.split(":")
            if (parts.size == 2) {
                val value = parts[1].trim().toLongOrNull() ?: 0L
                map[parts[0].trim()] = value
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return map
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroup = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroup.toDouble()), units[digitGroup])
}

fun formatEpoch(epoch: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(epoch))
    } catch (e: Exception) {
        "N/A"
    }
}
