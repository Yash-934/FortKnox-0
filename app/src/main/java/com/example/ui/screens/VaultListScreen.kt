package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.example.ui.theme.CyberCyanMuted
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.CyberCyan
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

@Composable
fun VaultListScreen(
    entries: List<VaultEntry>,
    onEntryClick: (VaultEntry) -> Unit,
    onAddClick: () -> Unit,
    onLockVault: () -> Unit,
    onCopyUsername: (String) -> Unit,
    onCopyPassword: (String) -> Unit,
    onToggleFavorite: (VaultEntry) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<VaultCategory?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }

    val countdownSec by ClipboardCleaner.countdownSec.collectAsState()

    val filteredEntries = remember(entries, searchQuery, selectedCategory, onlyFavorites) {
        entries.filter { entry ->
            val matchesQuery = searchQuery.isBlank() ||
                    entry.title.contains(searchQuery, ignoreCase = true) ||
                    entry.username.contains(searchQuery, ignoreCase = true) ||
                    entry.url.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == null || entry.category == selectedCategory
            val matchesFavorite = !onlyFavorites || entry.isFavorite

            matchesQuery && matchesCategory && matchesFavorite
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FORT KNOX",
                        color = CyberTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "${entries.size} CREDENTIALS SECURED",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lock Button
                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CutCornerShape(10.dp))
                            .background(CyberSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
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
                        .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                        .background(CyberGold.copy(alpha = 0.15f))
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
                                text = "Clipboard will self-clear in ${countdownSec}s",
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
                            modifier = Modifier.clickable { ClipboardCleaner.cancelWipe() }
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search decrypted vault...", color = CyberTextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
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
                shape = androidx.compose.foundation.shape.CutCornerShape(12.dp),
                singleLine = true
            )

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        label = "All (${entries.size})",
                        isSelected = selectedCategory == null && !onlyFavorites,
                        onClick = {
                            selectedCategory = null
                            onlyFavorites = false
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

            // Vault entries list
            if (filteredEntries.isEmpty()) {
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
                            onToggleFavorite = { onToggleFavorite(entry) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Sleek Cyber Extended FAB to Add Entry
        CyberExtendedFab(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )
    }
}

/**
 * Modern sleek cyber extended floating action button.
 * Features a clean gradient background, high contrast text, and subtle shadows.
 */
@Composable
fun CyberExtendedFab(
    onClick: () -> Unit,
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
            .clip(RoundedCornerShape(50)) // Pill shape matching JARVIS HUD buttons
            .background(CyberBackground.copy(alpha = 0.6f))
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HUD Rotating Icon Ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
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
                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = -rotation * 1.5f }) {
                    drawArc(
                        color = CyberGold, // Amber accent
                        startAngle = 45f, sweepAngle = 45f, useCenter = false,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawArc(
                        color = CyberGold,
                        startAngle = 225f, sweepAngle = 45f, useCenter = false,
                        style = Stroke(width = 1.dp.toPx())
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
                text = "ADD SECURE ENTRY",
                color = CyberCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.5.sp
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
            .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CyberCyan else CyberBorder
        ),
        shape = androidx.compose.foundation.shape.CutCornerShape(8.dp)
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
    onToggleFavorite: () -> Unit
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
                        .clip(androidx.compose.foundation.shape.CutCornerShape(10.dp))
                        .background(
                            when (entry.category) {
                                VaultCategory.LOGINS -> CyberCyan.copy(alpha = 0.15f)
                                VaultCategory.SECURE_NOTES -> CyberPurple.copy(alpha = 0.15f)
                                VaultCategory.CARDS -> CyberGold.copy(alpha = 0.15f)
                                VaultCategory.IDENTITY -> CyberEmerald.copy(alpha = 0.15f)
                            }
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
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.username.isNotBlank()) {
                        Text(
                            text = entry.username,
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
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
                    Spacer(modifier = Modifier.width(6.dp))
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
