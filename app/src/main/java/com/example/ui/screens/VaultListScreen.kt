package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import com.example.lifecycle.ClipboardCleaner
import com.example.ui.components.CopyIconButton
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberBorderGlowing
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanMuted
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberPink
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceHigh
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

enum class VaultDisplayMode {
    FOLDERS,
    ALL_ITEMS
}

@Composable
fun VaultListScreen(
    entries: List<VaultEntry>,
    onEntryClick: (VaultEntry) -> Unit,
    onAddClick: () -> Unit,
    onLockVault: () -> Unit,
    onCopyUsername: (String) -> Unit,
    onCopyPassword: (String) -> Unit,
    onToggleFavorite: (VaultEntry) -> Unit,
    onAddWithFolder: ((String) -> Unit)? = null
) {
    var displayMode by remember { mutableStateOf(VaultDisplayMode.FOLDERS) }
    var selectedFolderDetail by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<VaultCategory?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }

    val countdownSec by ClipboardCleaner.countdownSec.collectAsState()

    // Group entries by effective folder
    val folderGroups = remember(entries) {
        entries.groupBy { it.getEffectiveFolder() }
            .toList()
            .sortedByDescending { it.second.size }
    }

    val filteredEntries = remember(entries, searchQuery, selectedCategory, onlyFavorites, selectedFolderDetail) {
        entries.filter { entry ->
            val matchesFolder = selectedFolderDetail == null || entry.getEffectiveFolder().equals(selectedFolderDetail, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    entry.title.contains(searchQuery, ignoreCase = true) ||
                    entry.username.contains(searchQuery, ignoreCase = true) ||
                    entry.url.contains(searchQuery, ignoreCase = true) ||
                    entry.getEffectiveFolder().contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == null || entry.category == selectedCategory
            val matchesFavorite = !onlyFavorites || entry.isFavorite

            matchesFolder && matchesQuery && matchesCategory && matchesFavorite
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FORT KNOX VAULT",
                            color = CyberTextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(
                        text = "${entries.size} CREDENTIALS SECURED • ${folderGroups.size} FOLDERS",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lock Enclave Button
                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CutCornerShape(8.dp))
                            .background(CyberSurfaceVariant)
                            .border(1.dp, CyberBorder, CutCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Clipboard countdown bar
            AnimatedVisibility(visible = countdownSec != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(CutCornerShape(8.dp))
                        .background(CyberGold.copy(alpha = 0.15f))
                        .border(1.dp, CyberGold.copy(alpha = 0.4f), CutCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CyberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clipboard auto-wipes in ${countdownSec}s",
                                color = CyberGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "CLEAR NOW",
                            color = CyberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable { ClipboardCleaner.cancelWipe() }
                        )
                    }
                }
            }

            // Breadcrumb / Navigation if inside a Folder Detail
            if (selectedFolderDetail != null) {
                FolderDetailHeader(
                    folderName = selectedFolderDetail!!,
                    accountCount = entries.count { it.getEffectiveFolder().equals(selectedFolderDetail, ignoreCase = true) },
                    onBack = { 
                        selectedFolderDetail = null
                        searchQuery = ""
                    }
                )
            } else {
                // View Mode Toggle (Folders vs All Items)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeTabButton(
                        title = "FOLDERS VIEW",
                        icon = Icons.Default.Folder,
                        badgeCount = folderGroups.size,
                        isSelected = displayMode == VaultDisplayMode.FOLDERS,
                        onClick = { displayMode = VaultDisplayMode.FOLDERS },
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabButton(
                        title = "ALL ITEMS",
                        icon = Icons.Default.Key,
                        badgeCount = entries.size,
                        isSelected = displayMode == VaultDisplayMode.ALL_ITEMS,
                        onClick = { displayMode = VaultDisplayMode.ALL_ITEMS },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { 
                    Text(
                        if (selectedFolderDetail != null) "Search in $selectedFolderDetail folder..." 
                        else "Search credentials, usernames, URLs...", 
                        color = CyberTextMuted,
                        fontSize = 13.sp
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "CLEAR",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextPrimary,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface
                ),
                shape = CutCornerShape(10.dp),
                singleLine = true
            )

            // Category & Quick Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        label = "All (${entries.size})",
                        isSelected = selectedCategory == null && !onlyFavorites && selectedFolderDetail == null,
                        onClick = {
                            selectedCategory = null
                            onlyFavorites = false
                            selectedFolderDetail = null
                        }
                    )
                }
                item {
                    FilterChip(
                        label = "⭐ Favorites",
                        isSelected = onlyFavorites,
                        onClick = {
                            onlyFavorites = !onlyFavorites
                            selectedCategory = null
                        }
                    )
                }
                items(VaultCategory.values()) { cat ->
                    FilterChip(
                        label = cat.displayName,
                        isSelected = selectedCategory == cat && !onlyFavorites,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                            onlyFavorites = false
                        }
                    )
                }
            }

            // Screen Content: Either Folder Detail View, Folders Grid/List, or All Items List
            AnimatedContent(
                targetState = Triple(selectedFolderDetail, displayMode, filteredEntries.isEmpty()),
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "vaultContentTransition"
            ) { (currentFolder, mode, isEmpty) ->
                when {
                    // 1. Folder Detail View (e.g. GitHub folder open)
                    currentFolder != null -> {
                        val folderAccounts = filteredEntries.filter { it.getEffectiveFolder().equals(currentFolder, ignoreCase = true) }
                        if (folderAccounts.isEmpty()) {
                            EmptyFolderState(
                                folderName = currentFolder,
                                onAdd = {
                                    if (onAddWithFolder != null) onAddWithFolder(currentFolder)
                                    else onAddClick()
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    FolderDetailBanner(
                                        folderName = currentFolder,
                                        count = folderAccounts.size
                                    )
                                }
                                items(folderAccounts, key = { it.id }) { entry ->
                                    VaultEntryRow(
                                        entry = entry,
                                        onClick = { onEntryClick(entry) },
                                        onCopyUsername = { onCopyUsername(entry.username) },
                                        onCopyPassword = { onCopyPassword(entry.password) },
                                        onToggleFavorite = { onToggleFavorite(entry) },
                                        showFolderBadge = false
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(90.dp))
                                }
                            }
                        }
                    }

                    // 2. Folders View Mode
                    mode == VaultDisplayMode.FOLDERS && searchQuery.isBlank() && selectedCategory == null && !onlyFavorites -> {
                        if (folderGroups.isEmpty()) {
                            EmptyVaultState(onAddClick = onAddClick)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Text(
                                        text = "ORGANIZED FOLDERS",
                                        color = CyberTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                items(folderGroups, key = { it.first }) { (folderName, itemsInFolder) ->
                                    CyberFolderCard(
                                        folderName = folderName,
                                        entries = itemsInFolder,
                                        onClick = { selectedFolderDetail = folderName }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(90.dp))
                                }
                            }
                        }
                    }

                    // 3. All Items / Search / Filtered List
                    else -> {
                        if (isEmpty) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = CyberTextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (entries.isEmpty()) "Vault is empty" else "No matching items found",
                                        color = CyberTextSecondary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (entries.isEmpty()) "Tap '+' to secure your first login" else "Try adjusting search query or filters",
                                        color = CyberTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredEntries, key = { it.id }) { entry ->
                                    VaultEntryRow(
                                        entry = entry,
                                        onClick = { onEntryClick(entry) },
                                        onCopyUsername = { onCopyUsername(entry.username) },
                                        onCopyPassword = { onCopyPassword(entry.password) },
                                        onToggleFavorite = { onToggleFavorite(entry) },
                                        showFolderBadge = true,
                                        onFolderClick = { selectedFolderDetail = entry.getEffectiveFolder() }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(90.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sleek Cyber Extended FAB to Add Entry
        CyberExtendedFab(
            label = if (selectedFolderDetail != null) "ADD TO $selectedFolderDetail" else "ADD SECURE ENTRY",
            onClick = {
                if (selectedFolderDetail != null && onAddWithFolder != null) {
                    onAddWithFolder(selectedFolderDetail!!)
                } else {
                    onAddClick()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )
    }
}

/**
 * Top breadcrumb bar when inspecting a specific folder (e.g. GitHub).
 */
@Composable
private fun FolderDetailHeader(
    folderName: String,
    accountCount: Int,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        color = CyberSurface,
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
        shape = CutCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onBack)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Folders",
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FOLDERS",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = " / ",
                    color = CyberCyanMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = folderName,
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Count Badge
            Surface(
                color = CyberCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "$accountCount ACCOUNTS",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Header banner inside a folder view.
 */
@Composable
private fun FolderDetailBanner(
    folderName: String,
    count: Int
) {
    val folderIcon = getFolderIcon(folderName)
    val folderColor = getFolderColor(folderName)

    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CutCornerShape(10.dp))
                    .background(folderColor.copy(alpha = 0.15f))
                    .border(1.dp, folderColor.copy(alpha = 0.5f), CutCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = folderIcon,
                    contentDescription = null,
                    tint = folderColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$folderName Accounts",
                    color = CyberTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Encrypted credentials stored in this folder enclave",
                    color = CyberTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Cyber Folder Card displaying folder name, icon, accounts count, and quick previews.
 */
@Composable
private fun CyberFolderCard(
    folderName: String,
    entries: List<VaultEntry>,
    onClick: () -> Unit
) {
    val folderIcon = getFolderIcon(folderName)
    val folderColor = getFolderColor(folderName)

    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Folder Icon Badge
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CutCornerShape(10.dp))
                            .background(folderColor.copy(alpha = 0.15f))
                            .border(1.dp, folderColor.copy(alpha = 0.4f), CutCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = folderIcon,
                            contentDescription = null,
                            tint = folderColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folderName,
                            color = CyberTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${entries.size} ${if (entries.size == 1) "account" else "accounts"} secured",
                            color = folderColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Arrow Action
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Folder",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quick Account Titles Preview Chips
            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entries.take(3).forEach { entry ->
                        Surface(
                            color = CyberSurfaceVariant,
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = entry.title.take(18),
                                color = CyberTextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (entries.size > 3) {
                        Surface(
                            color = CyberSurfaceVariant,
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "+${entries.size - 3} more",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab switcher for Folders vs All Items.
 */
@Composable
private fun ModeTabButton(
    title: String,
    icon: ImageVector,
    badgeCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(CutCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) CyberCyan.copy(alpha = 0.15f) else CyberSurface,
        border = BorderStroke(
            1.dp,
            if (isSelected) CyberCyan else CyberBorder
        ),
        shape = CutCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CyberCyan else CyberTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$title ($badgeCount)",
                color = if (isSelected) CyberCyan else CyberTextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CutCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurface,
        border = BorderStroke(
            1.dp,
            if (isSelected) CyberCyan else CyberBorder
        ),
        shape = CutCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) CyberCyan else CyberTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun VaultEntryRow(
    entry: VaultEntry,
    onClick: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onToggleFavorite: () -> Unit,
    showFolderBadge: Boolean = true,
    onFolderClick: (() -> Unit)? = null
) {
    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CutCornerShape(10.dp))
                        .background(
                            when (entry.category) {
                                VaultCategory.LOGINS -> CyberCyan.copy(alpha = 0.15f)
                                VaultCategory.SECURE_NOTES -> CyberPurple.copy(alpha = 0.15f)
                                VaultCategory.CARDS -> CyberGold.copy(alpha = 0.15f)
                                VaultCategory.IDENTITY -> CyberEmerald.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when (entry.category) {
                                VaultCategory.LOGINS -> CyberCyan.copy(alpha = 0.3f)
                                VaultCategory.SECURE_NOTES -> CyberPurple.copy(alpha = 0.3f)
                                VaultCategory.CARDS -> CyberGold.copy(alpha = 0.3f)
                                VaultCategory.IDENTITY -> CyberEmerald.copy(alpha = 0.3f)
                            },
                            CutCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (entry.category) {
                            VaultCategory.LOGINS -> Icons.Default.Key
                            VaultCategory.SECURE_NOTES -> Icons.Default.Lock
                            VaultCategory.CARDS -> Icons.Default.CreditCard
                            VaultCategory.IDENTITY -> Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = when (entry.category) {
                            VaultCategory.LOGINS -> CyberCyan
                            VaultCategory.SECURE_NOTES -> CyberPurple
                            VaultCategory.CARDS -> CyberGold
                            VaultCategory.IDENTITY -> CyberEmerald
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        color = CyberTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (entry.username.isNotBlank()) {
                        Text(
                            text = entry.username,
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Folder tag badge
                    if (showFolderBadge) {
                        val folderName = entry.getEffectiveFolder()
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberSurfaceHigh)
                                .then(if (onFolderClick != null) Modifier.clickable(onClick = onFolderClick) else Modifier)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = folderName,
                                color = CyberCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Quick Copy actions & Favorite
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (entry.isFavorite) CyberGold else CyberTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (entry.username.isNotBlank()) {
                    CopyIconButton(
                        textToCopy = entry.username,
                        onCopy = onCopyUsername
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (entry.password.isNotBlank()) {
                    CopyIconButton(
                        textToCopy = entry.password,
                        onCopy = onCopyPassword
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFolderState(
    folderName: String,
    onAdd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = CyberCyanMuted,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No accounts in '$folderName'",
                color = CyberTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Store new credentials directly in this folder enclave",
                color = CyberTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .clip(CutCornerShape(8.dp))
                    .clickable(onClick = onAdd),
                color = CyberCyan.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, CyberCyan),
                shape = CutCornerShape(8.dp)
            ) {
                Text(
                    text = "+ ADD ACCOUNT TO $folderName",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyVaultState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                tint = CyberTextMuted,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Vault is empty",
                color = CyberTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap below to secure your first login or credential",
                color = CyberTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Modern sleek cyber extended floating action button.
 */
@Composable
fun CyberExtendedFab(
    onClick: () -> Unit,
    label: String = "ADD SECURE ENTRY",
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fab_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "hud_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(CyberBackground.copy(alpha = 0.85f))
            .border(1.dp, CyberCyanMuted.copy(alpha = pulse), RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp)) {
                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
                    drawArc(
                        color = CyberCyan,
                        startAngle = 0f, sweepAngle = 100f, useCenter = false,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawArc(
                        color = CyberCyan,
                        startAngle = 180f, sweepAngle = 100f, useCenter = false,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                color = CyberCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.2.sp
            )
        }
    }
}

/**
 * Returns distinct themed icons for known folder names.
 */
private fun getFolderIcon(folderName: String): ImageVector {
    val lower = folderName.lowercase()
    return when {
        lower.contains("github") -> Icons.Default.Code
        lower.contains("google") || lower.contains("aws") || lower.contains("cloud") -> Icons.Default.Cloud
        lower.contains("work") || lower.contains("enterprise") -> Icons.Default.Work
        lower.contains("netflix") || lower.contains("entertainment") || lower.contains("spotify") -> Icons.Default.Movie
        lower.contains("crypto") || lower.contains("wallet") -> Icons.Default.Shield
        lower.contains("card") || lower.contains("finance") || lower.contains("bank") -> Icons.Default.CreditCard
        lower.contains("identity") -> Icons.Default.Person
        else -> Icons.Default.Folder
    }
}

/**
 * Returns distinct themed neon colors for known folder names.
 */
private fun getFolderColor(folderName: String): Color {
    val lower = folderName.lowercase()
    return when {
        lower.contains("github") -> CyberCyan
        lower.contains("google") -> CyberEmerald
        lower.contains("aws") || lower.contains("amazon") -> CyberGold
        lower.contains("work") -> CyberPurple
        lower.contains("entertainment") || lower.contains("netflix") -> CyberPink
        lower.contains("crypto") -> CyberEmerald
        lower.contains("finance") || lower.contains("card") -> CyberGold
        else -> CyberCyan
    }
}
