package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CustomTeamEditorDialog
import com.example.ui.components.ExportAllAppDataDialog
import com.example.ui.components.ExportTeamsDialog
import com.example.ui.components.ImportTeamsDialog
import com.example.ui.components.RestoreAllAppDataDialog
import com.example.ui.components.ScorerProfileDialog
import com.example.viewmodel.CricketViewModel

@Composable
fun TeamsAndCloudScreen(
    viewModel: CricketViewModel,
    modifier: Modifier = Modifier
) {
    val customTeams by viewModel.customTeams.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isAutoSaving by viewModel.isAutoSaving.collectAsState()
    val matches by viewModel.matches.collectAsState()

    var showTeamEditor by remember { mutableStateOf<Team?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showExportAllDialog by remember { mutableStateOf(false) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }
    var showExportTeamsDialog by remember { mutableStateOf(false) }
    var showImportTeamsDialog by remember { mutableStateOf(false) }
    var teamToDelete by remember { mutableStateOf<Team?>(null) }
    var showDeleteAllTeamsDialog by remember { mutableStateOf(false) }

    val storageInfo = remember(matches, customTeams, userProfile) {
        viewModel.getLocalStorageInfo()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Scorer Profile & Local Storage Auto-Save Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row: Scorer Profile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userProfile.photoInitials.ifBlank { "RS" },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(userProfile.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (userProfile.autoSaveEnabled) Color(0xFF34A853).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (userProfile.autoSaveEnabled) "AUTO-SAVE ACTIVE" else "AUTO-SAVE PAUSED",
                                            color = if (userProfile.autoSaveEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(userProfile.role, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedButton(
                            onClick = { showProfileDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settings", fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Local Storage Auto-Save Engine Details
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Local Storage Auto-Save Engine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isAutoSaving) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Saving...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            // Storage Metrics Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Matches Saved", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("${storageInfo.totalMatches}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Custom Squads", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("${storageInfo.totalTeams}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Balls Logged", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("${storageInfo.totalDeliveries}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Storage Size", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("~${storageInfo.estimatedSizeKb} KB", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Target: criclive_data/app_local_backup.json",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (userProfile.lastAutoSaveTime.isNotBlank()) "Last saved: ${userProfile.lastAutoSaveTime}" else "Auto-save active",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Text(
                                text = "✓ Offline-first: Every live delivery, ball event, custom squad, and tournament fixture is continuously auto-saved directly on your device storage without requiring internet or cloud sign-in.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Quick Action Buttons for Full Local App Data
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.saveManualBackupSnapshot() },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Now", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { showExportAllDialog = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export Backup", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { showRestoreAllDialog = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Custom Teams Manager ("Add customs Teams and save Teams data for further matches")
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SAVED CUSTOM TEAMS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Create, import, and export custom squads for all matches", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showTeamEditor = Team(name = "", shortCode = "") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Team", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action Bar: Import, Export, Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showImportTeamsDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import JSON", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showExportTeamsDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = customTeams.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", fontSize = 11.sp)
                    }

                    if (customTeams.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showDeleteAllTeamsDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Clear", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (customTeams.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No custom teams created yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Tap 'New Team' or 'Import JSON' to load custom teams.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { viewModel.resetCustomTeamsToDefault() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load Default Custom Squads", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        } else {
            items(customTeams) { team ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(team.colorHex),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(team.shortCode.take(2), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(team.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${team.shortCode} • ${team.players.size} players", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row {
                                IconButton(onClick = { showTeamEditor = team }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit team", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { teamToDelete = team }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete team", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Players chips
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Squad: " + team.players.joinToString(", ") { it.name },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }

    // Dialogs

    // Scorer Profile Dialog
    if (showProfileDialog) {
        ScorerProfileDialog(
            currentProfile = userProfile,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, role, email ->
                viewModel.updateScorerProfile(name, role, email)
            },
            onToggleAutoSave = { enabled ->
                viewModel.toggleAutoSave(enabled)
            }
        )
    }

    // Export All Application Data Dialog
    if (showExportAllDialog) {
        ExportAllAppDataDialog(
            jsonString = viewModel.exportAllApplicationDataJson(),
            onDismiss = { showExportAllDialog = false }
        )
    }

    // Restore All Application Data Dialog
    if (showRestoreAllDialog) {
        RestoreAllAppDataDialog(
            onDismiss = { showRestoreAllDialog = false },
            onRestore = { json ->
                viewModel.restoreAllApplicationDataJson(json)
                showRestoreAllDialog = false
            }
        )
    }

    // Custom Team Editor dialog
    showTeamEditor?.let { editingTeam ->
        CustomTeamEditorDialog(
            initialTeam = if (editingTeam.name.isNotBlank()) editingTeam else null,
            onDismiss = { showTeamEditor = null },
            onSaveTeam = { savedTeam ->
                viewModel.saveCustomTeam(savedTeam)
                showTeamEditor = null
            }
        )
    }

    // Export Teams Dialog
    if (showExportTeamsDialog) {
        ExportTeamsDialog(
            teams = customTeams,
            jsonString = viewModel.exportTeamsJson(),
            onDismiss = { showExportTeamsDialog = false },
            onCopied = { /* Handled with toast in dialog */ }
        )
    }

    // Import Teams Dialog
    if (showImportTeamsDialog) {
        ImportTeamsDialog(
            onDismiss = { showImportTeamsDialog = false },
            onImport = { json, replaceExisting ->
                viewModel.importTeamsJson(json, replaceExisting)
                showImportTeamsDialog = false
            }
        )
    }

    // Confirm Delete Team Dialog
    teamToDelete?.let { team ->
        ConfirmDeleteDialog(
            title = "Delete Custom Team?",
            message = "Are you sure you want to delete '${team.name}' (${team.shortCode}) and its ${team.players.size} players? This action cannot be undone.",
            confirmButtonText = "Delete Team",
            onDismiss = { teamToDelete = null },
            onConfirm = {
                viewModel.deleteCustomTeam(team.id)
                teamToDelete = null
            }
        )
    }

    // Confirm Delete All Teams Dialog
    if (showDeleteAllTeamsDialog) {
        ConfirmDeleteDialog(
            title = "Delete All Custom Teams?",
            message = "Are you sure you want to delete all ${customTeams.size} custom teams? All saved squads will be cleared.",
            confirmButtonText = "Clear All Teams",
            onDismiss = { showDeleteAllTeamsDialog = false },
            onConfirm = {
                viewModel.deleteAllCustomTeams()
                showDeleteAllTeamsDialog = false
            }
        )
    }
}
