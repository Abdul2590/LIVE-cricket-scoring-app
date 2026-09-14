package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.viewmodel.CricketViewModel
import java.util.Locale

@Composable
fun ScorecardScreen(
    viewModel: CricketViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeMatch by viewModel.activeMatch.collectAsState()
    val match = activeMatch

    if (match == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No match selected.")
        }
        return
    }

    val inn = match.currentInnings
    val battingTeam = match.battingTeam
    val bowlingTeam = match.bowlingTeam

    var selectedTab by remember { mutableStateOf(0) } // 0: Scorecard, 1: Commentary, 2: Fall of Wickets

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // PDF Export Banner (Prominently as requested)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("OFFICIAL MATCH SCORECARD", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${match.teamA.shortCode} vs ${match.teamB.shortCode}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("${match.tournament} • ${match.venue}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.downloadPdfScorecard(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Match Summary Pill
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${battingTeam.name} 1st Innings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Toss: ${match.resultText}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${inn?.totalRuns ?: 0}/${inn?.wickets ?: 0}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                        val rr = String.format(Locale.US, "%.2f", inn?.runRate ?: 0.0)
                        Text("(${inn?.oversString ?: "0.0"} ov, CRR: $rr)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Tab Row: Full Scorecard, Commentary, Fall of Wickets
        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Scorecard") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Commentary") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("FOW & Info") })
            }
        }

        when (selectedTab) {
            0 -> {
                // Batting Table
                item {
                    Text("BATTING", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header Row
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Batter", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2f))
                                Text("R", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                                Text("B", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                                Text("4s", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                                Text("6s", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                                Text("SR", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            val stats = inn?.battingStats ?: emptyList()
                            stats.forEach { stat ->
                                val p = battingTeam.players.find { it.id == stat.playerId }
                                val pName = p?.name ?: "Batter"
                                val isNotOut = !stat.isOut
                                val sr = String.format(Locale.US, "%.1f", stat.strikeRate)

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$pName ${if (isNotOut && (stat.playerId == inn?.strikerId || stat.playerId == inn?.nonStrikerId)) "*" else ""}",
                                            fontWeight = if (isNotOut) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(2f)
                                        )
                                        Text("${stat.runs}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.6f))
                                        Text("${stat.balls}", fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                                        Text("${stat.fours}", fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                                        Text("${stat.sixes}", fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                                        Text(sr, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                    }
                                    val dismissalDesc = if (stat.isOut) {
                                        stat.dismissal?.let { "${it.type.displayName} ${it.fielderName?.let { f -> "c $f " } ?: ""}${it.bowlerId?.let { b -> "b ${bowlingTeam.players.find { pl -> pl.id == b }?.name ?: "bowler"}" } ?: ""}" } ?: "out"
                                    } else {
                                        "not out"
                                    }
                                    Text(
                                        text = dismissalDesc,
                                        fontSize = 10.5.sp,
                                        color = if (isNotOut) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                            }

                            // Extras row
                            val ex = inn?.extras
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Extras", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("${ex?.total ?: 0} (b ${ex?.byes ?: 0}, lb ${ex?.legByes ?: 0}, wd ${ex?.wides ?: 0}, nb ${ex?.noBalls ?: 0})", fontSize = 12.sp)
                            }

                            // Total row
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text("${inn?.totalRuns ?: 0}/${inn?.wickets ?: 0} (${inn?.oversString ?: "0.0"} Ov, RR ${String.format(Locale.US, "%.2f", inn?.runRate ?: 0.0)})", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Bowling Table
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("BOWLING", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Bowler", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2f))
                                Text("O", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                Text("M", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                                Text("R", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                Text("W", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                Text("Econ", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            val bStats = inn?.bowlingStats ?: emptyList()
                            bStats.forEach { bStat ->
                                val b = bowlingTeam.players.find { it.id == bStat.playerId }
                                val bName = b?.name ?: "Bowler"
                                val econ = String.format(Locale.US, "%.2f", bStat.economy)

                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(bName, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2f))
                                    Text(bStat.oversString, fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                    Text("${bStat.maidens}", fontSize = 12.sp, modifier = Modifier.weight(0.6f))
                                    Text("${bStat.runsConceded}", fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                    Text("${bStat.wickets}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828), modifier = Modifier.weight(0.7f))
                                    Text(econ, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                }
                                Divider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }

            1 -> {
                // Ball-by-ball Commentary Feed
                item {
                    Text("BALL-BY-BALL COMMENTARY", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }

                val deliveries = inn?.deliveries ?: emptyList()
                if (deliveries.isEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(10.dp)) {
                            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No deliveries recorded yet. Start scoring from the Scorer tab.", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(deliveries.reversed()) { delivery ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (delivery.isWicket) Color(0xFFC62828) else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = "${delivery.overNumber}.${delivery.ballNumberInOver}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (delivery.isWicket) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(delivery.commentary, fontSize = 12.5.sp)
                                    Text("Runs: ${delivery.runsBat} • Notation: [${delivery.shortNotation}]", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Fall of Wickets & Match Info
                item {
                    Text("FALL OF WICKETS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val fow = inn?.fallOfWickets ?: emptyList()
                            if (fow.isEmpty()) {
                                Text("No wickets have fallen yet in this innings.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                fow.forEach { wkt ->
                                    val outPlayer = battingTeam.players.find { it.id == wkt.playerId }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${wkt.score}-${wkt.wicketNumber} (${outPlayer?.name ?: "Batter"}, ${wkt.overBall} ov)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("MATCH DETAILS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailRow("Tournament", match.tournament)
                            DetailRow("Venue", match.venue)
                            DetailRow("Pitch Condition", match.pitchCondition)
                            DetailRow("Match Format", "${match.oversLimit} Overs (${match.matchType.displayName})")
                            DetailRow("Toss", match.resultText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
