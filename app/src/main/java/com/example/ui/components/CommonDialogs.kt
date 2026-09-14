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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

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
fun GoogleUserSwitcherDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onLogin: (name: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.displayName) }
    var email by remember { mutableStateOf(currentProfile.email) }

    val sampleUsers = listOf(
        Pair("Rehman Shaikh (Lead Scorer)", "rehman.shaikh4@gmail.com"),
        Pair("Official Match Umpire", "umpire.icc.cricket@gmail.com"),
        Pair("Club Admin", "admin.strikers@gmail.com")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4285F4),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Google Account Sign-In", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                    "Sign in or switch to another Google user profile to sync matches and backups with Google Drive.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Quick Switch Profile:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                sampleUsers.forEach { (uName, uEmail) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (email == uEmail) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                name = uName
                                email = uEmail
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(uName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(uEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Or enter custom Google credentials:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Google Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onLogin(name.trim(), email.trim())
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("Sign In / Switch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
