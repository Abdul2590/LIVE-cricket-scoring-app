package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AndroidStatusBar
import com.example.ui.components.CelebrationOverlay
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CricketViewModel
import kotlinx.coroutines.launch

enum class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    SCORER("scorer", "Live Scorer", Icons.Default.SportsCricket),
    FIXTURES("fixtures", "Fixtures", Icons.Default.CalendarMonth),
    SCORECARD("scorecard", "Scorecard", Icons.Default.Assessment),
    TEAMS_CLOUD("teams", "Teams & Cloud", Icons.Default.CloudSync)
}

class MainActivity : ComponentActivity() {

    private val viewModel: CricketViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: CricketViewModel) {
    val dashboardSettings by viewModel.dashboardSettings.collectAsState()
    val celebration by viewModel.celebration.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val activeMatch by viewModel.activeMatch.collectAsState()

    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var isPhoneMockupFrameEnabled by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearStatusMessage()
            }
        }
    }

    val primaryThemeColor = Color(dashboardSettings.theme.primaryHex)
    val accentThemeColor = Color(dashboardSettings.theme.accentHex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isPhoneMockupFrameEnabled) Color(0xFF1E242B) else MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // App Frame Wrapper
        Box(
            modifier = if (isPhoneMockupFrameEnabled) {
                Modifier
                    .fillMaxHeight(0.96f)
                    .widthIn(max = 440.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(3.dp, Color(0xFF37474F), RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.background)
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column {
                        // Simulated Android Status Bar
                        AndroidStatusBar(
                            backgroundColor = primaryThemeColor,
                            contentColor = Color.White
                        )

                        // App Top Bar
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = accentThemeColor,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            "LIVE",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "CricLive",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = Color.White
                                        )
                                        activeMatch?.let { m ->
                                            Text(
                                                "${m.teamA.shortCode} v ${m.teamB.shortCode} • ${m.tournament.take(16)}",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                // Frame toggle button
                                IconButton(onClick = { isPhoneMockupFrameEnabled = !isPhoneMockupFrameEnabled }) {
                                    Icon(
                                        imageVector = if (isPhoneMockupFrameEnabled) Icons.Default.Fullscreen else Icons.Default.Smartphone,
                                        contentDescription = "Toggle Device Frame",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = primaryThemeColor
                            )
                        )
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        AppDestination.values().forEach { destination ->
                            val isSelected = currentDestination == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title
                                    )
                                },
                                label = {
                                    Text(
                                        destination.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = primaryThemeColor,
                                    indicatorColor = primaryThemeColor.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentDestination) {
                        AppDestination.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToLiveScoring = { currentDestination = AppDestination.SCORER },
                            onNavigateToScorecard = { currentDestination = AppDestination.SCORECARD }
                        )
                        AppDestination.SCORER -> LiveScoringScreen(
                            viewModel = viewModel,
                            onNavigateToScorecard = { currentDestination = AppDestination.SCORECARD }
                        )
                        AppDestination.FIXTURES -> FixturesAdminScreen(
                            viewModel = viewModel,
                            onNavigateToScoring = { currentDestination = AppDestination.SCORER }
                        )
                        AppDestination.SCORECARD -> ScorecardScreen(
                            viewModel = viewModel
                        )
                        AppDestination.TEAMS_CLOUD -> TeamsAndCloudScreen(
                            viewModel = viewModel
                        )
                    }

                    // Celebration Overlay (Confetti & Milestones)
                    CelebrationOverlay(
                        celebration = celebration,
                        onDismiss = { viewModel.dismissCelebration() }
                    )
                }
            }
        }
    }
}
