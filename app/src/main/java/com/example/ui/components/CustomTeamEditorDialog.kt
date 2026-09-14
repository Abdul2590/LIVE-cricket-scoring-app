package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.util.UUID

@Composable
fun CustomTeamEditorDialog(
    initialTeam: Team?,
    onDismiss: () -> Unit,
    onSaveTeam: (Team) -> Unit
) {
    var teamName by remember { mutableStateOf(initialTeam?.name ?: "") }
    var shortCode by remember { mutableStateOf(initialTeam?.shortCode ?: "") }
    var selectedColorHex by remember { mutableStateOf(initialTeam?.colorHex ?: 0xFF1976D2) }
    var players by remember {
        mutableStateOf(
            initialTeam?.players ?: (1..11).map { index ->
                Player(
                    id = "custom_p_${UUID.randomUUID().toString().take(6)}",
                    name = "Player $index",
                    role = when (index) {
                        in 1..4 -> PlayerRole.BATSMAN
                        5 -> PlayerRole.WICKET_KEEPER
                        in 6..7 -> PlayerRole.ALL_ROUNDER
                        else -> PlayerRole.BOWLER
                    },
                    jerseyNumber = index,
                    isCaptain = (index == 1),
                    isWicketKeeper = (index == 5)
                )
            }
        )
    }

    var editingPlayerIndex by remember { mutableStateOf<Int?>(null) }
    var newPlayerNameInput by remember { mutableStateOf("") }

    val colorPalette = listOf(
        0xFF1976D2, 0xFF388E3C, 0xFFD32F2F, 0xFFF57F17,
        0xFF7B1FA2, 0xFF0097A7, 0xFF455A64, 0xFFE65100
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (initialTeam == null) "Create Custom Team" else "Edit Custom Team", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Save this team to reuse in fixtures, tournaments, and offline matches.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    placeholder = { Text("e.g. Royal Strikers") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shortCode,
                    onValueChange = { shortCode = it.take(4).uppercase() },
                    label = { Text("Short Code (Max 4 chars)") },
                    placeholder = { Text("e.g. RST") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Team Jersey Color", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorPalette.forEach { hex ->
                        Surface(
                            shape = CircleShape,
                            color = Color(hex),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { selectedColorHex = hex }
                        ) {
                            if (selectedColorHex == hex) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Squad Members (${players.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Tap player to edit", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }

                // Add quick player input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newPlayerNameInput,
                        onValueChange = { newPlayerNameInput = it },
                        label = { Text("Add Player Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newPlayerNameInput.isNotBlank()) {
                                players = players + Player(
                                    name = newPlayerNameInput.trim(),
                                    jerseyNumber = players.size + 1
                                )
                                newPlayerNameInput = ""
                            }
                        },
                        enabled = newPlayerNameInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Player list
                players.forEachIndexed { idx, p ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingPlayerIndex = idx }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "${p.role.displayName} • ${p.battingStyle.displayName}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (players.size > 2) {
                                IconButton(
                                    onClick = { players = players.filterIndexed { i, _ -> i != idx } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTeam = Team(
                        id = initialTeam?.id ?: "custom_${UUID.randomUUID()}",
                        name = teamName.trim(),
                        shortCode = if (shortCode.isNotBlank()) shortCode.trim() else teamName.take(3).uppercase(),
                        colorHex = selectedColorHex,
                        players = players,
                        isCustom = true
                    )
                    onSaveTeam(finalTeam)
                },
                enabled = teamName.isNotBlank() && players.size >= 2
            ) {
                Text("Save Custom Team")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Edit individual player dialog
    editingPlayerIndex?.let { idx ->
        if (idx < players.size) {
            PlayerDetailsDialog(
                player = players[idx],
                onDismiss = { editingPlayerIndex = null },
                onSave = { updated ->
                    val updatedList = players.toMutableList()
                    updatedList[idx] = updated
                    players = updatedList
                    editingPlayerIndex = null
                }
            )
        }
    }
}
