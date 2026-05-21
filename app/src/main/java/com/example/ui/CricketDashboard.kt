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
    val players by viewModel.filteredPlayers.collectAsState()
    val allPerformances by viewModel.performancesFlow.collectAsState(initial = emptyList())
    val selectedPlayerPerformances by viewModel.selectedPlayerPerformances.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedRoleFilter by viewModel.filterRole.collectAsState()

    // Dialog trigger states
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showAddPerformanceDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_scaffold"),
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // FAB to register a new player
                FloatingActionButton(
                    onClick = { showAddPlayerDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .testTag("add_player_fab")
                        .size(56.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Add New Player"
                    )
                }

                // FAB to record a new match performance (is showing if players aren't empty)
                if (players.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddPerformanceDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.testTag("add_performance_fab"),
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Match Stats") },
                        text = { Text("Log Inning", fontWeight = FontWeight.Bold) }
                    )
                }
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
                onResetSelection = { viewModel.selectPlayer(null) },
                selectedPlayerName = selectedPlayer?.name
            )

            // Player Quick Slide list at the top to filter statistics context
            HorizontalPlayerSelector(
                players = players,
                selectedPlayer = selectedPlayer,
                onPlayerSelect = { viewModel.selectPlayer(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Tabs Layout
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    text = { Text("Players & Career", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Players tab") },
                    modifier = Modifier.testTag("player_list_tab")
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    text = { Text("Match Innings", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Match logs tab") },
                    modifier = Modifier.testTag("match_log_tab")
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { viewModel.setActiveTab(2) },
                    text = { Text("Visual Insights", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Charts tab") },
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
                            players = players,
                            selectedPlayer = selectedPlayer,
                            searchQuery = searchQuery,
                            selectedRoleFilter = selectedRoleFilter,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onRoleFilterChange = { viewModel.setFilterRole(it) },
                            onSelectPlayer = { viewModel.selectPlayer(it) },
                            onDeletePlayer = { viewModel.deletePlayer(it) },
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
    if (showAddPlayerDialog) {
        AddPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onConfirm = { name, role, team, jersey, colorIndex ->
                viewModel.addPlayer(name, role, team, jersey, colorIndex)
                showAddPlayerDialog = false
            }
        )
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
                // Drawing Turf Grass gradient as visual accent
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            TurfGreenPrimary,
                            TurfGreenClassic
                        )
                    )
                )
            }
            .statusBarsPadding()
            .padding(16.dp)
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

                // App symbol
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(WicketGold)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "App logo",
                        tint = TurfGreenPrimary,
                        modifier = Modifier.size(28.dp)
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
                        val color = AvatarColors[player.avatarColorIndex % AvatarColors.size]
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

// --- Tab 0 View: Players & Quick summary ---
@Composable
fun PlayersTabContent(
    players: List<Player>,
    selectedPlayer: Player?,
    searchQuery: String,
    selectedRoleFilter: String?,
    onSearchChange: (String) -> Unit,
    onRoleFilterChange: (String?) -> Unit,
    onSelectPlayer: (Player) -> Unit,
    onDeletePlayer: (Player) -> Unit,
    viewModel: CricketViewModel
) {
    val roles = listOf("All", "Batter", "Bowler", "All-Rounder", "Wicketkeeper")
    var confirmDeletePlayer by remember { mutableStateOf<Player?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search player by name or team...") },
            prefix = { Icon(Icons.Default.Search, contentDescription = "Search icon", modifier = Modifier.padding(end = 4.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_search_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Role quick filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            roles.forEach { r ->
                val currentFilter = if (r == "All") null else r
                FilterChip(
                    selected = selectedRoleFilter == currentFilter,
                    onClick = { onRoleFilterChange(currentFilter) },
                    label = { Text(r) },
                    shape = RoundedCornerShape(50.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (players.isEmpty()) {
            // Elegant Empty state
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
                        imageVector = Icons.Default.Person,
                        contentDescription = "Empty list outline",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No players found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap the '+' icon on the bottom right to add a cricketer and record performance stats.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            // Player listing scroll
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("player_lazy_column")
            ) {
                items(players, key = { it.id }) { player ->
                    // Reactive database collection for individual career stats preview
                    val playerPerfFlow = remember(player.id) { viewModel.getPerformancesForPlayer(player.id) }
                    val playerPerf: List<MatchPerformance> by playerPerfFlow.collectAsState(initial = emptyList())
                    val careerStats = viewModel.computeStats(playerPerf)

                    PlayerCard(
                        player = player,
                        stats = careerStats,
                        isSelected = selectedPlayer?.id == player.id,
                        onSelect = { onSelectPlayer(player) },
                        onDelete = { confirmDeletePlayer = player }
                    )
                }
            }
        }
    }

    if (confirmDeletePlayer != null) {
        AlertDialog(
            onDismissRequest = { confirmDeletePlayer = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeletePlayer?.let(onDeletePlayer)
                        confirmDeletePlayer = null
                    }
                ) {
                    Text("Delete player", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeletePlayer = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete Player?") },
            text = { Text("Are you sure you want to delete ${confirmDeletePlayer?.name}? This will permanently remove all of their registered batting and bowling match data.") }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
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
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(avatarColor)
                    ) {
                        Text(
                            text = player.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            ),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        // Jersey number in sub title badge
                        if (player.jerseyNumber.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(WicketGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.jerseyNumber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player.role,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "  •  ",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
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
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.0.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
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
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
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
                val color = AvatarColors[colorIndex % AvatarColors.size]
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

        // Draw guideline grids
        val gridLinesCount = 3
        for (i in 0..gridLinesCount) {
            val gridY = 0.15f * height + (i.toFloat() / gridLinesCount) * (0.7f * height)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw path connect
        if (points.size > 1) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw filled background gradient
            val filledPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, height)
                for (p in points) {
                    lineTo(p.x, p.y)
                }
                lineTo(points.last().x, height)
                close()
            }
            drawPath(
                path = filledPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.0f)
                    )
                )
            )
        }

        // Draw individual data points with numeric score text overlays
        points.forEachIndexed { index, p ->
            drawCircle(
                color = accentColor,
                radius = 5.dp.toPx(),
                center = p
            )
            drawCircle(
                color = primaryColor,
                radius = 3.dp.toPx(),
                center = p
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
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val maxWickets = (wickets.maxOrNull() ?: 2).coerceAtLeast(3)
        val itemCount = wickets.size
        
        val spacing = 20.dp.toPx()
        val totalSpacing = spacing * (itemCount + 1)
        val barWidth = (width - totalSpacing) / itemCount

        wickets.forEachIndexed { index, w ->
            val left = spacing + index * (barWidth + spacing)
            val top = height - (0.1f * height + (w.toFloat() / maxWickets) * (0.8f * height))
            val right = left + barWidth
            val bottom = height - 10.dp.toPx()

            // Draw bar with rounded corners
            drawRoundRect(
                color = barColor.copy(alpha = if (w == 0) 0.15f else 0.85f),
                topLeft = Offset(left, top),
                size = Size(barWidth, (bottom - top).coerceAtLeast(5.dp.toPx())),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Overlap line inside bar
            if (w > 0) {
                drawLine(
                    color = WicketGold,
                    start = Offset(left + barWidth / 2, top + 4.dp.toPx()),
                    end = Offset(left + barWidth / 2, bottom),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

// --- Dialog 1: Create a player ---
@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: String, team: String, jersey: String, colorIndex: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var team by remember { mutableStateOf("") }
    var jersey by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Batter") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    
    val rolesList = listOf("Batter", "Bowler", "All-Rounder", "Wicketkeeper")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_player_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add Cricketer",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Input fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_name_field")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = team,
                    onValueChange = { team = it },
                    label = { Text("Team Name (e.g. Thunder, Hurricanes)") },
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
                    text = "Playing Role:",
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
                    text = "Profile Avatar Theme Color:",
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
                        Text("Add Player", fontWeight = FontWeight.Bold)
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

                // Selector: Which Player does this performance belong to
                Text(
                    text = "Select Cricketer:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    players.forEachIndexed { index, p ->
                        InputChip(
                            selected = chosenPlayerIndex == index,
                            onClick = { chosenPlayerIndex = index },
                            label = { Text(p.name) }
                        )
                    }
                }
                
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
                            if (opponent.isNotBlank() && (didBat || didBowl)) {
                                val selectedPlayerId = players[chosenPlayerIndex].id
                                
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AGE GROUPS PERFORMANCE REPORT",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TurfGreenClassic
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Age Group", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Text(text = "Matches", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Text(text = "Bat Avg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Text(text = "Bowl Avg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Text(text = "Econ", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            }

            listOf(
                Quadruple("U15", statsU15, Color(0xFF1E88E5), "U15"),
                Quadruple("U17", statsU17, Color(0xFF8E24AA), "U17"),
                Quadruple("Adult", statsAdult, Color(0xFFE53935), "Adult"),
                Quadruple("All Groups", statsAll, TurfGreenClassic, "All")
            ).forEach { item ->
                val groupStats = item.stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Group name with a small bullet indicator
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Matches
                    Text(
                        text = "${groupStats.totalMatches}",
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )

                    // Bat Avg
                    Text(
                        text = groupStats.battingAvg,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        color = if (groupStats.battingAvg != "0" && groupStats.battingAvg != "0.00" && groupStats.battingAvg != "N/A") TurfGreenClassic else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // Bowl Avg
                    Text(
                        text = groupStats.bowlingAvg,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        color = if (groupStats.bowlingAvg != "N/A" && groupStats.bowlingAvg != "0.00" && groupStats.bowlingAvg != "0") BallRedClassic else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // Bowl Economy
                    Text(
                        text = groupStats.bowlingEconomy,
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = if (groupStats.bowlingEconomy != "0.00") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
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
