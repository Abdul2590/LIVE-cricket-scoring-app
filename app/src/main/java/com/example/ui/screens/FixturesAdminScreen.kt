package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.PlayerDetailsDialog
import com.example.viewmodel.CricketViewModel

@Composable
fun FixturesAdminScreen(
    viewModel: CricketViewModel,
    onNavigateToScoring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matches by viewModel.matches.collectAsState()
    val activeMatch by viewModel.activeMatch.collectAsState()
    val customTeams by viewModel.customTeams.collectAsState()

    var statusFilter by remember { mutableStateOf<MatchStatus?>(null) }
    var showCreateFixtureDialog by remember { mutableStateOf(false) }
    var matchToDelete by remember { mutableStateOf<CricketMatch?>(null) }
    var showClearCompletedDialog by remember { mutableStateOf(false) }

    val filteredMatches = remember(matches, statusFilter) {
        if (statusFilter == null) matches else matches.filter { it.status == statusFilter }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateFixtureDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Fix Match") },
                text = { Text("Fix New Match") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Admin Fixture Suite", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Schedule fixtures, configure overs format, customize player details, and switch active matches.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tournament Presets for 1-Tap Fast Testing
            item {
                Text("ONE-TAP TOURNAMENT PRESETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetButton(title = "ICC T20 Final", subtitle = "IND vs AUS", modifier = Modifier.weight(1f)) {
                        matches.find { it.id == "match_icc_final" }?.let {
                            viewModel.selectActiveMatch(it.id)
                            onNavigateToScoring()
                        }
                    }
                    PresetButton(title = "IPL Derby", subtitle = "MUM vs CHN", modifier = Modifier.weight(1f)) {
                        matches.find { it.id == "match_ipl_derby" }?.let {
                            viewModel.selectActiveMatch(it.id)
                        }
                    }
                    PresetButton(title = "T10 Blitz", subtitle = "LAH vs PSH", modifier = Modifier.weight(1f)) {
                        matches.find { it.id == "match_t10_blitz" }?.let {
                            viewModel.selectActiveMatch(it.id)
                        }
                    }
                }
            }

            // Filter Chips (All, Live, Upcoming, Completed)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { statusFilter = null },
                            label = { Text("All (${matches.size})") }
                        )
                        FilterChip(
                            selected = statusFilter == MatchStatus.LIVE,
                            onClick = { statusFilter = MatchStatus.LIVE },
                            label = { Text("Live") }
                        )
                        FilterChip(
                            selected = statusFilter == MatchStatus.UPCOMING,
                            onClick = { statusFilter = MatchStatus.UPCOMING },
                            label = { Text("Upcoming") }
                        )
                        FilterChip(
                            selected = statusFilter == MatchStatus.COMPLETED,
                            onClick = { statusFilter = MatchStatus.COMPLETED },
                            label = { Text("Completed") }
                        )
                    }
                    if (matches.any { it.status == MatchStatus.COMPLETED }) {
                        TextButton(
                            onClick = { showClearCompletedDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Match List Items
            items(filteredMatches) { m ->
                val isActive = m.id == activeMatch?.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectActiveMatch(m.id)
                            if (m.status == MatchStatus.LIVE) onNavigateToScoring()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(m.tournament, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (m.status) {
                                        MatchStatus.LIVE -> Color.Red
                                        MatchStatus.COMPLETED -> Color(0xFF2E7D32)
                                        MatchStatus.UPCOMING -> Color(0xFF1565C0)
                                    }
                                ) {
                                    Text(
                                        m.status.displayName,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { matchToDelete = m },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Fixture",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${m.teamA.name} vs ${m.teamB.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${m.venue} • ${m.oversLimit} Overs Format", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isActive) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("ACTIVE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }

                        val inn = m.currentInnings
                        if (inn != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${m.battingTeam.shortCode}: ${inn.totalRuns}/${inn.wickets} (${inn.oversString} ov)", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text("CRR: ${String.format("%.2f", inn.runRate)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFixtureDialog) {
        CreateFixtureDialog(
            customTeams = customTeams,
            onDismiss = { showCreateFixtureDialog = false },
            onSaveFixture = { title, tour, venue, teamA, teamB, type, customOvers, pitch, tossWin, tossDec, openingStriker, openingNonStriker, openingBowler ->
                viewModel.createFixture(
                    title = title,
                    tournament = tour,
                    venue = venue,
                    teamA = teamA,
                    teamB = teamB,
                    matchType = type,
                    customOvers = customOvers,
                    pitchCondition = pitch,
                    tossWinnerId = tossWin,
                    tossDecision = tossDec,
                    openingStrikerId = openingStriker,
                    openingNonStrikerId = openingNonStriker,
                    openingBowlerId = openingBowler
                )
                showCreateFixtureDialog = false
                onNavigateToScoring()
            }
        )
    }

    matchToDelete?.let { m ->
        ConfirmDeleteDialog(
            title = "Delete Fixture?",
            message = "Are you sure you want to delete '${m.teamA.name} vs ${m.teamB.name}' (${m.tournament})? All associated scorecard and commentary data will be permanently removed.",
            confirmButtonText = "Delete Fixture",
            onDismiss = { matchToDelete = null },
            onConfirm = {
                viewModel.deleteFixture(m.id)
                matchToDelete = null
            }
        )
    }

    if (showClearCompletedDialog) {
        ConfirmDeleteDialog(
            title = "Clear Completed Matches?",
            message = "Are you sure you want to delete all completed matches from your fixtures list?",
            confirmButtonText = "Clear All Completed",
            onDismiss = { showClearCompletedDialog = false },
            onConfirm = {
                viewModel.clearCompletedFixtures()
                showClearCompletedDialog = false
            }
        )
    }
}

@Composable
private fun PresetButton(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreateFixtureDialog(
    customTeams: List<Team>,
    onDismiss: () -> Unit,
    onSaveFixture: (
        title: String,
        tournament: String,
        venue: String,
        teamA: Team,
        teamB: Team,
        matchType: MatchType,
        customOvers: Int,
        pitchCondition: String,
        tossWinnerId: String,
        tossDecision: TossDecision,
        openingStrikerId: String,
        openingNonStrikerId: String,
        openingBowlerId: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("Club Derby Clash") }
    var tournament by remember { mutableStateOf("Premier Cricket League 2026") }
    var venue by remember { mutableStateOf("National Cricket Stadium") }
    var matchType by remember { mutableStateOf(MatchType.T20) }
    var customOvers by remember { mutableStateOf("5") }
    var pitchCondition by remember { mutableStateOf("Dry Pitch (True Bounce)") }

    // Teams pool: custom teams + default standard teams
    val availableTeams = remember(customTeams) {
        if (customTeams.size >= 2) customTeams else customTeams + listOf(
            Team("std_1", "India", "IND", 0xFF1976D2),
            Team("std_2", "Australia", "AUS", 0xFFFBC02D)
        )
    }

    var selectedTeamA by remember { mutableStateOf(availableTeams.getOrNull(0) ?: Team(name = "Team Alpha", shortCode = "ALP")) }
    var selectedTeamB by remember { mutableStateOf(availableTeams.getOrNull(1) ?: Team(name = "Team Beta", shortCode = "BET")) }

    var tossWinnerId by remember { mutableStateOf(selectedTeamA.id) }
    var tossDecision by remember { mutableStateOf(TossDecision.BAT) }

    val battingTeam = remember(selectedTeamA, selectedTeamB, tossWinnerId, tossDecision) {
        if (tossWinnerId == selectedTeamA.id) {
            if (tossDecision == TossDecision.BAT) selectedTeamA else selectedTeamB
        } else {
            if (tossDecision == TossDecision.BAT) selectedTeamB else selectedTeamA
        }
    }
    val bowlingTeam = remember(selectedTeamA, selectedTeamB, tossWinnerId, tossDecision) {
        if (battingTeam.id == selectedTeamA.id) selectedTeamB else selectedTeamA
    }

    var selectedStrikerId by remember(battingTeam.id) {
        mutableStateOf(battingTeam.players.getOrNull(0)?.id ?: "")
    }
    var selectedNonStrikerId by remember(battingTeam.id) {
        mutableStateOf(battingTeam.players.getOrNull(1)?.id ?: "")
    }
    var selectedOpeningBowlerId by remember(bowlingTeam.id) {
        val b = bowlingTeam.players.find { it.role == PlayerRole.BOWLER }
            ?: bowlingTeam.players.find { it.role == PlayerRole.ALL_ROUNDER }
            ?: bowlingTeam.players.getOrNull(10)
            ?: bowlingTeam.players.getOrNull(0)
        mutableStateOf(b?.id ?: "")
    }

    // Editing player details while fixing the match
    var editingPlayerByTeam by remember { mutableStateOf<Pair<Boolean, Player>?>(null) } // true: TeamA, false: TeamB

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsCricket, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Schedule / Fix Match", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Match Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tournament,
                    onValueChange = { tournament = it },
                    label = { Text("Tournament / Series") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Stadium") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Match Overs Format", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MatchType.values().forEach { mt ->
                        FilterChip(
                            selected = matchType == mt,
                            onClick = {
                                matchType = mt
                                if (mt != MatchType.CUSTOM) {
                                    customOvers = mt.defaultOvers.toString()
                                }
                            },
                            label = { Text(mt.displayName, fontSize = 11.5.sp) }
                        )
                    }
                }

                if (matchType == MatchType.CUSTOM) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Custom Overs Preset / Quick Select:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(2, 3, 5, 6, 8, 10, 12, 15, 20, 25, 30, 40, 50).forEach { ov ->
                                SuggestionChip(
                                    onClick = { customOvers = ov.toString() },
                                    label = { Text("${ov} ov", fontSize = 11.sp, fontWeight = if (customOvers == ov.toString()) FontWeight.Bold else FontWeight.Normal) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (customOvers == ov.toString()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                        OutlinedTextField(
                            value = customOvers,
                            onValueChange = { customOvers = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("Exact Custom Overs (e.g. 5, 10, 15, 25)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Divider()

                // Team Selection
                Text("Select Teams (Custom or Presets)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Home Team (Team A):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableTeams.forEach { t ->
                        FilterChip(
                            selected = selectedTeamA.id == t.id,
                            onClick = {
                                selectedTeamA = t
                                tossWinnerId = t.id
                            },
                            label = { Text(t.shortCode) }
                        )
                    }
                }

                Text("Away Team (Team B):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableTeams.forEach { t ->
                        FilterChip(
                            selected = selectedTeamB.id == t.id,
                            onClick = { selectedTeamB = t },
                            label = { Text(t.shortCode) },
                            enabled = selectedTeamA.id != t.id
                        )
                    }
                }

                Divider()

                // "add players details options while fixing the match"
                Text("Player Details & Squad Configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Tap any squad member to configure Role, Batting Hand, or Bowling Style:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Team A Players Preview
                Text("${selectedTeamA.name} Squad (${selectedTeamA.players.size}):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                selectedTeamA.players.take(4).forEach { p ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingPlayerByTeam = Pair(true, p) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${p.role.displayName} • ${p.battingStyle.displayName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Team B Players Preview
                Text("${selectedTeamB.name} Squad (${selectedTeamB.players.size}):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                selectedTeamB.players.take(4).forEach { p ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingPlayerByTeam = Pair(false, p) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${p.role.displayName} • ${p.bowlingStyle.displayName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                Divider()

                // Toss result
                Text("Toss Result", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tossWinnerId == selectedTeamA.id,
                        onClick = { tossWinnerId = selectedTeamA.id },
                        label = { Text("${selectedTeamA.shortCode} won toss") }
                    )
                    FilterChip(
                        selected = tossWinnerId == selectedTeamB.id,
                        onClick = { tossWinnerId = selectedTeamB.id },
                        label = { Text("${selectedTeamB.shortCode} won toss") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tossDecision == TossDecision.BAT,
                        onClick = { tossDecision = TossDecision.BAT },
                        label = { Text("Elected to BAT") }
                    )
                    FilterChip(
                        selected = tossDecision == TossDecision.BOWL,
                        onClick = { tossDecision = TossDecision.BOWL },
                        label = { Text("Elected to BOWL") }
                    )
                }
                Divider()

                // Opening Lineup & Bowler Selection before start match
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsBaseball, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Opening Bowler (Over 1)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Fielding: ${bowlingTeam.name} • Tap bowler to bowl first over:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            bowlingTeam.players.forEach { p ->
                                val isSelected = selectedOpeningBowlerId == p.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedOpeningBowlerId = p.id }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedOpeningBowlerId = p.id },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color.White,
                                                    unselectedColor = MaterialTheme.colorScheme.outline
                                                ),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                p.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.5.sp,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            "${p.role.displayName} • ${p.bowlingStyle.displayName}",
                                            fontSize = 10.5.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Divider()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsCricket, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Opening Batsmen (${battingTeam.name})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        // Striker
                        Text("Striker (*):", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            battingTeam.players.forEach { p ->
                                FilterChip(
                                    selected = selectedStrikerId == p.id,
                                    onClick = {
                                        selectedStrikerId = p.id
                                        if (selectedNonStrikerId == p.id) {
                                            selectedNonStrikerId = battingTeam.players.firstOrNull { it.id != p.id }?.id ?: ""
                                        }
                                    },
                                    label = { Text(p.name, fontSize = 11.5.sp) }
                                )
                            }
                        }

                        // Non-Striker
                        Text("Non-Striker:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            battingTeam.players.filter { it.id != selectedStrikerId }.forEach { p ->
                                FilterChip(
                                    selected = selectedNonStrikerId == p.id,
                                    onClick = { selectedNonStrikerId = p.id },
                                    label = { Text(p.name, fontSize = 11.5.sp) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveFixture(
                        title.trim(),
                        tournament.trim(),
                        venue.trim(),
                        selectedTeamA,
                        selectedTeamB,
                        matchType,
                        customOvers.toIntOrNull() ?: 5,
                        pitchCondition,
                        tossWinnerId,
                        tossDecision,
                        selectedStrikerId,
                        selectedNonStrikerId,
                        selectedOpeningBowlerId
                    )
                },
                enabled = title.isNotBlank() && selectedTeamA.id != selectedTeamB.id
            ) {
                Text("Schedule & Start Match")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Player Details Editor dialog triggered from within fixture scheduling
    editingPlayerByTeam?.let { (isTeamA, player) ->
        PlayerDetailsDialog(
            player = player,
            onDismiss = { editingPlayerByTeam = null },
            onSave = { updated ->
                if (isTeamA) {
                    val updatedPlayers = selectedTeamA.players.map { if (it.id == updated.id) updated else it }
                    selectedTeamA = selectedTeamA.copy(players = updatedPlayers)
                } else {
                    val updatedPlayers = selectedTeamB.players.map { if (it.id == updated.id) updated else it }
                    selectedTeamB = selectedTeamB.copy(players = updatedPlayers)
                }
                editingPlayerByTeam = null
            }
        )
    }
}
