package com.example.ui.screens

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
import com.example.ui.components.ExportTeamsDialog
import com.example.ui.components.GoogleUserSwitcherDialog
import com.example.ui.components.ImportTeamsDialog
import com.example.viewmodel.CricketViewModel

@Composable
fun TeamsAndCloudScreen(
    viewModel: CricketViewModel,
    modifier: Modifier = Modifier
) {
    val customTeams by viewModel.customTeams.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val backups by viewModel.backups.collectAsState()
    val activeMatch by viewModel.activeMatch.collectAsState()

    var showTeamEditor by remember { mutableStateOf<Team?>(null) } // null = create new, non-null = edit
    var showUserDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var teamToDelete by remember { mutableStateOf<Team?>(null) }
    var showDeleteAllTeamsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Google Account Profile Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4285F4),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(userProfile.photoInitials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(userProfile.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF34A853).copy(alpha = 0.2f)
                                ) {
                                    Text("Google Verified", color = Color(0xFF2E7D32), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text(userProfile.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(
                        onClick = { showUserDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Switch", fontSize = 11.5.sp)
                    }
                }
            }
        }

        // 2. Google Drive Cloud Sync & Backups
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF4285F4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Drive Cloud Backups", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Button(
                            onClick = { viewModel.backupToDrive() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Sync live match sessions and tournaments securely to your Google Drive for offline restoration.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (backups.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                Text("No cloud backups yet. Tap 'Backup Now' to create one.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        backups.forEach { b ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(b.matchTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${b.formattedDate} • ${b.fileSizeKb} KB JSON", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row {
                                        TextButton(onClick = { viewModel.restoreFromBackup(b) }) {
                                            Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = { viewModel.deleteBackup(b.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Custom Teams Manager ("Add customs Teams and save Teams data for further matches")
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

                // Action Bar: Import, Export, Reset/Delete All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import JSON", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showExportDialog = true },
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

    // Google User switcher dialog
    if (showUserDialog) {
        GoogleUserSwitcherDialog(
            currentProfile = userProfile,
            onDismiss = { showUserDialog = false },
            onLogin = { name, email ->
                viewModel.switchGoogleUser(name, email)
                showUserDialog = false
            }
        )
    }

    // Export Teams Dialog
    if (showExportDialog) {
        ExportTeamsDialog(
            teams = customTeams,
            jsonString = viewModel.exportTeamsJson(),
            onDismiss = { showExportDialog = false },
            onCopied = { /* Handled with toast in dialog */ }
        )
    }

    // Import Teams Dialog
    if (showImportDialog) {
        ImportTeamsDialog(
            onDismiss = { showImportDialog = false },
            onImport = { json, replaceExisting ->
                viewModel.importTeamsJson(json, replaceExisting)
                showImportDialog = false
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
