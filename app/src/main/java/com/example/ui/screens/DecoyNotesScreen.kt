package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DecoyNoteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DecoyNotesScreen(
    notes: List<DecoyNoteEntity>,
    onSaveNote: (DecoyNoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onTriggerSecretVault: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<DecoyNoteEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showSecretHintDialog by remember { mutableStateOf(false) }

    // Check if secret code was entered into search
    if (searchQuery.trim().equals("#vault", ignoreCase = true) ||
        searchQuery.trim().equals("#fortknox", ignoreCase = true) ||
        searchQuery.trim().equals("##open", ignoreCase = true)
    ) {
        searchQuery = ""
        onTriggerSecretVault()
    }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    val noteColors = listOf(
        Color(0xFFFFF9C4), // Light Yellow
        Color(0xFFE1F5FE), // Light Blue
        Color(0xFFE8F5E9), // Light Green
        Color(0xFFFCE4EC), // Light Pink
        Color(0xFFEDE7F6)  // Light Purple
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF8E1))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Secret Trigger 1: Long Press Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        onTriggerSecretVault()
                                    },
                                    onTap = {
                                        // Regular tap does nothing suspicious
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                .background(Color(0xFFFFB300)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "My Notes",
                                color = Color(0xFF37474F),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${notes.size} notes stored locally",
                                color = Color(0xFF78909C),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Discreet secret trigger helper button (top right)
                    IconButton(onClick = { showSecretHintDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Vault Access",
                            tint = Color(0xFFB0BEC5).copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notes...", color = Color(0xFF90A4AE), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF90A4AE)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF90A4AE)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.CutCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color(0xFFFFE082),
                        focusedTextColor = Color(0xFF263238),
                        unfocusedTextColor = Color(0xFF263238)
                    ),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreatingNew = true },
                containerColor = Color(0xFFFFB300),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        containerColor = Color(0xFFFFFDF5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filteredNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFFFFE082),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No notes matching \"$searchQuery\"" else "No notes yet",
                        color = Color(0xFF546E7A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Tap the + button to write a quick note",
                        color = Color(0xFF90A4AE),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        val cardBg = noteColors[note.colorTag.coerceIn(0, noteColors.lastIndex)]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingNote = note },
                            shape = androidx.compose.foundation.shape.CutCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title,
                                        color = Color(0xFF263238),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDeleteNote(note.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFF78909C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (note.content.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = note.content,
                                        color = Color(0xFF455A64),
                                        fontSize = 13.sp,
                                        maxLines = 4,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(note.updatedAt))
                                Text(
                                    text = dateStr,
                                    color = Color(0xFF90A4AE),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Note Dialog
    if (isCreatingNew || editingNote != null) {
        var noteTitle by remember { mutableStateOf(editingNote?.title ?: "") }
        var noteContent by remember { mutableStateOf(editingNote?.content ?: "") }
        var selectedColor by remember { mutableStateOf(editingNote?.colorTag ?: 0) }

        // If secret code is typed in title
        if (noteTitle.trim().equals("#vault", ignoreCase = true) ||
            noteTitle.trim().equals("#fortknox", ignoreCase = true)
        ) {
            isCreatingNew = false
            editingNote = null
            onTriggerSecretVault()
        }

        AlertDialog(
            onDismissRequest = {
                isCreatingNew = false
                editingNote = null
            },
            title = {
                Text(
                    text = if (editingNote != null) "Edit Note" else "New Note",
                    color = Color(0xFF263238),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Note content...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 6
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Color Tag", color = Color(0xFF546E7A), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        noteColors.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = index }
                                    .then(
                                        if (selectedColor == index) Modifier.background(
                                            Color(0xFFFFB300).copy(alpha = 0.4f)
                                        ) else Modifier
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteTitle.isNotBlank() || noteContent.isNotBlank()) {
                            val saved = DecoyNoteEntity(
                                id = editingNote?.id ?: 0L,
                                title = noteTitle.ifBlank { "Untitled Note" },
                                content = noteContent,
                                updatedAt = System.currentTimeMillis(),
                                colorTag = selectedColor
                            )
                            onSaveNote(saved)
                        }
                        isCreatingNew = false
                        editingNote = null
                    }
                ) {
                    Text("SAVE", color = Color(0xFFFF8F00), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isCreatingNew = false
                    editingNote = null
                }) {
                    Text("CANCEL", color = Color(0xFF78909C))
                }
            },
            containerColor = Color.White
        )
    }

    // Secret Vault Trigger Dialog / Instructions
    if (showSecretHintDialog) {
        AlertDialog(
            onDismissRequest = { showSecretHintDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fort Knox Stealth Enclave", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Disguise Mode is active. To unlock the real password manager vault, use any secret trigger:",
                        color = Color(0xFFCFD8DC),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("1. Long press \"My Notes\" title for 2.5 seconds", color = Color(0xFF80D8FF), fontSize = 12.sp)
                    Text("2. Type \"#vault\" in the search bar or note title", color = Color(0xFF80D8FF), fontSize = 12.sp)
                    Text("3. Or tap \"ENTER VAULT\" below", color = Color(0xFF80D8FF), fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSecretHintDialog = false
                        onTriggerSecretVault()
                    }
                ) {
                    Text("ENTER VAULT", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecretHintDialog = false }) {
                    Text("CLOSE", color = Color(0xFF90A4AE))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
