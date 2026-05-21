package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CricketViewModel(
    application: Application,
    private val repository: CricketRepository
) : AndroidViewModel(application) {

    // Active tab: 0 = Players, 1 = Performances / Match Logs, 2 = Insights & Charts
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Selected Player for detailed statistical breakdown (the single tracked player)
    private val _selectedPlayer = MutableStateFlow<Player?>(null)
    val selectedPlayer: StateFlow<Player?> = _selectedPlayer.asStateFlow()

    // Raw reactive flows from database
    val playersFlow: Flow<List<Player>> = repository.allPlayers
    val performancesFlow: Flow<List<MatchPerformance>> = repository.allPerformances

    init {
        var hasAttemptedInit = false
        viewModelScope.launch {
            playersFlow.collect { list ->
                if (list.isEmpty() && !hasAttemptedInit) {
                    hasAttemptedInit = true
                    val defaultPlayer = Player(
                        name = "Your Profile",
                        role = "All-Rounder",
                        team = "Thunder CC",
                        jerseyNumber = "7",
                        avatarColorIndex = 0
                    )
                    repository.insertPlayer(defaultPlayer)
                } else if (list.isNotEmpty()) {
                    val previouslySelected = _selectedPlayer.value
                    if (previouslySelected == null) {
                        _selectedPlayer.value = list.first()
                    } else {
                        // Sync edited state
                        val updated = list.find { it.id == previouslySelected.id } ?: list.first()
                        _selectedPlayer.value = updated
                    }
                }
            }
        }
    }

    // Filters & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterRole = MutableStateFlow<String?>(null) // null = All
    val filterRole: StateFlow<String?> = _filterRole.asStateFlow()

    // Filtered Players list based on search and role filters
    val filteredPlayers: StateFlow<List<Player>> = combine(
        playersFlow,
        _searchQuery,
        _filterRole
    ) { players, search, role ->
        players.filter { player ->
            val matchesSearch = player.name.contains(search, ignoreCase = true) || 
                                player.team.contains(search, ignoreCase = true)
            val matchesRole = role == null || player.role == role
            matchesSearch && matchesRole
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Match performances of the currently selected player
    val selectedPlayerPerformances: StateFlow<List<MatchPerformance>> = _selectedPlayer
        .flatMapLatest { player ->
            if (player != null) {
                repository.getPerformancesForPlayer(player.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setActiveTab(tabIndex: Int) {
        _activeTab.value = tabIndex
    }

    fun selectPlayer(player: Player?) {
        _selectedPlayer.value = player
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterRole(role: String?) {
        _filterRole.value = role
    }

    fun getPerformancesForPlayer(playerId: Int): Flow<List<MatchPerformance>> {
        return repository.getPerformancesForPlayer(playerId)
    }

    // --- Action functions to update database ---

    fun addPlayer(name: String, role: String, team: String, jersey: String, colorIndex: Int) {
        viewModelScope.launch {
            val newPlayer = Player(
                name = name.trim(),
                role = role,
                team = team.trim(),
                jerseyNumber = jersey.trim(),
                avatarColorIndex = colorIndex
            )
            repository.insertPlayer(newPlayer)
        }
    }

    fun updatePlayer(id: Int, name: String, role: String, team: String, jersey: String, colorIndex: Int) {
        viewModelScope.launch {
            val updatedPlayer = Player(
                id = id,
                name = name.trim(),
                role = role,
                team = team.trim(),
                jerseyNumber = jersey.trim(),
                avatarColorIndex = colorIndex
            )
            repository.insertPlayer(updatedPlayer)
            _selectedPlayer.value = updatedPlayer
        }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            // Room cascades match performances deletion due to the ForeignKey cascade rule
            repository.deletePlayer(player)
            if (_selectedPlayer.value?.id == player.id) {
                _selectedPlayer.value = null
            }
        }
    }

    fun addMatchPerformance(
        playerId: Int,
        opponent: String,
        matchFormat: String,
        didBat: Boolean,
        runsScored: Int,
        ballsFaced: Int,
        isNotOut: Boolean,
        fours: Int,
        sixes: Int,
        didBowl: Boolean,
        overs: Double, // represented as e.g. 3.4 (meaning 3 overs & 4 balls) OR 2.0
        runsConceded: Int,
        wickets: Int,
        maidens: Int,
        ageGroup: String
    ) {
        viewModelScope.launch {
            // Convert overs to total balls bowled inside database for precision
            val fullOvers = overs.toInt()
            val fractionalBalls = ((overs - fullOvers) * 10).toInt()
            val totalBallsBowled = (fullOvers * 6) + (if (fractionalBalls in 0..5) fractionalBalls else 0)

            val performance = MatchPerformance(
                playerId = playerId,
                opponent = opponent.trim(),
                matchFormat = matchFormat,
                ageGroup = ageGroup,
                didBat = didBat,
                runsScored = if (didBat) runsScored else 0,
                ballsFaced = if (didBat) ballsFaced else 0,
                isNotOut = if (didBat) isNotOut else false,
                fours = if (didBat) fours else 0,
                sixes = if (didBat) sixes else 0,
                didBowl = didBowl,
                ballsBowled = if (didBowl) totalBallsBowled else 0,
                runsConceded = if (didBowl) runsConceded else 0,
                wicketsTaken = if (didBowl) wickets else 0,
                maidensBowled = if (didBowl) maidens else 0
            )

            repository.insertPerformance(performance)
        }
    }

    fun deletePerformance(performance: MatchPerformance) {
        viewModelScope.launch {
            repository.deletePerformance(performance)
        }
    }

    // --- Helper class to compile compiled stats objects ---

    class PlayerCareerStats(
        val totalMatches: Int,
        val battingInnings: Int,
        val totalRuns: Int,
        val totalBallsFaced: Int,
        val totalSixes: Int,
        val totalFours: Int,
        val outCount: Int,
        val maxRuns: Int,
        val maxRunsIsNotOut: Boolean,
        val bowlingInnings: Int,
        val totalBallsBowled: Int,
        val totalRunsConceded: Int,
        val totalWickets: Int,
        val totalMaidens: Int
    ) {
        val battingAvg: String
            get() {
                return if (battingInnings == 0) "0"
                else if (outCount == 0) "N/A"
                else String.format("%.2f", totalRuns.toDouble() / outCount)
            }

        val battingStrikeRate: String
            get() {
                return if (totalBallsFaced == 0) "0.00"
                else String.format("%.2f", (totalRuns.toDouble() / totalBallsFaced) * 100)
            }

        val highestScoreFormatted: String
            get() {
                if (battingInnings == 0) return "0"
                return if (maxRunsIsNotOut) "$maxRuns*" else "$maxRuns"
            }

        val bowlingOvers: String
            get() {
                val comp = totalBallsBowled / 6
                val b = totalBallsBowled % 6
                return if (b == 0) "$comp" else "$comp.$b"
            }

        val bowlingEconomy: String
            get() {
                if (totalBallsBowled == 0) return "0.00"
                val overs = totalBallsBowled / 6.0
                return String.format("%.2f", totalRunsConceded / overs)
            }

        val bowlingAvg: String
            get() {
                if (totalWickets == 0) return "N/A"
                return String.format("%.2f", totalRunsConceded.toDouble() / totalWickets)
            }

        val bowlingStrikeRate: String
            get() {
                if (totalWickets == 0) return "N/A"
                return String.format("%.2f", totalBallsBowled.toDouble() / totalWickets)
            }
    }

    /**
     * Compute statistics for a list of performances belonging to a player/team
     */
    fun computeStats(perfList: List<MatchPerformance>): PlayerCareerStats {
        val totalMatches = perfList.size
        
        var battingInnings = 0
        var totalRuns = 0
        var totalBallsFaced = 0
        var totalSixes = 0
        var totalFours = 0
        var outCount = 0
        var maxRuns = 0
        var maxRunsIsNotOut = false

        // Bowling stats
        var bowlingInnings = 0
        var totalBallsBowled = 0
        var totalRunsConceded = 0
        var totalWickets = 0
        var totalMaidens = 0

        for (p in perfList) {
            if (p.didBat) {
                battingInnings++
                totalRuns += p.runsScored
                totalBallsFaced += p.ballsFaced
                totalSixes += p.sixes
                totalFours += p.fours
                if (!p.isNotOut) {
                    outCount++
                }
                if (p.runsScored > maxRuns) {
                    maxRuns = p.runsScored
                    maxRunsIsNotOut = p.isNotOut
                } else if (p.runsScored == maxRuns && p.isNotOut && !maxRunsIsNotOut) {
                    maxRunsIsNotOut = true
                }
            }

            if (p.didBowl) {
                bowlingInnings++
                totalBallsBowled += p.ballsBowled
                totalRunsConceded += p.runsConceded
                totalWickets += p.wicketsTaken
                totalMaidens += p.maidensBowled
            }
        }

        return PlayerCareerStats(
            totalMatches = totalMatches,
            battingInnings = battingInnings,
            totalRuns = totalRuns,
            totalBallsFaced = totalBallsFaced,
            totalSixes = totalSixes,
            totalFours = totalFours,
            outCount = outCount,
            maxRuns = maxRuns,
            maxRunsIsNotOut = maxRunsIsNotOut,
            bowlingInnings = bowlingInnings,
            totalBallsBowled = totalBallsBowled,
            totalRunsConceded = totalRunsConceded,
            totalWickets = totalWickets,
            totalMaidens = totalMaidens
        )
    }
}

class CricketViewModelFactory(
    private val application: Application,
    private val repository: CricketRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CricketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CricketViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
