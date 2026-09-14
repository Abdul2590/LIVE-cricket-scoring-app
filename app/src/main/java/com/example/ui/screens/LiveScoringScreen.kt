package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.viewmodel.CricketViewModel
import java.util.Locale

@Composable
fun LiveScoringScreen(
    viewModel: CricketViewModel,
    onNavigateToScorecard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMatch by viewModel.activeMatch.collectAsState()
    val dashboardSettings by viewModel.dashboardSettings.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()

    var showChangeBatsmanDialog by remember { mutableStateOf(false) }
    var showWicketDialog by remember { mutableStateOf(false) }
    var showNoBallDialog by remember { mutableStateOf(false) }
    var editingPlayerTarget by remember { mutableStateOf<Pair<String, Player>?>(null) } // teamId to Player
    var customExtrasDialog by remember { mutableStateOf(false) }
    var commentaryInput by remember { mutableStateOf("") }
    var showCommentaryField by remember { mutableStateOf(false) }

    val match = activeMatch
    if (match == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active match selected. Fix a fixture in Admin tab to begin scoring.")
        }
        return
    }

    val inn = match.currentInnings
    val battingTeam = match.battingTeam
    val bowlingTeam = match.bowlingTeam
    val striker = battingTeam.players.find { it.id == inn?.strikerId }
    val nonStriker = battingTeam.players.find { it.id == inn?.nonStrikerId }
    val bowler = bowlingTeam.players.find { it.id == inn?.currentBowlerId }
    val strikerStat = inn?.battingStats?.find { it.playerId == inn.strikerId }
    val nonStrikerStat = inn?.battingStats?.find { it.playerId == inn.nonStrikerId }
    val bowlerStat = inn?.bowlingStats?.find { it.playerId == inn.currentBowlerId }

    val isFreeHit = inn?.deliveries?.lastOrNull()?.extraType == ExtraType.NO_BALL

    val theme = dashboardSettings.theme
    val primaryColor = Color(theme.primaryHex)
    val accentColor = Color(theme.accentHex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Match Header & Live Broadcast Scorebug
        item {
            MatchScoreHeroCard(
                match = match,
                innings = inn,
                primaryColor = primaryColor,
                accentColor = accentColor,
                isFreeHit = isFreeHit,
                onViewFullScorecard = onNavigateToScorecard
            )
        }

        // 2. Crease Tracker (Striker, Non-Striker, Bowler)
        item {
            CreaseTrackerCard(
                striker = striker,
                nonStriker = nonStriker,
                bowler = bowler,
                strikerStat = strikerStat,
                nonStrikerStat = nonStrikerStat,
                bowlerStat = bowlerStat,
                primaryColor = primaryColor,
                accentColor = accentColor,
                onChangeBatsman = { showChangeBatsmanDialog = true },
                onRotateStrike = { viewModel.rotateStrike() },
                onEditPlayerName = { teamId, p -> editingPlayerTarget = Pair(teamId, p) }
            )
        }

        // 3. Quick Action Toolbar: Undo, Change Batsman, Change Bowler, Commentary Toggle
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prominent Undo Button as explicitly requested
                    Button(
                        onClick = { viewModel.undoLastBall() },
                        enabled = canUndo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canUndo) Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("UNDO BALL", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }

                    // Change Batsman button
                    OutlinedButton(
                        onClick = { showChangeBatsmanDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change Batsman", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Rotate Strike button
                    FilledTonalIconButton(
                        onClick = { viewModel.rotateStrike() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = "Rotate Strike", modifier = Modifier.size(18.dp))
                    }

                    // Commentary toggle button
                    FilledTonalIconButton(
                        onClick = { showCommentaryField = !showCommentaryField },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = "Ball Commentary", modifier = Modifier.size(18.dp))
                    }
                }

                if (showCommentaryField) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentaryInput,
                            onValueChange = { commentaryInput = it },
                            placeholder = { Text("Custom commentary for next ball (optional)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { commentaryInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 4. Over Deliveries Strip
        if (dashboardSettings.showRecentBalls && inn != null) {
            item {
                OverStripCard(deliveries = inn.recentBalls)
            }
        }

        // 5. Fast-Action Keypad: Runs, Only Wide & No-Ball, Wicket
        item {
            ScoringKeypad(
                onScoreRuns = { runs ->
                    viewModel.recordBall(
                        runsBat = runs,
                        extraType = ExtraType.NONE,
                        extraRuns = 0,
                        isWicket = false,
                        commentaryText = commentaryInput
                    )
                    commentaryInput = ""
                },
                onQuickWide = {
                    // Dedicated Wide option: +1 extra run, ball not counted
                    viewModel.recordBall(
                        runsBat = 0,
                        extraType = ExtraType.WIDE,
                        extraRuns = 1,
                        isWicket = false,
                        commentaryText = commentaryInput.ifBlank { "Wide ball outside off stump." }
                    )
                    commentaryInput = ""
                },
                onQuickNoBall = {
                    // Open dedicated No-Ball extra runs dialog
                    showNoBallDialog = true
                },
                onNoBallRuns = { runsBat ->
                    viewModel.recordBall(
                        runsBat = runsBat,
                        extraType = ExtraType.NO_BALL,
                        extraRuns = 1,
                        isWicket = false,
                        commentaryText = commentaryInput.ifBlank {
                            when (runsBat) {
                                6 -> "NO BALL & SIX! Bowler oversteps and is punished for a maximum! Free hit next!"
                                4 -> "NO BALL & FOUR! Smashed to the fence off the illegal delivery! Free hit next!"
                                1 -> "No ball! Single taken, strike rotates. Free hit signaled next ball."
                                0 -> "No ball! Bowler oversteps the line. 1 penalty run. Free hit next!"
                                else -> "No ball! $runsBat runs scored off the bat. Free hit next!"
                            }
                        }
                    )
                    commentaryInput = ""
                },
                onOpenExtrasModal = { customExtrasDialog = true },
                onOpenWicketModal = { showWicketDialog = true },
                primaryColor = primaryColor,
                accentColor = accentColor
            )
        }

        // 6. Active Partnership & Bowler Figures (if enabled in dashboard)
        if (dashboardSettings.showPartnershipMeter && inn != null) {
            item {
                PartnershipMeterCard(
                    striker = striker,
                    nonStriker = nonStriker,
                    strikerStat = strikerStat,
                    nonStrikerStat = nonStrikerStat
                )
            }
        }
    }

    // --- Dialogs ---

    if (showChangeBatsmanDialog && inn != null) {
        ChangeBatsmanDialog(
            battingSquad = battingTeam.players,
            bowlingSquad = bowlingTeam.players,
            currentStrikerId = inn.strikerId,
            currentNonStrikerId = inn.nonStrikerId,
            currentBowlerId = inn.currentBowlerId,
            onDismiss = { showChangeBatsmanDialog = false },
            onConfirm = { sId, nsId, bId ->
                viewModel.changeStrikerAndNonStriker(sId, nsId)
                if (bId != inn.currentBowlerId) viewModel.changeBowler(bId)
                showChangeBatsmanDialog = false
            }
        )
    }

    if (showWicketDialog) {
        WicketDismissalDialog(
            striker = striker,
            nonStriker = nonStriker,
            currentBowler = bowler,
            fieldingSquad = bowlingTeam.players,
            onDismiss = { showWicketDialog = false },
            onConfirm = { runs, dismissal ->
                viewModel.recordBall(
                    runsBat = runs,
                    extraType = ExtraType.NONE,
                    extraRuns = 0,
                    isWicket = true,
                    dismissal = dismissal,
                    commentaryText = commentaryInput
                )
                commentaryInput = ""
                showWicketDialog = false
            }
        )
    }

    editingPlayerTarget?.let { (teamId, player) ->
        EditPlayerNameDialog(
            initialName = player.name,
            onDismiss = { editingPlayerTarget = null },
            onSave = { newName ->
                viewModel.editPlayerName(teamId, player.id, newName)
                editingPlayerTarget = null
            }
        )
    }

    if (showNoBallDialog) {
        NoBallOptionsDialog(
            onDismiss = { showNoBallDialog = false },
            onConfirm = { runsBat, extraRuns, comm ->
                viewModel.recordBall(
                    runsBat = runsBat,
                    extraType = ExtraType.NO_BALL,
                    extraRuns = extraRuns,
                    isWicket = false,
                    commentaryText = comm.ifBlank { commentaryInput }
                )
                commentaryInput = ""
                showNoBallDialog = false
            }
        )
    }

    if (customExtrasDialog) {
        CustomExtrasSelectorDialog(
            onDismiss = { customExtrasDialog = false },
            onSelect = { extraType, extraRuns, runsBat ->
                viewModel.recordBall(
                    runsBat = runsBat,
                    extraType = extraType,
                    extraRuns = extraRuns,
                    isWicket = false,
                    commentaryText = commentaryInput
                )
                commentaryInput = ""
                customExtrasDialog = false
            }
        )
    }
}

// --- Composable Sub-Components ---

@Composable
private fun MatchScoreHeroCard(
    match: CricketMatch,
    innings: Innings?,
    primaryColor: Color,
    accentColor: Color,
    isFreeHit: Boolean,
    onViewFullScorecard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(primaryColor, primaryColor.copy(alpha = 0.85f))
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = match.tournament.uppercase(),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${match.teamA.shortCode} vs ${match.teamB.shortCode}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isFreeHit) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFD600),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    "FREE HIT!",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Red,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "LIVE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        IconButton(
                            onClick = onViewFullScorecard,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = "Scorecard", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Score Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        val runs = innings?.totalRuns ?: 0
                        val wkts = innings?.wickets ?: 0
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$runs",
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "/$wkts",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "${match.battingTeam.name} Innings",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val oversStr = innings?.oversString ?: "0.0"
                        Text(
                            text = "$oversStr / ${match.oversLimit} Ov",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val rr = String.format(Locale.US, "%.2f", innings?.runRate ?: 0.0)
                        Text(
                            text = "CRR: $rr",
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (match.resultText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = match.resultText,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CreaseTrackerCard(
    striker: Player?,
    nonStriker: Player?,
    bowler: Player?,
    strikerStat: BattingStat?,
    nonStrikerStat: BattingStat?,
    bowlerStat: BowlingStat?,
    primaryColor: Color,
    accentColor: Color,
    onChangeBatsman: () -> Unit,
    onRotateStrike: () -> Unit,
    onEditPlayerName: (teamId: String, player: Player) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LIVE CREASE TRACKER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = primaryColor
                )
                Text(
                    "Tap name to edit",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Striker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryColor.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = primaryColor,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.clickable {
                        striker?.let { onEditPlayerName("batting", it) }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${striker?.name ?: "Striker"} *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit name", modifier = Modifier.size(12.dp), tint = primaryColor)
                        }
                        val sr = String.format(Locale.US, "%.1f", strikerStat?.strikeRate ?: 0.0)
                        Text(
                            text = "SR: $sr • 4s: ${strikerStat?.fours ?: 0} • 6s: ${strikerStat?.sixes ?: 0}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${strikerStat?.runs ?: 0}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = primaryColor
                    )
                    Text(
                        text = " (${strikerStat?.balls ?: 0})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Non-Striker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.clickable {
                        nonStriker?.let { onEditPlayerName("batting", it) }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = nonStriker?.name ?: "Non-Striker",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit name", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                        val sr = String.format(Locale.US, "%.1f", nonStrikerStat?.strikeRate ?: 0.0)
                        Text(
                            text = "SR: $sr • 4s: ${nonStrikerStat?.fours ?: 0} • 6s: ${nonStrikerStat?.sixes ?: 0}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${nonStrikerStat?.runs ?: 0}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = " (${nonStrikerStat?.balls ?: 0})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp))

            // Bowler Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsBaseball, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.clickable {
                        bowler?.let { onEditPlayerName("bowling", it) }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bowler?.name ?: "Current Bowler",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit bowler", modifier = Modifier.size(12.dp), tint = accentColor)
                        }
                        val econ = String.format(Locale.US, "%.2f", bowlerStat?.economy ?: 0.0)
                        Text(
                            text = "Econ: $econ • Wides: ${bowlerStat?.wides ?: 0} • NB: ${bowlerStat?.noBalls ?: 0}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${bowlerStat?.wickets ?: 0} - ${bowlerStat?.runsConceded ?: 0}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = "${bowlerStat?.oversString ?: "0.0"} overs",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OverStripCard(deliveries: List<BallDelivery>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("THIS OVER / RECENT DELIVERIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            if (deliveries.isEmpty()) {
                Text("No balls bowled yet in this innings.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    deliveries.forEach { ball ->
                        val (bg, fg) = when {
                            ball.isWicket -> Pair(Color(0xFFC62828), Color.White)
                            ball.runsBat == 6 -> Pair(Color(0xFFE65100), Color.White)
                            ball.runsBat == 4 -> Pair(Color(0xFF2E7D32), Color.White)
                            ball.extraType == ExtraType.WIDE || ball.extraType == ExtraType.NO_BALL -> Pair(Color(0xFFFBC02D), Color.Black)
                            ball.runsBat == 0 -> Pair(Color(0xFFECEFF1), Color.Black)
                            else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            shape = CircleShape,
                            color = bg,
                            modifier = Modifier.size(36.dp),
                            tonalElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = ball.shortNotation,
                                    color = fg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoringKeypad(
    onScoreRuns: (Int) -> Unit,
    onQuickWide: () -> Unit,
    onQuickNoBall: () -> Unit,
    onNoBallRuns: (Int) -> Unit,
    onOpenExtrasModal: () -> Unit,
    onOpenWicketModal: () -> Unit,
    primaryColor: Color,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("FAST-ACTION SCORING KEYPAD", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = primaryColor)
            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: 0, 1, 2, 3
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeypadRunButton(label = "0", subtitle = "Dot", modifier = Modifier.weight(1f)) { onScoreRuns(0) }
                KeypadRunButton(label = "1", subtitle = "Single", modifier = Modifier.weight(1f)) { onScoreRuns(1) }
                KeypadRunButton(label = "2", subtitle = "Double", modifier = Modifier.weight(1f)) { onScoreRuns(2) }
                KeypadRunButton(label = "3", subtitle = "Three", modifier = Modifier.weight(1f)) { onScoreRuns(3) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: 4 (Four), 6 (Six), Wicket
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Four
                Button(
                    onClick = { onScoreRuns(4) },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("4", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        Text("FOUR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                    }
                }

                // Six
                Button(
                    onClick = { onScoreRuns(6) },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("6", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        Text("SIX", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                    }
                }

                // Wicket
                Button(
                    onClick = onOpenWicketModal,
                    modifier = Modifier.weight(1.2f).height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WICKET", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        Text("Dismissal Flow", fontSize = 9.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Dedicated Wide & No-Ball Options (As requested: "add options only wide and no ball")
            Text("EXTRAS (FAST ACCESS)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Wide Button
                OutlinedButton(
                    onClick = onQuickWide,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF57F17))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WD", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("+1 Wide", fontSize = 9.sp)
                    }
                }

                // No Ball Button (Full Modal with bat runs, byes, leg-byes)
                OutlinedButton(
                    onClick = onQuickNoBall,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD84315))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NB...", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("No Ball Options", fontSize = 8.5.sp)
                    }
                }

                // More Extras (Byes, Leg Byes, Wides with extra runs)
                FilledTonalButton(
                    onClick = onOpenExtrasModal,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("More Extras", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Bye / LB / runs", fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("NO BALL EXTRA RUNS (1-TAP):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD84315))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onNoBallRuns(0) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD84315))
                ) {
                    Text("NB+0 (1)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onNoBallRuns(1) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD84315))
                ) {
                    Text("NB+1 (2)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onNoBallRuns(2) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD84315))
                ) {
                    Text("NB+2 (3)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onNoBallRuns(4) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("NB+4 FOUR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onNoBallRuns(6) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("NB+6 SIX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun KeypadRunButton(
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PartnershipMeterCard(
    striker: Player?,
    nonStriker: Player?,
    strikerStat: BattingStat?,
    nonStrikerStat: BattingStat?
) {
    val sRuns = strikerStat?.runs ?: 0
    val nsRuns = nonStrikerStat?.runs ?: 0
    val totalPartnership = sRuns + nsRuns
    val sBalls = strikerStat?.balls ?: 0
    val nsBalls = nonStrikerStat?.balls ?: 0
    val totalBalls = sBalls + nsBalls

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CURRENT PARTNERSHIP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$totalPartnership runs ($totalBalls balls)", fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Visual bar
            val strikerShare = if (totalPartnership > 0) sRuns.toFloat() / totalPartnership else 0.5f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(strikerShare.coerceIn(0.05f, 0.95f))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight((1f - strikerShare).coerceIn(0.05f, 0.95f))
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${striker?.name ?: "Striker"}: $sRuns ($sBalls)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("${nonStriker?.name ?: "Non-Striker"}: $nsRuns ($nsBalls)", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CustomExtrasSelectorDialog(
    onDismiss: () -> Unit,
    onSelect: (type: ExtraType, extraRuns: Int, runsBat: Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(ExtraType.WIDE) }
    var extraRuns by remember { mutableStateOf(1) }
    var batRuns by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Extras Delivery", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Extra Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(ExtraType.WIDE, ExtraType.NO_BALL, ExtraType.BYE, ExtraType.LEG_BYE).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                if (selectedType == ExtraType.WIDE) {
                    Text("Total Wide Runs (1 penalty + overthrows):", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 5).forEach { r ->
                            FilterChip(selected = extraRuns == r, onClick = { extraRuns = r }, label = { Text("$r WD") })
                        }
                    }
                } else if (selectedType == ExtraType.NO_BALL) {
                    Text("Runs scored off the bat (plus 1 NB):", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 1, 2, 4, 6).forEach { r ->
                            FilterChip(selected = batRuns == r, onClick = { batRuns = r }, label = { Text("$r runs") })
                        }
                    }
                } else {
                    Text("Byes / Leg Byes completed:", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 4).forEach { r ->
                            FilterChip(selected = extraRuns == r, onClick = { extraRuns = r }, label = { Text("$r runs") })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSelect(selectedType, extraRuns, batRuns)
            }) {
                Text("Confirm Extra")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
