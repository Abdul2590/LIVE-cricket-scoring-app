package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import android.widget.Toast
import kotlinx.coroutines.launch

@Composable
fun EditPlayerNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Player Name", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Update the player's official display name for the live scorecard and commentary.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save Name")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PlayerDetailsDialog(
    player: Player,
    onDismiss: () -> Unit,
    onSave: (Player) -> Unit
) {
    var name by remember { mutableStateOf(player.name) }
    var role by remember { mutableStateOf(player.role) }
    var battingStyle by remember { mutableStateOf(player.battingStyle) }
    var bowlingStyle by remember { mutableStateOf(player.bowlingStyle) }
    var isCaptain by remember { mutableStateOf(player.isCaptain) }
    var isWk by remember { mutableStateOf(player.isWicketKeeper) }
    var jersey by remember { mutableStateOf(player.jerseyNumber.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Player Details", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Playing Role", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlayerRole.values().forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.displayName, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("Batting Hand", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BattingStyle.values().forEach { bs ->
                        FilterChip(
                            selected = battingStyle == bs,
                            onClick = { battingStyle = bs },
                            label = { Text(bs.displayName, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("Bowling Style", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BowlingStyle.values().take(4).forEach { bst ->
                        FilterChip(
                            selected = bowlingStyle == bst,
                            onClick = { bowlingStyle = bst },
                            label = { Text(bst.displayName, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Team Captain", fontSize = 13.sp)
                    Switch(checked = isCaptain, onCheckedChange = { isCaptain = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Wicketkeeper", fontSize = 13.sp)
                    Switch(checked = isWk, onCheckedChange = { isWk = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = player.copy(
                        name = name.trim(),
                        role = role,
                        battingStyle = battingStyle,
                        bowlingStyle = bowlingStyle,
                        isCaptain = isCaptain,
                        isWicketKeeper = isWk,
                        jerseyNumber = jersey.toIntOrNull() ?: player.jerseyNumber
                    )
                    onSave(updated)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Details")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangeBatsmanDialog(
    battingSquad: List<Player>,
    bowlingSquad: List<Player>,
    currentStrikerId: String,
    currentNonStrikerId: String,
    currentBowlerId: String,
    onDismiss: () -> Unit,
    onConfirm: (strikerId: String, nonStrikerId: String, bowlerId: String) -> Unit
) {
    var selectedStriker by remember { mutableStateOf(currentStrikerId) }
    var selectedNonStriker by remember { mutableStateOf(currentNonStrikerId) }
    var selectedBowler by remember { mutableStateOf(currentBowlerId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Batsmen / Bowler", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Select who is currently on strike, the non-striker, and the active bowler before beginning or during play.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Striker
                Text("Striker (On-Strike Batsman) 🏏", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    battingSquad.forEach { player ->
                        val isSelected = selectedStriker == player.id
                        val isNonStriker = selectedNonStriker == player.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isNonStriker) { selectedStriker = player.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = null, enabled = !isNonStriker)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${player.name} ${if (isNonStriker) "(Non-Striker)" else ""}",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${player.role.displayName} • ${player.battingStyle.displayName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Non Striker
                Text("Non-Striker Batsman 🏃", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    battingSquad.forEach { player ->
                        val isSelected = selectedNonStriker == player.id
                        val isStriker = selectedStriker == player.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isStriker) { selectedNonStriker = player.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = null, enabled = !isStriker)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${player.name} ${if (isStriker) "(Striker)" else ""}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Bowler
                Text("Active Bowler 🎯", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bowlingSquad.forEach { player ->
                        val isSelected = selectedBowler == player.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBowler = player.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(player.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                    Text("${player.bowlingStyle.displayName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStriker, selectedNonStriker, selectedBowler) },
                enabled = selectedStriker.isNotBlank() && selectedNonStriker.isNotBlank() && selectedStriker != selectedNonStriker
            ) {
                Text("Apply Selection")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SelectBowlerDialog(
    bowlingSquad: List<Player>,
    currentBowlerId: String,
    lastOverBowlerId: String = "",
    isWaitingForNewBowler: Boolean = false,
    overNumber: Int = 1,
    bowlingStats: List<BowlingStat> = emptyList(),
    onDismiss: () -> Unit,
    onSelectBowler: (bowlerId: String) -> Unit
) {
    val initialSelection = remember(currentBowlerId, lastOverBowlerId, isWaitingForNewBowler, bowlingSquad, overNumber) {
        if (isWaitingForNewBowler && overNumber > 1 && currentBowlerId == lastOverBowlerId && bowlingSquad.size > 1) {
            bowlingSquad.firstOrNull { it.id != lastOverBowlerId }?.id ?: ""
        } else if (currentBowlerId.isNotBlank()) {
            currentBowlerId
        } else {
            bowlingSquad.firstOrNull { it.role == PlayerRole.BOWLER }?.id
                ?: bowlingSquad.firstOrNull { it.role == PlayerRole.ALL_ROUNDER }?.id
                ?: bowlingSquad.firstOrNull()?.id
                ?: ""
        }
    }
    var selectedBowler by remember { mutableStateOf(initialSelection) }

    AlertDialog(
        onDismissRequest = {
            // If waiting for new bowler, user must select or dismiss to acknowledge
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isWaitingForNewBowler) Icons.Default.SportsBaseball else Icons.Default.SportsCricket,
                    contentDescription = null,
                    tint = if (isWaitingForNewBowler) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (overNumber == 1 && currentBowlerId.isBlank()) "Select Opening Bowler (Over 1)"
                    else if (isWaitingForNewBowler && overNumber > 1) "Select Bowler for Over $overNumber"
                    else "Change / Select Bowler",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isWaitingForNewBowler && overNumber > 1) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Over ${overNumber - 1} completed! Bowler cannot bowl consecutive overs. Choose the bowler for Over $overNumber.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (overNumber == 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsBaseball, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Select the opening bowler from the fielding team to bowl the first over of the innings.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Text(
                        "Choose an active bowler from the fielding team to bowl the next deliveries.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                bowlingSquad.forEach { player ->
                    val isLastOverBowler = isWaitingForNewBowler && overNumber > 1 && player.id == lastOverBowlerId && bowlingSquad.size > 1
                    val isSelected = selectedBowler == player.id && !isLastOverBowler
                    val stat = bowlingStats.find { it.playerId == player.id }
                    
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isLastOverBowler -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLastOverBowler) {
                                selectedBowler = player.id
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = if (!isLastOverBowler) { { selectedBowler = player.id } } else null,
                                enabled = !isLastOverBowler
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        player.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = if (isLastOverBowler) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isLastOverBowler) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "Just Bowled",
                                                color = Color(0xFFC62828),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else if (player.id == currentBowlerId && !isWaitingForNewBowler) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "Current",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    if (isLastOverBowler) "Restricted: cannot bowl back-to-back overs" else "${player.role.displayName} • ${player.bowlingStyle.displayName}",
                                    fontSize = 11.sp,
                                    color = if (isLastOverBowler) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (stat != null && stat.legalBalls > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${stat.wickets}/${stat.runsConceded}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isLastOverBowler) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${stat.oversString} ov • Econ: ${String.format(java.util.Locale.US, "%.1f", stat.economy)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                    if (selectedBowler.isNotBlank() && (!isWaitingForNewBowler || selectedBowler != lastOverBowlerId || bowlingSquad.size <= 1)) {
                        onSelectBowler(selectedBowler)
                    }
                },
                enabled = selectedBowler.isNotBlank() && (!isWaitingForNewBowler || selectedBowler != lastOverBowlerId || bowlingSquad.size <= 1),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWaitingForNewBowler) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isWaitingForNewBowler) "Set Bowler for Over $overNumber" else "Set Bowler")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun WicketDismissalDialog(
    striker: Player?,
    nonStriker: Player?,
    currentBowler: Player?,
    fieldingSquad: List<Player>,
    onDismiss: () -> Unit,
    onConfirm: (runsBat: Int, dismissal: Dismissal) -> Unit
) {
    var wicketType by remember { mutableStateOf(WicketType.BOWLED) }
    var outPlayerId by remember { mutableStateOf(striker?.id ?: "") }
    var fielderName by remember { mutableStateOf("") }
    var runsOffWicketBall by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsCricket, contentDescription = null, tint = Color(0xFFC2185B))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wicket Dismissal Flow", fontWeight = FontWeight.Bold, color = Color(0xFFC2185B))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Who got OUT?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = outPlayerId == striker?.id,
                        onClick = { striker?.id?.let { outPlayerId = it } },
                        label = { Text("Striker (${striker?.name?.take(10) ?: "Striker"})") }
                    )
                    FilterChip(
                        selected = outPlayerId == nonStriker?.id,
                        onClick = { nonStriker?.id?.let { outPlayerId = it } },
                        label = { Text("Non-Striker (${nonStriker?.name?.take(10) ?: "Non-Striker"})") }
                    )
                }

                Text("Dismissal Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(WicketType.BOWLED, WicketType.CAUGHT, WicketType.LBW).forEach { wt ->
                            FilterChip(
                                selected = wicketType == wt,
                                onClick = { wicketType = wt },
                                label = { Text(wt.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(WicketType.RUN_OUT, WicketType.STUMPED, WicketType.HIT_WICKET).forEach { wt ->
                            FilterChip(
                                selected = wicketType == wt,
                                onClick = { wicketType = wt },
                                label = { Text(wt.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (wicketType == WicketType.CAUGHT || wicketType == WicketType.RUN_OUT || wicketType == WicketType.STUMPED) {
                    OutlinedTextField(
                        value = fielderName,
                        onValueChange = { fielderName = it },
                        label = { Text("Fielder Name (Optional)") },
                        placeholder = { Text("e.g. Jadeja") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Runs completed before dismissal:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 1, 2, 3).forEach { r ->
                        FilterChip(
                            selected = runsOffWicketBall == r,
                            onClick = { runsOffWicketBall = r },
                            label = { Text("$r run${if (r != 1) "s" else ""}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dismissal = Dismissal(
                        type = wicketType,
                        outPlayerId = outPlayerId,
                        bowlerId = currentBowler?.id,
                        fielderName = fielderName.takeIf { it.isNotBlank() }
                    )
                    onConfirm(runsOffWicketBall, dismissal)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B))
            ) {
                Text("Confirm Wicket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ScorerProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (name: String, role: String, email: String) -> Unit,
    onToggleAutoSave: (Boolean) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.displayName) }
    var role by remember { mutableStateOf(currentProfile.role) }
    var email by remember { mutableStateOf(currentProfile.email) }
    var autoSave by remember { mutableStateOf(currentProfile.autoSaveEnabled) }

    val roleSuggestions = listOf("Lead Scorer & Match Official", "Official Scorer", "Club Administrator", "Team Captain")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentProfile.photoInitials.ifBlank { "RS" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Scorer Profile & Storage", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Local storage auto-save settings", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "All matches, live deliveries, custom squads, and tournament fixtures are automatically saved to your device's local internal storage.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Scorer Details
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Scorer / Official Name") },
                    placeholder = { Text("e.g. Rehman Shaikh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Official Role / Title") },
                    placeholder = { Text("e.g. Lead Scorer & Match Official") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Role quick-select chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roleSuggestions.take(2).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.take(15), fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Scorer Contact / Email (Optional)") },
                    placeholder = { Text("e.g. scorer@criclive.local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(modifier = Modifier.padding(vertical = 2.dp))

                // Auto-save toggle card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Save to Local Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "Instantly persist every ball, match event, and squad change to device internal storage",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = autoSave,
                            onCheckedChange = {
                                autoSave = it
                                onToggleAutoSave(it)
                            }
                        )
                    }
                }

                // Storage location details
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Storage Location: Internal App Storage", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                        Text(
                            "Directory: criclive_data/app_local_backup.json\nStatus: ${currentProfile.storageStatusDescription}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(name, role, email)
                    onDismiss()
                },
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ExportAllAppDataDialog(
    jsonString: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export All App Data", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Complete offline backup of all matches, scorecards, balls, custom teams, and settings in JSON format.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = jsonString,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(jsonString))
                    Toast.makeText(context, "Full app backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun RestoreAllAppDataDialog(
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore App Data", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Paste a previously exported CricLive backup JSON to restore all matches and custom teams.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("Backup JSON Content") },
                    placeholder = { Text("{\n  \"storageType\": \"...\",\n  \"matches\": [...]\n}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jsonText.isNotBlank()) {
                        onRestore(jsonText)
                    }
                },
                enabled = jsonText.isNotBlank()
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore All Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
