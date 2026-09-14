package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.util.SoundManager
import com.example.viewmodel.CricketViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: CricketViewModel,
    onNavigateToLiveScoring: () -> Unit,
    onNavigateToScorecard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.dashboardSettings.collectAsState()
    val activeMatch by viewModel.activeMatch.collectAsState()
    val matches by viewModel.matches.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Dashboard Header Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(settings.theme.primaryHex))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CRICLIVE STADIUM DASHBOARD", color = Color(settings.theme.accentHex), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(settings.theme.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text(settings.theme.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { onNavigateToLiveScoring() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.SportsCricket, contentDescription = "Scoring", tint = Color.White)
                        }
                    }
                }
            }
        }

        // 2. Active Match Score Preview according to selected Card Presentation Mode
        activeMatch?.let { match ->
            if (settings.showHeroScorecard) {
                item {
                    Text("ACTIVE MATCH PRESENTATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    when (settings.cardMode) {
                        CardMode.BROADCAST_SCOREBUG -> BroadcastScorebugView(match, settings.theme, onNavigateToLiveScoring)
                        CardMode.DETAILED_STATS -> DetailedStatsView(match, settings.theme, onNavigateToLiveScoring)
                        CardMode.COMPACT_STRIP -> CompactStripView(match, settings.theme, onNavigateToLiveScoring)
                    }
                }
            }
        }

        // 3. Mini Fixtures Carousel (if enabled)
        if (settings.showMiniFixtures && matches.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FIXTURES CAROUSEL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("${matches.size} matches", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(matches) { m ->
                            val isSelected = m.id == activeMatch?.id
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable { viewModel.selectActiveMatch(m.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(m.tournament.take(18), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (m.status) {
                                                MatchStatus.LIVE -> Color.Red
                                                MatchStatus.COMPLETED -> Color(0xFF388E3C)
                                                MatchStatus.UPCOMING -> Color(0xFF1976D2)
                                            }
                                        ) {
                                            Text(m.status.displayName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("${m.teamA.shortCode} vs ${m.teamB.shortCode}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val inn = m.currentInnings
                                    if (inn != null) {
                                        Text("${m.battingTeam.shortCode}: ${inn.totalRuns}/${inn.wickets} (${inn.oversString} ov)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Text(m.venue.take(22), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Stadium Color Themes Selector
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Five Stadium Color Themes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text("Select high-contrast stadium aesthetics tailored for day/night match scoring.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StadiumTheme.values().forEach { th ->
                            val isSelected = settings.theme == th
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateTheme(th) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(th.primaryHex),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(th.accentHex),
                                                modifier = Modifier.size(10.dp)
                                            ) {}
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(th.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        Text(th.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Card Presentation Modes Selector
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewAgenda, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Card Presentation Modes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text("Alternate between Broadcast TV scorebug, Detailed stats, or Compact strip.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    CardMode.values().forEach { mode ->
                        val isSelected = settings.cardMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateCardMode(mode) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { viewModel.updateCardMode(mode) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(mode.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(mode.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // 6. Granular Widget Toggles
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom Dashboard Widgets", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text("Show or hide live widgets based on your scoring console workflow.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    WidgetToggleRow("Hero Scorecard Banner", settings.showHeroScorecard) { viewModel.toggleWidget("hero") }
                    WidgetToggleRow("Recent Deliveries Strip", settings.showRecentBalls) { viewModel.toggleWidget("recent") }
                    WidgetToggleRow("Partnership Meter", settings.showPartnershipMeter) { viewModel.toggleWidget("partner") }
                    WidgetToggleRow("Bowler Figures Widget", settings.showBowlerFigures) { viewModel.toggleWidget("bowler") }
                    WidgetToggleRow("Commentary Feed", settings.showCommentaryFeed) { viewModel.toggleWidget("commentary") }
                    WidgetToggleRow("Mini Fixtures Carousel", settings.showMiniFixtures) { viewModel.toggleWidget("fixtures") }
                }
            }
        }

        // 7. Integrated Audio Sound Engine Section
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
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stadium Sound Synthesis", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Switch(checked = settings.soundEnabled, onCheckedChange = { viewModel.toggleWidget("sound") })
                    }
                    Text("Synthesizes offline tone audio: bat cracks, boundary chimes, and umpire whistle.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { SoundManager.playBatCrack(true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Bat Crack", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { SoundManager.playBoundaryChime(true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Four Chime", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { SoundManager.playSixFanfare(true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Six Fanfare", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetToggleRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.5.sp)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun BroadcastScorebugView(match: CricketMatch, theme: StadiumTheme, onOpenScoring: () -> Unit) {
    val inn = match.currentInnings
    val primaryColor = Color(theme.primaryHex)
    val accentColor = Color(theme.accentHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenScoring() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${match.teamA.shortCode} vs ${match.teamB.shortCode}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${match.battingTeam.shortCode} ${inn?.totalRuns ?: 0}/${inn?.wickets ?: 0}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("${inn?.oversString ?: "0.0"} / ${match.oversLimit} Overs", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                val rr = String.format(Locale.US, "%.2f", inn?.runRate ?: 0.0)
                Text("CRR: $rr", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(
                    onClick = onOpenScoring,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Scorer Console", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailedStatsView(match: CricketMatch, theme: StadiumTheme, onOpenScoring: () -> Unit) {
    val inn = match.currentInnings
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenScoring() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${match.title} • ${match.venue}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text("${inn?.oversString ?: "0.0"} ov", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${match.battingTeam.name}: ${inn?.totalRuns ?: 0}/${inn?.wickets ?: 0}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                val rr = String.format(Locale.US, "%.2f", inn?.runRate ?: 0.0)
                Text("RR: $rr", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Extras: ${inn?.extras?.total ?: 0} (wd ${inn?.extras?.wides ?: 0}, nb ${inn?.extras?.noBalls ?: 0})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompactStripView(match: CricketMatch, theme: StadiumTheme, onOpenScoring: () -> Unit) {
    val inn = match.currentInnings
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenScoring() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${match.teamA.shortCode} v ${match.teamB.shortCode}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("${inn?.totalRuns ?: 0}/${inn?.wickets ?: 0} (${inn?.oversString ?: "0.0"} ov)", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
