package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.MatchPerformance
import com.example.data.Player
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Avatar color preset selection
val AvatarColors = listOf(
    TurfGreenClassic,
    BallRedPrimary,
    WicketGold,
    Color(0xFF2196F3), // Athletic blue
    Color(0xFF9C27B0), // Purple royalty
    Color(0xFFE65100)  // Flame orange
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CricketDashboard(
    viewModel: CricketViewModel,
    modifier: Modifier = Modifier
) {
    // UI state streams from ViewModel
    val currentTab by viewModel.activeTab.collectAsState()
    val selectedPlayer by viewModel.selectedPlayer.collectAsState()
    val players by viewModel.playersFlow.collectAsState(initial = emptyList())
    val allPerformances by viewModel.performancesFlow.collectAsState(initial = emptyList())
    val selectedPlayerPerformances by viewModel.selectedPlayerPerformances.collectAsState()

    // Dialog trigger states
    var showEditPlayerDialog by remember { mutableStateOf(false) }
    var showAddPerformanceDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_scaffold"),
        floatingActionButton = {
            if (selectedPlayer != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddPerformanceDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_performance_fab"),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Match Stats") },
                    text = { Text("Log Inning", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // App Hero / Title
            CricketHeroSection(
                onResetSelection = { },
                selectedPlayerName = null
            )

            // Tabs Layout
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[currentTab])
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    text = { 
                        Text(
                            text = "My Profile", 
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (currentTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (currentTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        ) 
                    },
                    icon = { 
                        Icon(
                            imageVector = Icons.Default.Person, 
                            contentDescription = "My Profile tab",
                            tint = if (currentTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        ) 
                    },
                    modifier = Modifier.testTag("player_list_tab")
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    text = { 
                        Text(
                            text = "Match Innings", 
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (currentTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (currentTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        ) 
                    },
                    icon = { 
                        Icon(
                            imageVector = Icons.Default.List, 
                            contentDescription = "Match logs tab",
                            tint = if (currentTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        ) 
                    },
                    modifier = Modifier.testTag("match_log_tab")
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { viewModel.setActiveTab(2) },
                    text = { 
                        Text(
                            text = "Visual Insights", 
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (currentTab == 2) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (currentTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        ) 
                    },
                    icon = { 
                        Icon(
                            imageVector = Icons.Default.Star, 
                            contentDescription = "Charts tab",
                            tint = if (currentTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        ) 
                    },
                    modifier = Modifier.testTag("visual_insights_tab")
                )
            }

            // Tab Views Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> PlayersTabContent(
                            player = selectedPlayer,
                            onEditProfile = { showEditPlayerDialog = true },
                            viewModel = viewModel
                        )
                        1 -> MatchLogTabContent(
                            allPerformances = allPerformances,
                            selectedPlayer = selectedPlayer,
                            playerIdToName = players.associate { it.id to it.name },
                            playerIdToColor = players.associate { it.id to it.avatarColorIndex },
                            onDeletePerformance = { viewModel.deletePerformance(it) }
                        )
                        2 -> InsightsTabContent(
                            selectedPlayer = selectedPlayer,
                            performances = if (selectedPlayer != null) selectedPlayerPerformances else allPerformances,
                            playerName = selectedPlayer?.name ?: "All Team Players",
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Modal forms implementation
    if (showEditPlayerDialog) {
        selectedPlayer?.let { player ->
            EditPlayerDialog(
                player = player,
                onDismiss = { showEditPlayerDialog = false },
                onConfirm = { name, role, team, jersey, colorIndex ->
                    viewModel.updatePlayer(player.id, name, role, team, jersey, colorIndex)
                    showEditPlayerDialog = false
                }
            )
        }
    }

    if (showAddPerformanceDialog) {
        AddPerformanceDialog(
            players = players,
            selectedPlayer = selectedPlayer,
            onDismiss = { showAddPerformanceDialog = false },
            onConfirm = { playerId, opponent, format, ageGroup, didBat, runs, balls, isNotOut, fours, sixes, didBowl, overs, runsCon, wickets, maidens ->
                viewModel.addMatchPerformance(
                    playerId, opponent, format, didBat, runs, balls, isNotOut, fours, sixes,
                    didBowl, overs, runsCon, wickets, maidens, ageGroup
                )
                showAddPerformanceDialog = false
            }
        )
    }
}

// --- Top Title / Hero header component ---
@Composable
fun CricketHeroSection(
    onResetSelection: () -> Unit,
    selectedPlayerName: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Draw outfield ground lawn stripe pattern
                drawRect(color = TurfGreenPrimary)
                
                val widthPx = size.width
                val heightPx = size.height
                val stripeWidth = 55.dp.toPx()
                
                var xOffset = -heightPx
                while (xOffset < widthPx) {
                    val path = Path().apply {
                        moveTo(xOffset, 0f)
                        lineTo(xOffset + stripeWidth, 0f)
                        lineTo(xOffset + stripeWidth + heightPx, heightPx)
                        lineTo(xOffset + heightPx, heightPx)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = TurfGreenClassic.copy(alpha = 0.45f)
                    )
                    xOffset += stripeWidth * 2f
                }

                // Smooth darkening shadow gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
            }
            .statusBarsPadding()
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Cricket Stats",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Real-time batting, bowling, matches & averages tracker",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TurfGreenSoft,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // App symbol - beautifully crafted red leather cricket ball with white seam stitch
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(BallRedClassic)
                        .padding(4.dp)
                        .drawBehind {
                            // Gloss highlight shininess
                            drawCircle(
                                color = Color.White.copy(alpha = 0.18f),
                                radius = size.minDimension / 2.4f,
                                center = Offset(size.width * 0.35f, size.height * 0.35f)
                            )
                            // White stitched leather ball seam line
                            val seamPath = Path().apply {
                                moveTo(size.width / 2f, 0f)
                                cubicTo(
                                    size.width / 2f - 4.dp.toPx(), size.height * 0.3f,
                                    size.width / 2f - 4.dp.toPx(), size.height * 0.7f,
                                    size.width / 2f, size.height
                                )
                            }
                            drawPath(
                                path = seamPath,
                                color = Color.White.copy(alpha = 0.85f),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Icon badge",
                        tint = WicketGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Interactive Breadcrumb showing focus context
            AnimatedVisibility(
                visible = selectedPlayerName != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onResetSelection() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Overall Team Dashboard",
                        tint = WicketGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filtering: $selectedPlayerName (Click to reset)",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

// --- Horizontal quick selector of players to change tracking context ---
@Composable
fun HorizontalPlayerSelector(
    players: List<Player>,
    selectedPlayer: Player?,
    onPlayerSelect: (Player?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Select Stat Focus:",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // First item is always the global team overview selector
            item {
                InputChip(
                    selected = selectedPlayer == null,
                    onClick = { onPlayerSelect(null) },
                    label = { Text("Team Overview", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Global stats representation",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("team_overview_chip")
                )
            }

            items(players) { player ->
                InputChip(
                    selected = selectedPlayer?.id == player.id,
                    onClick = { onPlayerSelect(player) },
                    label = { Text(player.name) },
                    leadingIcon = {
                        val colorIdx = ((player.avatarColorIndex % AvatarColors.size) + AvatarColors.size) % AvatarColors.size
                        val color = AvatarColors[colorIdx]
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(color)
                        ) {
                            Text(
                                text = player.name.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("player_chip_${player.id}")
                )
            }
        }
    }
}

// --- Tab 0 View: Core Player Profile & Detailed Career Highlights ---
@Composable
fun PlayersTabContent(
    player: Player?,
    onEditProfile: () -> Unit,
    viewModel: CricketViewModel
) {
    if (player == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val playerPerfFlow = remember(player.id) { viewModel.getPerformancesForPlayer(player.id) }
    val playerPerf: List<MatchPerformance> by playerPerfFlow.collectAsState(initial = emptyList())
    val careerStats = viewModel.computeStats(playerPerf)
    val avatarColor = AvatarColors[player.avatarColorIndex % AvatarColors.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Large Polished Profile Header Card ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile large circle avatar
                    Box(modifier = Modifier.size(72.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(avatarColor, avatarColor.copy(alpha = 0.7f))
                                    )
                                )
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                        }
                        // Subtitle jersey overlay badge
                        if (player.jerseyNumber.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(WicketGold)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.jerseyNumber,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TurfGreenPrimary
                                    )
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = player.role,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = player.team,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onEditProfile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit profile settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Edit Profile & Theme", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 2. Highlights Stat Overviews Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatHeaderCard(label = "Matches", value = "${careerStats.totalMatches}", activeColor = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            StatHeaderCard(label = "Runs Scored", value = "${careerStats.totalRuns}", activeColor = TurfGreenClassic, modifier = Modifier.weight(1f))
            StatHeaderCard(label = "Wickets Taken", value = "${careerStats.totalWickets}", activeColor = BallRedClassic, modifier = Modifier.weight(1f))
        }

        // --- 3. Complete Batting Statistics Section ---
        Text(
            text = "BATTING PROFILE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = TurfGreenClassic,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricBlock(label = "Bat Innings", value = "${careerStats.battingInnings}", modifier = Modifier.weight(1f))
                    MetricBlock(label = "Average", value = careerStats.battingAvg, valueColor = TurfGreenClassic, modifier = Modifier.weight(1f))
                    MetricBlock(label = "Strike Rate", value = careerStats.battingStrikeRate, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricBlock(label = "Highest", value = careerStats.highestScoreFormatted, modifier = Modifier.weight(1f))
                    MetricBlock(label = "Fours (4s)", value = "${careerStats.totalFours}", modifier = Modifier.weight(1f))
                    MetricBlock(label = "Sixes (6s)", value = "${careerStats.totalSixes}", modifier = Modifier.weight(1f))
                }
            }
        }

        // --- 4. Complete Bowling Statistics Section ---
        Text(
            text = "BOWLING PROFILE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = BallRedClassic,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricBlock(label = "Bowl Innings", value = "${careerStats.bowlingInnings}", modifier = Modifier.weight(1f))
                    MetricBlock(label = "Economy", value = careerStats.bowlingEconomy, valueColor = BallRedClassic, modifier = Modifier.weight(1f))
                    MetricBlock(label = "Average", value = careerStats.bowlingAvg, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricBlock(label = "Overs", value = careerStats.bowlingOvers, modifier = Modifier.weight(1f))
                    MetricBlock(label = "Runs Con", value = "${careerStats.totalRunsConceded}", modifier = Modifier.weight(1f))
                    MetricBlock(label = "Maidens", value = "${careerStats.totalMaidens}", modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun StatHeaderCard(
    label: String,
    value: String,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = activeColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun MetricBlock(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}

// Player representation Card design
@Composable
fun PlayerCard(
    player: Player,
    stats: CricketViewModel.PlayerCareerStats,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val avatarColor = AvatarColors[player.avatarColorIndex % AvatarColors.size]
    
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("player_card_${player.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Profile thumbnail representation
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            avatarColor,
                                            avatarColor.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                        }
                        
                        // Jersey number in sub title badge with border outline
                        if (player.jerseyNumber.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(WicketGold)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.jerseyNumber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TurfGreenPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = player.role,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = player.team,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Delete Icon
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_player_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete player record",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )

            // Career highlight metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatThumbnail(label = "Matches", value = "${stats.totalMatches}", modifier = Modifier.weight(1f))
                StatThumbnail(label = "Runs Scored", value = "${stats.totalRuns}", modifier = Modifier.weight(1f))
                StatThumbnail(label = "Bat Avg", value = stats.battingAvg, modifier = Modifier.weight(1f))
                StatThumbnail(label = "Wickets", value = "${stats.totalWickets}", modifier = Modifier.weight(1f))
                StatThumbnail(label = "Econ", value = stats.bowlingEconomy, modifier = Modifier.weight(1f))
            }
        }
    }
}

// Micro stat tracker box inside cards
@Composable
fun StatThumbnail(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        val color = when (label.uppercase()) {
            "BAT AVG" -> TurfGreenClassic
            "ECON" -> if (value != "0.00" && value != "N/A" && value != "0") BallRedClassic else MaterialTheme.colorScheme.onBackground
            "RUNS SCORED" -> TurfGreenPrimary
            "WICKETS" -> BallRedPrimary
            else -> MaterialTheme.colorScheme.onBackground
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            letterSpacing = 0.4.sp
        )
    }
}

// --- Tab 1 View: Match logs list ---
@Composable
fun MatchLogTabContent(
    allPerformances: List<MatchPerformance>,
    selectedPlayer: Player?,
    playerIdToName: Map<Int, String>,
    playerIdToColor: Map<Int, Int>,
    onDeletePerformance: (MatchPerformance) -> Unit
) {
    // Determine which logs to show: if selective context is active, filter.
    val filteredLogs = remember(allPerformances, selectedPlayer) {
        if (selectedPlayer != null) {
            allPerformances.filter { it.playerId == selectedPlayer.id }
        } else {
            allPerformances
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (selectedPlayer != null) "Innings for ${selectedPlayer.name}" else "Recent Match Innings (All Team)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp),
                        contentDescription = "Empty match logs icon"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No innings logged yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Log cricket matches by pressing 'Log Inning' on the bottom right.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("innings_lazy_column")
            ) {
                items(filteredLogs) { performance ->
                    InningLogCard(
                        performance = performance,
                        playerName = playerIdToName[performance.playerId] ?: "Deleted Player",
                        colorIndex = playerIdToColor[performance.playerId] ?: 0,
                        onDelete = { onDeletePerformance(performance) }
                    )
                }
            }
        }
    }
}

// Inning/Match scorecard representation
@Composable
fun InningLogCard(
    performance: MatchPerformance,
    playerName: String,
    colorIndex: Int,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val dateString = remember(performance.date) { formatter.format(Date(performance.date)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Format, date, player avatar, delete option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Match format badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (performance.matchFormat) {
                                    "T20" -> TurfGreenClassic.copy(alpha = 0.15f)
                                    "ODI" -> WicketGold.copy(alpha = 0.15f)
                                    else -> BallRedClassic.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = performance.matchFormat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (performance.matchFormat) {
                                "T20" -> TurfGreenClassic
                                "ODI" -> WicketWood
                                else -> BallRedClassic
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    // Age group badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (performance.ageGroup) {
                                    "U15" -> Color(0xFF1E88E5).copy(alpha = 0.15f)
                                    "U17" -> Color(0xFF8E24AA).copy(alpha = 0.15f)
                                    else -> Color(0xFFE53935).copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = performance.ageGroup,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (performance.ageGroup) {
                                    "U15" -> Color(0xFF1E88E5)
                                    "U17" -> Color(0xFF8E24AA)
                                    else -> Color(0xFFE53935)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "vs ${performance.opponent}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete this logged match performance",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player who compiled the innings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val colorIdx = ((colorIndex % AvatarColors.size) + AvatarColors.size) % AvatarColors.size
                val color = AvatarColors[colorIdx]
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                ) {
                    Text(
                        text = playerName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // Performance scorecard blocks side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Batting Score outline
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "BATTING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurfGreenClassic
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (performance.didBat) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${performance.runsScored}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (performance.isNotOut) {
                                    Text(
                                        text = "*",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WicketGold,
                                        modifier = Modifier.offset(y = (-4).dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${performance.ballsFaced}b)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "4s: ${performance.fours}  •  6s: ${performance.sixes}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = "DNB (Did Not Bat)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // Bowling Score outline
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "BOWLING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BallRedClassic
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (performance.didBowl) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${performance.wicketsTaken}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BallRedClassic
                                )
                                Text(
                                    text = " for ${performance.runsConceded}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${performance.getOversFormatted()} ovs  •  Mdn: ${performance.maidensBowled}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = "DNB (Did Not Bowl)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 2 View: Interactive charts and summaries ---
@Composable
fun InsightsTabContent(
    selectedPlayer: Player?,
    performances: List<MatchPerformance>,
    playerName: String,
    viewModel: CricketViewModel
) {
    var selectedAgeFilter by remember { mutableStateOf("All") }

    val filteredPerformances = remember(performances, selectedAgeFilter) {
        if (selectedAgeFilter == "All") performances else performances.filter { it.ageGroup == selectedAgeFilter }
    }
    val stats = remember(filteredPerformances) { viewModel.computeStats(filteredPerformances) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Summary Title Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Career overview symbol",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Historical Focus: $playerName",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Comprehensive performance analytics and career graphs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Age filter chips row
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Filter career charts by Age Group:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("All", "U15", "U17", "Adult").forEach { ageOpt ->
                            val isSelected = selectedAgeFilter == ageOpt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) {
                                            when (ageOpt) {
                                                "U15" -> Color(0xFF1E88E5)
                                                "U17" -> Color(0xFF8E24AA)
                                                "Adult" -> Color(0xFFE53935)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        }
                                    )
                                    .clickable { selectedAgeFilter = ageOpt }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ageOpt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        if (performances.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty graph view symbol",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No analytics data yet",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "You need to log at least one match performance inside Tab 1 to generate analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Include Age Group Comparison Report card
            item {
                AgeGroupComparisonReport(allPerformances = performances, viewModel = viewModel)
            }
            // General Stats numerical summary
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricBox(
                        title = "Matches",
                        value = "${stats.totalMatches}",
                        icon = Icons.Default.Star,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Form / Formats",
                        value = performances.map { it.matchFormat }.distinct().joinToString("/"),
                        icon = Icons.Default.LocationOn,
                        color = WicketWood,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Batting performance charts
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BATTING HIGHLIGHTS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TurfGreenClassic
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Total Runs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text(text = "${stats.totalRuns}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                            }
                            Column {
                                Text(text = "Average", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text(text = stats.battingAvg, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                            }
                            Column {
                                Text(text = "Strike Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text(text = stats.battingStrikeRate, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Canvas-drawn runs chart
                        Text(
                            text = "Recent Batting Scores",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val battingScores = performances
                            .filter { it.didBat }
                            .map { it.runsScored }
                            .reversed() // order chronological

                        if (battingScores.isNotEmpty()) {
                            ScoresLineChart(
                                scores = battingScores,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(vertical = 8.dp)
                            )
                        } else {
                            Text(
                                "No batting outings for this selection yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }

            // Bowling highlights
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BOWLING HIGHLIGHTS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = BallRedClassic
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Wickets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text(text = "${stats.totalWickets}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                            }
                            Column {
                                Text(text = "Overs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text(text = stats.bowlingOvers, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                            }
                            Column {
                                Text(text = "Economy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text(text = stats.bowlingEconomy, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Wickets distribution chart
                        Text(
                            text = "Wickets Taken (Last 6 Games with bowling)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val wicketsList = performances
                            .filter { it.didBowl }
                            .map { it.wicketsTaken }
                            .reversed()
                            .takeLast(6)

                        if (wicketsList.isNotEmpty()) {
                            WicketsBarChart(
                                wickets = wicketsList,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .padding(vertical = 8.dp)
                            )
                        } else {
                            Text(
                                "No bowling outings for this selection yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Stats metrics detail display
@Composable
fun MetricBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

// --- Custom Canvas charts to display Batting scores line ---
@Composable
fun ScoresLineChart(
    scores: List<Int>,
    modifier: Modifier = Modifier
) {
    val primaryColor = TurfGreenClassic
    val accentColor = WicketGold
    val textColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxScore = (scores.maxOrNull() ?: 10).coerceAtLeast(30)
        
        val pointSpacing = if (scores.size > 1) width / (scores.size - 1) else width
        val points = scores.mapIndexed { idx, score ->
            val x = idx * pointSpacing
            // Map height, keep 15% padding at top and bottom
            val y = height - (0.15f * height + (score.toFloat() / maxScore) * (0.7f * height))
            Offset(x, y)
        }

        // Draw guideline grids as beautiful dashed lines
        val gridLinesCount = 3
        for (i in 0..gridLinesCount) {
            val fraction = i.toFloat() / gridLinesCount
            val gridY = 0.15f * height + fraction * (0.7f * height)
            drawLine(
                color = textColor.copy(alpha = 0.08f),
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        }

        // Draw path connect
        if (points.size > 1) {
            // Draw a smooth bezier cubic spline
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p_prev = points[i]
                    val p_next = points[i + 1]
                    cubicTo(
                        x1 = p_prev.x + (p_next.x - p_prev.x) / 2f,
                        y1 = p_prev.y,
                        x2 = p_prev.x + (p_next.x - p_prev.x) / 2f,
                        y2 = p_next.y,
                        x3 = p_next.x,
                        y3 = p_next.y
                    )
                }
            }
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw filled background gradient
            val filledPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, height)
                lineTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p_prev = points[i]
                    val p_next = points[i + 1]
                    cubicTo(
                        x1 = p_prev.x + (p_next.x - p_prev.x) / 2f,
                        y1 = p_prev.y,
                        x2 = p_prev.x + (p_next.x - p_prev.x) / 2f,
                        y2 = p_next.y,
                        x3 = p_next.x,
                        y3 = p_next.y
                    )
                }
                lineTo(points.last().x, height)
                close()
            }
            drawPath(
                path = filledPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.25f),
                        primaryColor.copy(alpha = 0.0f)
                    )
                )
            )
        } else if (points.size == 1) {
            // Draw a horizontal trend line for a single match point
            drawLine(
                color = primaryColor.copy(alpha = 0.4f),
                start = Offset(0f, points[0].y),
                end = Offset(width, points[0].y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Draw individual data points with numeric score text overlays
        val nativePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (textColor.alpha * 220).toInt(),
                (textColor.red * 255).toInt(),
                (textColor.green * 255).toInt(),
                (textColor.blue * 255).toInt()
            )
            textSize = 9.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        points.forEachIndexed { index, p ->
            // Drawing points with outer halo
            drawCircle(
                color = primaryColor.copy(alpha = 0.2f),
                radius = 8.dp.toPx(),
                center = p
            )
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = p
            )
            drawCircle(
                color = primaryColor,
                radius = 2.5f.dp.toPx(),
                center = p
            )

            // Draw label value text right above the dot
            val scoreVal = scores[index]
            drawContext.canvas.nativeCanvas.drawText(
                "$scoreVal",
                p.x,
                p.y - 12.dp.toPx(),
                nativePaint
            )

            // Draw small match counter index at the very bottom
            drawContext.canvas.nativeCanvas.drawText(
                "M${index + 1}",
                p.x,
                height - 4.dp.toPx(),
                nativePaint.apply {
                    textSize = 8.dp.toPx()
                    alpha = 130
                }
            )
        }
    }
}

// --- Custom Canvas-drawn wickets bar chart ---
@Composable
fun WicketsBarChart(
    wickets: List<Int>,
    modifier: Modifier = Modifier
) {
    val barColor = BallRedClassic
    val textColor = MaterialTheme.colorScheme.onSurface
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val maxWickets = (wickets.maxOrNull() ?: 2).coerceAtLeast(3)
        val itemCount = wickets.size
        
        val spacing = 16.dp.toPx()
        val totalSpacing = spacing * (itemCount + 1)
        val barWidth = ((width - totalSpacing) / itemCount).coerceAtLeast(1f)
        
        // Define native paint for numbers
        val nativePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (textColor.alpha * 220).toInt(),
                (textColor.red * 255).toInt(),
                (textColor.green * 255).toInt(),
                (textColor.blue * 255).toInt()
            )
            textSize = 9.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        wickets.forEachIndexed { index, w ->
            val left = spacing + index * (barWidth + spacing)
            val top = height - (0.15f * height + (w.toFloat() / maxWickets) * (0.65f * height))
            val right = left + barWidth
            val bottom = height - 16.dp.toPx()

            // Draw a soft ambient guide line for empty spaces
            if (w == 0) {
                drawRoundRect(
                    color = barColor.copy(alpha = 0.05f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, (bottom - top).coerceAtLeast(6.dp.toPx())),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            } else {
                // High-fidelity bowling ball red glassmorphism bar
                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        barColor,
                        barColor.copy(alpha = 0.7f)
                    )
                )
                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, (bottom - top).coerceAtLeast(6.dp.toPx())),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                // Draw wicket stumps inside the bar (3 vertical gold lines representing the stumps!)
                val stumpCount = 3
                val stumpWidth = (barWidth * 0.12f).coerceIn(1.dp.toPx(), 4.dp.toPx())
                val startStumpX = left + barWidth * 0.2f
                val endStumpX = left + barWidth * 0.8f
                val stumpSpacing = if (stumpCount > 1) (endStumpX - startStumpX) / (stumpCount - 1) else 0f
                
                for (s in 0 until stumpCount) {
                    val stumpX = startStumpX + s * stumpSpacing
                    drawLine(
                        color = WicketGold.copy(alpha = 0.85f),
                        start = Offset(stumpX, top + 6.dp.toPx()),
                        end = Offset(stumpX, bottom - 4.dp.toPx()),
                        strokeWidth = stumpWidth
                    )
                }

                // Draw the bail on top of stumps
                drawLine(
                    color = WicketGold,
                    start = Offset(left + barWidth * 0.15f, top + 4.dp.toPx()),
                    end = Offset(left + barWidth * 0.85f, top + 4.dp.toPx()),
                    strokeWidth = (barWidth * 0.08f).coerceIn(1.dp.toPx(), 2.dp.toPx())
                )
            }

            // Draw number of wickets taken right above the column
            drawContext.canvas.nativeCanvas.drawText(
                "$w W",
                left + barWidth / 2f,
                top - 8.dp.toPx(),
                nativePaint.apply {
                    color = if (w > 0) {
                        android.graphics.Color.argb(
                            255,
                            (barColor.red * 255).toInt(),
                            (barColor.green * 255).toInt(),
                            (barColor.blue * 255).toInt()
                        )
                    } else {
                        android.graphics.Color.argb(
                            120,
                            (textColor.red * 255).toInt(),
                            (textColor.green * 255).toInt(),
                            (textColor.blue * 255).toInt()
                        )
                    }
                }
            )

            // Draw match counter label at the bottom line
            drawContext.canvas.nativeCanvas.drawText(
                "M${index + 1}",
                left + barWidth / 2f,
                height - 2.dp.toPx(),
                nativePaint.apply {
                    color = android.graphics.Color.argb(
                        130,
                        (textColor.red * 255).toInt(),
                        (textColor.green * 255).toInt(),
                        (textColor.blue * 255).toInt()
                    )
                    textSize = 8.dp.toPx()
                }
            )
        }
    }
}

// --- Dialog 1: Edit Profile Settings ---
@Composable
fun EditPlayerDialog(
    player: Player,
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: String, team: String, jersey: String, colorIndex: Int) -> Unit
) {
    var name by remember { mutableStateOf(player.name) }
    var team by remember { mutableStateOf(player.team) }
    var jersey by remember { mutableStateOf(player.jerseyNumber) }
    var selectedRole by remember { mutableStateOf(player.role) }
    var selectedColorIndex by remember { mutableIntStateOf(player.avatarColorIndex) }
    
    val rolesList = listOf("Batter", "Bowler", "All-Rounder", "Wicketkeeper")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("edit_player_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Profile & Theme",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Input fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_name_field")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = team,
                    onValueChange = { team = it },
                    label = { Text("Your Team Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jersey,
                    onValueChange = { jersey = it },
                    label = { Text("Jersey Number (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Player Role Chips selection
                Text(
                    text = "Your Playing Role:",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rolesList.forEach { r ->
                        FilterChip(
                            selected = selectedRole == r,
                            onClick = { selectedRole = r },
                            label = { Text(r) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Profile color indices selection
                Text(
                    text = "Profile Theme Color:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AvatarColors.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColorIndex == idx) 3.dp else 0.dp,
                                    color = if (selectedColorIndex == idx) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = idx }
                        ) {
                            if (selectedColorIndex == idx) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected color indicator",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, selectedRole, team.ifBlank { "Unassigned" }, jersey, selectedColorIndex)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("player_confirm_button")
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- Dialog 2: Add Game Inning scorecard ---
@Composable
fun AddPerformanceDialog(
    players: List<Player>,
    selectedPlayer: Player?,
    onDismiss: () -> Unit,
    onConfirm: (
        playerId: Int,
        opponent: String,
        matchFormat: String,
        ageGroup: String,
        didBat: Boolean,
        runsScored: Int,
        ballsFaced: Int,
        isNotOut: Boolean,
        fours: Int,
        sixes: Int,
        didBowl: Boolean,
        overs: Double,
        runsConceded: Int,
        wickets: Int,
        maidens: Int
    ) -> Unit
) {
    // Basic setup
    var chosenPlayerIndex by remember { 
        val defaultIdx = players.indexOfFirst { it.id == selectedPlayer?.id }
        mutableIntStateOf(if (defaultIdx != -1) defaultIdx else 0) 
    }
    
    var opponent by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("T20") }
    var selectedAgeGroup by remember { mutableStateOf("Adult") }
    
    // Batting stats toggle & inputs
    var didBat by remember { mutableStateOf(true) }
    var runsStr by remember { mutableStateOf("0") }
    var ballsStr by remember { mutableStateOf("0") }
    var isNotOut by remember { mutableStateOf(false) }
    var foursStr by remember { mutableStateOf("0") }
    var sixesStr by remember { mutableStateOf("0") }

    // Bowling stats toggle & inputs
    var didBowl by remember { mutableStateOf(false) }
    var oversStr by remember { mutableStateOf("0") } // e.g. "4" or "3.4"
    var runsConcededStr by remember { mutableStateOf("0") }
    var wicketsStr by remember { mutableStateOf("0") }
    var maidensStr by remember { mutableStateOf("0") }

    val formats = listOf("T20", "ODI", "Test")
    val ageGroups = listOf("U15", "U17", "Adult")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("add_performance_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Log Innings Card",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Player is selected automatically (Single Player Mode)
                Spacer(modifier = Modifier.height(10.dp))

                // Opponent Text input
                OutlinedTextField(
                    value = opponent,
                    onValueChange = { opponent = it },
                    label = { Text("Opponent Team") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opponent_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Format selection dropdown custom row
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Format:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            formats.forEach { form ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedFormat == form) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        )
                                        .clickable { selectedFormat = form }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = form,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedFormat == form) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Age Group Selection
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "Age Group:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ageGroups.forEach { group ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedAgeGroup == group) {
                                                when (group) {
                                                    "U15" -> Color(0xFF1E88E5)
                                                    "U17" -> Color(0xFF8E24AA)
                                                    else -> Color(0xFFE53935)
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                            }
                                        )
                                        .clickable { selectedAgeGroup = group }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = group,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedAgeGroup == group) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- BATTING SECTION ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (didBat) TurfGreenSoft.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = TurfGreenClassic, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Batter Outing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Switch(
                                checked = didBat,
                                onCheckedChange = { didBat = it }
                            )
                        }

                        if (didBat) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = runsStr,
                                    onValueChange = { runsStr = it.filter { char -> char.isDigit() } },
                                    label = { Text("Runs") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("runs_field")
                                )
                                OutlinedTextField(
                                    value = ballsStr,
                                    onValueChange = { ballsStr = it.filter { char -> char.isDigit() } },
                                    label = { Text("Balls") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = foursStr,
                                    onValueChange = { foursStr = it.filter { char -> char.isDigit() } },
                                    label = { Text("4s") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = sixesStr,
                                    onValueChange = { sixesStr = it.filter { char -> char.isDigit() } },
                                    label = { Text("6s") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isNotOut,
                                    onCheckedChange = { isNotOut = it }
                                )
                                Text(text = "Remained Not Out", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- BOWLING SECTION ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (didBowl) BallRedClassic.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BallRedClassic, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Bowler Outing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Switch(
                                checked = didBowl,
                                onCheckedChange = { didBowl = it }
                            )
                        }

                        if (didBowl) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = oversStr,
                                    onValueChange = { oversStr = it.filter { char -> char.isDigit() || char == '.' } },
                                    label = { Text("Overs (e.g. 4 or 3.1)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.3f)
                                )
                                OutlinedTextField(
                                    value = runsConcededStr,
                                    onValueChange = { runsConcededStr = it.filter { char -> char.isDigit() } },
                                    label = { Text("Runs Con") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = wicketsStr,
                                    onValueChange = { wicketsStr = it.filter { char -> char.isDigit() } },
                                    label = { Text("Wick Con") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("wickets_field")
                                )
                                OutlinedTextField(
                                    value = maidensStr,
                                    onValueChange = { maidensStr = it.filter { char -> char.isDigit() } },
                                    label = { Text("Maidens") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val selectedPlayerId = players.getOrNull(chosenPlayerIndex)?.id ?: selectedPlayer?.id ?: 0
                            if (opponent.isNotBlank() && (didBat || didBowl) && selectedPlayerId > 0) {
                                val runs = runsStr.toIntOrNull() ?: 0
                                val balls = ballsStr.toIntOrNull() ?: 0
                                val fours = foursStr.toIntOrNull() ?: 0
                                val sixes = sixesStr.toIntOrNull() ?: 0
                                
                                val overs = oversStr.toDoubleOrNull() ?: 0.0
                                val runsCon = runsConcededStr.toIntOrNull() ?: 0
                                val wickets = wicketsStr.toIntOrNull() ?: 0
                                val maidens = maidensStr.toIntOrNull() ?: 0

                                onConfirm(
                                    selectedPlayerId,
                                    opponent,
                                    selectedFormat,
                                    selectedAgeGroup,
                                    didBat,
                                    runs,
                                    balls,
                                    isNotOut,
                                    fours,
                                    sixes,
                                    didBowl,
                                    overs,
                                    runsCon,
                                    wickets,
                                    maidens
                                )
                            }
                        },
                        enabled = opponent.isNotBlank() && (didBat || didBowl),
                        modifier = Modifier.testTag("score_confirm_button")
                    ) {
                        Text("Save Performance", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AgeGroupComparisonReport(
    allPerformances: List<MatchPerformance>,
    viewModel: CricketViewModel
) {
    val statsAll = remember(allPerformances) { viewModel.computeStats(allPerformances) }
    val statsU15 = remember(allPerformances) { viewModel.computeStats(allPerformances.filter { it.ageGroup == "U15" }) }
    val statsU17 = remember(allPerformances) { viewModel.computeStats(allPerformances.filter { it.ageGroup == "U17" }) }
    val statsAdult = remember(allPerformances) { viewModel.computeStats(allPerformances.filter { it.ageGroup == "Adult" }) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AGE GROUPS COMPARISON REPORT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                ),
                color = TurfGreenClassic
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Table Header with capsule border background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Age Group", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)))
                Text(text = "Matches", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)), textAlign = TextAlign.Center)
                Text(text = "Bat Avg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)), textAlign = TextAlign.Center)
                Text(text = "Bowl Avg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)), textAlign = TextAlign.Center)
                Text(text = "Econ", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)), textAlign = TextAlign.Center)
            }

            listOf(
                Quadruple("U15 Team", statsU15, Color(0xFF1E88E5), "U15"),
                Quadruple("U17 Team", statsU17, Color(0xFF8E24AA), "U17"),
                Quadruple("Adults", statsAdult, Color(0xFFE53935), "Adult"),
                Quadruple("All Groups", statsAll, TurfGreenClassic, "All")
            ).forEach { item ->
                val groupStats = item.stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Group name with a bold bullet indicator
                    Row(
                        modifier = Modifier.weight(1.1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Matches
                    Text(
                        text = "${groupStats.totalMatches}",
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center
                    )

                    // Bat Avg with adaptive green color text
                    Text(
                        text = groupStats.battingAvg,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center,
                        color = if (groupStats.battingAvg != "0" && groupStats.battingAvg != "0.00" && groupStats.battingAvg != "N/A") TurfGreenClassic else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    // Bowl Avg with adaptive red color text
                    Text(
                        text = groupStats.bowlingAvg,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center,
                        color = if (groupStats.bowlingAvg != "N/A" && groupStats.bowlingAvg != "0.00" && groupStats.bowlingAvg != "0") BallRedClassic else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    // Bowl Economy
                    Text(
                        text = groupStats.bowlingEconomy,
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        color = if (groupStats.bowlingEconomy != "0.00" && groupStats.bowlingEconomy != "N/A") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom explanation insight callout box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Insight icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Performances are grouped logically to contrast developmental trajectory averages across age-group divisions.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

// Simple Quadruple helper data holder
data class Quadruple<A, B, C, D>(
    val label: A,
    val stats: B,
    val color: C,
    val keyID: D
)
