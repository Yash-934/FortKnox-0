package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import com.example.security.PasswordGenerator
import com.example.ui.components.CopyIconButton
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberOutlinedButton
import com.example.ui.components.PasswordStrengthBar
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberLaserRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun AddEditEntryScreen(
    entryToEdit: VaultEntry?,
    onSave: (VaultEntry) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    onCopyUsername: (String) -> Unit,
    onCopyPassword: (String) -> Unit
) {
    var title by remember { mutableStateOf(entryToEdit?.title ?: "") }
    var username by remember { mutableStateOf(entryToEdit?.username ?: "") }
    var password by remember { mutableStateOf(entryToEdit?.password ?: "") }
    var url by remember { mutableStateOf(entryToEdit?.url ?: "") }
    var notes by remember { mutableStateOf(entryToEdit?.notes ?: "") }
    var category by remember { mutableStateOf(entryToEdit?.category ?: VaultCategory.LOGINS) }
    var isFavorite by remember { mutableStateOf(entryToEdit?.isFavorite ?: false) }

    var passwordVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val strengthAnalysis = remember(password) {
        PasswordGenerator.evaluateStrength(password)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyberCyan
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (entryToEdit == null) "NEW VAULT ENTRY" else "EDIT CREDENTIALS",
                    color = CyberTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            if (entryToEdit != null) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = CyberLaserRed
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Category Selection Chips
                    Text(
                        text = "ENTRY CATEGORY",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (cat in VaultCategory.values()) {
                            val isSelected = category == cat
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                    .clickable { category = cat },
                                color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyberCyan else CyberBorder
                                )
                            ) {
                                Text(
                                    text = cat.displayName,
                                    color = if (isSelected) CyberCyan else CyberTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Service Name (e.g. Google, GitHub)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Username / Email
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username / Email") },
                            modifier = Modifier.weight(1f),
                            colors = customFieldColors(),
                            singleLine = true
                        )
                        if (username.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CopyIconButton(textToCopy = username, onCopy = { onCopyUsername(username) })
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = CyberTextMuted
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = customFieldColors(),
                            singleLine = true
                        )
                        if (password.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CopyIconButton(textToCopy = password, onCopy = { onCopyPassword(password) })
                        }
                    }

                    // Strength bar
                    if (password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PasswordStrengthBar(analysis = strengthAnalysis)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick generate strong button
                    CyberOutlinedButton(
                        text = "Generate Strong Password",
                        icon = Icons.Default.AutoAwesome,
                        onClick = {
                            password = PasswordGenerator.generatePassword(length = 20)
                            passwordVisible = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Website / Autofill Domain URL
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Website Domain / App Identifier") },
                        placeholder = { Text("https://example.com or package name", color = CyberTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Secure Notes / Recovery Codes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = customFieldColors(),
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            CyberButton(
                text = if (entryToEdit == null) "SAVE TO ENCLAVE" else "UPDATE ENTRY",
                onClick = {
                    if (title.isNotBlank()) {
                        val updated = VaultEntry(
                            id = entryToEdit?.id ?: 0L,
                            title = title.trim(),
                            username = username.trim(),
                            password = password,
                            url = url.trim(),
                            notes = notes.trim(),
                            category = category,
                            isFavorite = isFavorite,
                            createdAt = entryToEdit?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(updated)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeleteDialog && entryToEdit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Credential", color = CyberTextPrimary) },
            text = { Text("Are you sure you want to permanently delete '${entryToEdit.title}' from the vault?", color = CyberTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(entryToEdit.id)
                    }
                ) {
                    Text("DELETE", color = CyberLaserRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL", color = CyberTextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }
}

@Composable
private fun customFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberCyan,
    unfocusedBorderColor = CyberBorder,
    focusedTextColor = CyberTextPrimary,
    unfocusedTextColor = CyberTextPrimary,
    focusedLabelColor = CyberCyan,
    unfocusedLabelColor = CyberTextSecondary
)
