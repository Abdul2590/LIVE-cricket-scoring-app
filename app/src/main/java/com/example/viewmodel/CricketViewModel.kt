package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CricketRepository
import com.example.model.*
import com.example.util.ScorecardPdfGenerator
import com.example.util.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class CelebrationType(val title: String, val subtitle: String, val isMajor: Boolean) {
    FOUR("SUPER FOUR! 🏏", "Cracking boundary through the covers", false),
    SIX("MAXIMUM SIX! 🚀", "Cleared the boundary ropes in style!", true),
    FIFTY("HALF CENTURY! 👏", "Brilliant 50 runs milestone", true),
    CENTURY("MAGNIFICENT 100! 👑", "Sensational century performance!", true),
    WICKET("WICKET TAKEN! ⚡", "Timber & breakthrough for the bowling side", true),
    OVER_COMPLETE("OVER COMPLETED 🔄", "Time to rotate bowlers", false),
    MATCH_WON("CHAMPIONS VICTORY! 🏆", "Match sealed with victory!", true)
}

class CricketViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CricketRepository(application)

    private val _matches = MutableStateFlow<List<CricketMatch>>(emptyList())
    val matches: StateFlow<List<CricketMatch>> = _matches.asStateFlow()

    private val _activeMatch = MutableStateFlow<CricketMatch?>(null)
    val activeMatch: StateFlow<CricketMatch?> = _activeMatch.asStateFlow()

    private val _customTeams = MutableStateFlow<List<Team>>(emptyList())
    val customTeams: StateFlow<List<Team>> = _customTeams.asStateFlow()

    private val _dashboardSettings = MutableStateFlow(repository.loadDashboardSettings())
    val dashboardSettings: StateFlow<DashboardSettings> = _dashboardSettings.asStateFlow()

    private val _userProfile = MutableStateFlow(repository.loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _backups = MutableStateFlow<List<BackupItem>>(emptyList())
    val backups: StateFlow<List<BackupItem>> = _backups.asStateFlow()

    private val _celebration = MutableStateFlow<CelebrationType?>(null)
    val celebration: StateFlow<CelebrationType?> = _celebration.asStateFlow()

    // Undo stack stores snapshots of active Innings
    private val undoStack = mutableListOf<Innings>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        val preset = repository.getPresetMatches()
        _matches.value = preset
        _activeMatch.value = preset.firstOrNull { it.status == MatchStatus.LIVE } ?: preset.firstOrNull()
        _customTeams.value = repository.loadCustomTeams()
        _backups.value = repository.loadBackups()
    }

    fun dismissCelebration() {
        _celebration.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun selectActiveMatch(matchId: String) {
        val m = _matches.value.find { it.id == matchId }
        if (m != null) {
            _activeMatch.value = m
            undoStack.clear()
            _canUndo.value = false
        }
    }

    // --- Ball-by-Ball Scoring Engine ---

    fun recordBall(
        runsBat: Int,
        extraType: ExtraType,
        extraRuns: Int = 0,
        isWicket: Boolean = false,
        dismissal: Dismissal? = null,
        commentaryText: String = ""
    ) {
        val currentMatch = _activeMatch.value ?: return
        val inn = currentMatch.currentInnings ?: return

        // 1. Snapshot current innings state for multi-level Undo
        undoStack.add(inn)
        _canUndo.value = true

        val isSound = _dashboardSettings.value.soundEnabled
        val strikerId = inn.strikerId
        val nonStrikerId = inn.nonStrikerId
        val bowlerId = inn.currentBowlerId

        // Calculations
        val isLegal = (extraType != ExtraType.WIDE && extraType != ExtraType.NO_BALL)
        val ballRunsTotal = runsBat + extraRuns
        val newTotalRuns = inn.totalRuns + ballRunsTotal
        val newLegalBalls = if (isLegal) inn.legalBalls + 1 else inn.legalBalls
        val newWickets = if (isWicket) inn.wickets + 1 else inn.wickets

        // Update Extras
        val prevExtras = inn.extras
        val newExtras = when (extraType) {
            ExtraType.WIDE -> prevExtras.copy(wides = prevExtras.wides + extraRuns)
            ExtraType.NO_BALL -> {
                if (runsBat == 0 && extraRuns > 1) {
                    val byePart = extraRuns - 1
                    prevExtras.copy(noBalls = prevExtras.noBalls + 1, byes = prevExtras.byes + byePart)
                } else {
                    prevExtras.copy(noBalls = prevExtras.noBalls + extraRuns)
                }
            }
            ExtraType.BYE -> prevExtras.copy(byes = prevExtras.byes + extraRuns)
            ExtraType.LEG_BYE -> prevExtras.copy(legByes = prevExtras.legByes + extraRuns)
            ExtraType.PENALTY -> prevExtras.copy(penalty = prevExtras.penalty + extraRuns)
            ExtraType.NONE -> prevExtras
        }

        // Update Batting Stats
        val updatedBattingStats = inn.battingStats.toMutableList()
        val strikerStatIndex = updatedBattingStats.indexOfFirst { it.playerId == strikerId }
        var oldStrikerRuns = 0
        var newStrikerRuns = 0

        if (strikerStatIndex >= 0) {
            val stat = updatedBattingStats[strikerStatIndex]
            oldStrikerRuns = stat.runs
            val runsAdded = if (extraType == ExtraType.WIDE) 0 else runsBat
            val ballsFacedAdded = if (extraType == ExtraType.WIDE) 0 else 1
            val foursAdded = if (runsBat == 4 && extraType != ExtraType.BYE && extraType != ExtraType.LEG_BYE) 1 else 0
            val sixesAdded = if (runsBat == 6 && extraType != ExtraType.BYE && extraType != ExtraType.LEG_BYE) 1 else 0

            val isStrikerOut = isWicket && (dismissal == null || dismissal.outPlayerId == strikerId)
            val updatedStat = stat.copy(
                runs = stat.runs + runsAdded,
                balls = stat.balls + ballsFacedAdded,
                fours = stat.fours + foursAdded,
                sixes = stat.sixes + sixesAdded,
                isOut = isStrikerOut,
                dismissal = if (isStrikerOut) dismissal else stat.dismissal
            )
            newStrikerRuns = updatedStat.runs
            updatedBattingStats[strikerStatIndex] = updatedStat
        } else if (strikerId.isNotBlank()) {
            val runsAdded = if (extraType == ExtraType.WIDE) 0 else runsBat
            val isStrikerOut = isWicket && (dismissal == null || dismissal.outPlayerId == strikerId)
            newStrikerRuns = runsAdded
            updatedBattingStats.add(
                BattingStat(
                    playerId = strikerId,
                    runs = runsAdded,
                    balls = if (extraType == ExtraType.WIDE) 0 else 1,
                    fours = if (runsBat == 4) 1 else 0,
                    sixes = if (runsBat == 6) 1 else 0,
                    isOut = isStrikerOut,
                    dismissal = if (isStrikerOut) dismissal else null
                )
            )
        }

        // Handle Non-striker out (e.g. run out at non-striker end)
        if (isWicket && dismissal != null && dismissal.outPlayerId == nonStrikerId) {
            val nonStrikerIndex = updatedBattingStats.indexOfFirst { it.playerId == nonStrikerId }
            if (nonStrikerIndex >= 0) {
                val nsStat = updatedBattingStats[nonStrikerIndex]
                updatedBattingStats[nonStrikerIndex] = nsStat.copy(isOut = true, dismissal = dismissal)
            }
        }

        // Update Bowling Stats
        val updatedBowlingStats = inn.bowlingStats.toMutableList()
        val bowlerIndex = updatedBowlingStats.indexOfFirst { it.playerId == bowlerId }
        val bowlerRunsConceded = when (extraType) {
            ExtraType.BYE, ExtraType.LEG_BYE -> 0
            ExtraType.NO_BALL -> if (runsBat == 0 && extraRuns > 1) 1 else ballRunsTotal
            else -> ballRunsTotal
        }
        val bowlerWicketsAdded = if (isWicket && dismissal?.type != WicketType.RUN_OUT) 1 else 0

        if (bowlerIndex >= 0) {
            val bStat = updatedBowlingStats[bowlerIndex]
            updatedBowlingStats[bowlerIndex] = bStat.copy(
                legalBalls = bStat.legalBalls + if (isLegal) 1 else 0,
                runsConceded = bStat.runsConceded + bowlerRunsConceded,
                wickets = bStat.wickets + bowlerWicketsAdded,
                wides = bStat.wides + if (extraType == ExtraType.WIDE) extraRuns else 0,
                noBalls = bStat.noBalls + if (extraType == ExtraType.NO_BALL) 1 else 0
            )
        } else if (bowlerId.isNotBlank()) {
            updatedBowlingStats.add(
                BowlingStat(
                    playerId = bowlerId,
                    legalBalls = if (isLegal) 1 else 0,
                    runsConceded = bowlerRunsConceded,
                    wickets = bowlerWicketsAdded,
                    wides = if (extraType == ExtraType.WIDE) extraRuns else 0,
                    noBalls = if (extraType == ExtraType.NO_BALL) 1 else 0
                )
            )
        }

        // Fall of wicket
        val updatedFow = inn.fallOfWickets.toMutableList()
        if (isWicket) {
            val outId = dismissal?.outPlayerId ?: strikerId
            val overStr = "${newLegalBalls / 6}.${newLegalBalls % 6}"
            updatedFow.add(FallOfWicket(wicketNumber = newWickets, score = newTotalRuns, overBall = overStr, playerId = outId))
        }

        // Delivery Record
        val overNum = newLegalBalls / 6
        val ballInOver = (newLegalBalls % 6).let { if (it == 0 && isLegal) 6 else it }
        val delivery = BallDelivery(
            overNumber = if (isLegal && newLegalBalls % 6 == 0) overNum - 1 else overNum,
            ballNumberInOver = ballInOver,
            bowlerId = bowlerId,
            strikerId = strikerId,
            nonStrikerId = nonStrikerId,
            runsBat = runsBat,
            extraType = extraType,
            extraRuns = extraRuns,
            isWicket = isWicket,
            dismissal = dismissal,
            commentary = if (commentaryText.isNotBlank()) commentaryText else defaultCommentary(runsBat, extraType, isWicket, dismissal)
        )
        val updatedDeliveries = inn.deliveries + delivery

        // Strike rotation logic
        // 1. Odd runs rotated striker and non-striker
        var nextStrikerId = strikerId
        var nextNonStrikerId = nonStrikerId

        // If batsman got out, determine who comes next
        if (isWicket) {
            val battingTeam = currentMatch.battingTeam
            val outPlayerId = dismissal?.outPlayerId ?: strikerId
            val alreadyBattedIds = updatedBattingStats.map { it.playerId }.toSet()
            val nextBatsman = battingTeam.players.firstOrNull { it.id !in alreadyBattedIds }
            val incomingId = nextBatsman?.id ?: ""

            if (outPlayerId == strikerId) {
                nextStrikerId = incomingId
                // add new batsman to batting stats with 0 runs
                if (incomingId.isNotBlank() && updatedBattingStats.none { it.playerId == incomingId }) {
                    updatedBattingStats.add(BattingStat(playerId = incomingId, runs = 0, balls = 0))
                }
            } else {
                nextNonStrikerId = incomingId
                if (incomingId.isNotBlank() && updatedBattingStats.none { it.playerId == incomingId }) {
                    updatedBattingStats.add(BattingStat(playerId = incomingId, runs = 0, balls = 0))
                }
            }
        }

        val runsForRotation = when {
            extraType == ExtraType.NO_BALL && runsBat > 0 -> runsBat
            extraType == ExtraType.NO_BALL && extraRuns > 1 -> extraRuns - 1
            extraType == ExtraType.BYE || extraType == ExtraType.LEG_BYE -> extraRuns
            else -> runsBat
        }
        if (runsForRotation % 2 != 0) {
            val temp = nextStrikerId
            nextStrikerId = nextNonStrikerId
            nextNonStrikerId = temp
        }

        // End of over rotation
        val isOverFinished = isLegal && (newLegalBalls % 6 == 0)
        if (isOverFinished) {
            val temp = nextStrikerId
            nextStrikerId = nextNonStrikerId
            nextNonStrikerId = temp
        }

        // Celebrations & Audio Triggers
        if (isWicket) {
            SoundManager.playWicketAlert(isSound)
            _celebration.value = CelebrationType.WICKET
        } else if (runsBat == 6) {
            SoundManager.playSixFanfare(isSound)
            _celebration.value = CelebrationType.SIX
        } else if (runsBat == 4) {
            SoundManager.playBoundaryChime(isSound)
            _celebration.value = CelebrationType.FOUR
        } else if (isOverFinished) {
            SoundManager.playUmpireWhistle(isSound)
            _celebration.value = CelebrationType.OVER_COMPLETE
        } else {
            SoundManager.playBatCrack(isSound)
        }

        if (oldStrikerRuns < 50 && newStrikerRuns >= 50) {
            _celebration.value = CelebrationType.FIFTY
        } else if (oldStrikerRuns < 100 && newStrikerRuns >= 100) {
            _celebration.value = CelebrationType.CENTURY
        }

        // Check Innings or Match Completion
        val maxBalls = currentMatch.oversLimit * 6
        val allOut = newWickets >= (currentMatch.battingTeam.players.size - 1)
        val isInningsOver = newLegalBalls >= maxBalls || allOut

        val updatedInnings = inn.copy(
            totalRuns = newTotalRuns,
            wickets = newWickets,
            legalBalls = newLegalBalls,
            extras = newExtras,
            strikerId = nextStrikerId,
            nonStrikerId = nextNonStrikerId,
            battingStats = updatedBattingStats,
            bowlingStats = updatedBowlingStats,
            deliveries = updatedDeliveries,
            fallOfWickets = updatedFow,
            isCompleted = isInningsOver
        )

        val updatedInningsList = currentMatch.innings.toMutableList()
        updatedInningsList[currentMatch.currentInningsIndex] = updatedInnings

        val newMatch = currentMatch.copy(
            innings = updatedInningsList,
            status = if (isInningsOver && currentMatch.currentInningsIndex >= 1) MatchStatus.COMPLETED else currentMatch.status
        )

        _activeMatch.value = newMatch
        updateMatchInList(newMatch)
    }

    // --- Multi-Level Undo ---
    fun undoLastBall() {
        if (undoStack.isEmpty()) {
            _canUndo.value = false
            return
        }

        val previousInnings = undoStack.removeAt(undoStack.lastIndex)
        _canUndo.value = undoStack.isNotEmpty()

        val currentMatch = _activeMatch.value ?: return
        val updatedInningsList = currentMatch.innings.toMutableList()
        updatedInningsList[currentMatch.currentInningsIndex] = previousInnings

        val newMatch = currentMatch.copy(innings = updatedInningsList)
        _activeMatch.value = newMatch
        updateMatchInList(newMatch)

        SoundManager.playUndoTone(_dashboardSettings.value.soundEnabled)
        _statusMessage.value = "Undid last ball entry"
    }

    // --- Batsman and Bowler Controls (Change Batsman Before Scoring or Anytime) ---

    fun changeStrikerAndNonStriker(strikerId: String, nonStrikerId: String) {
        val currentMatch = _activeMatch.value ?: return
        val inn = currentMatch.currentInnings ?: return

        val battingStats = inn.battingStats.toMutableList()
        if (strikerId.isNotBlank() && battingStats.none { it.playerId == strikerId }) {
            battingStats.add(BattingStat(playerId = strikerId))
        }
        if (nonStrikerId.isNotBlank() && battingStats.none { it.playerId == nonStrikerId }) {
            battingStats.add(BattingStat(playerId = nonStrikerId))
        }

        val updatedInnings = inn.copy(
            strikerId = strikerId,
            nonStrikerId = nonStrikerId,
            battingStats = battingStats
        )
        val updatedInningsList = currentMatch.innings.toMutableList()
        updatedInningsList[currentMatch.currentInningsIndex] = updatedInnings
        val newMatch = currentMatch.copy(innings = updatedInningsList)
        _activeMatch.value = newMatch
        updateMatchInList(newMatch)
        _statusMessage.value = "Active batsmen updated"
    }

    fun rotateStrike() {
        val currentMatch = _activeMatch.value ?: return
        val inn = currentMatch.currentInnings ?: return
        val updatedInnings = inn.copy(
            strikerId = inn.nonStrikerId,
            nonStrikerId = inn.strikerId
        )
        val updatedInningsList = currentMatch.innings.toMutableList()
        updatedInningsList[currentMatch.currentInningsIndex] = updatedInnings
        val newMatch = currentMatch.copy(innings = updatedInningsList)
        _activeMatch.value = newMatch
        updateMatchInList(newMatch)
        SoundManager.playBatCrack(_dashboardSettings.value.soundEnabled)
        _statusMessage.value = "Strike rotated"
    }

    fun changeBowler(bowlerId: String) {
        val currentMatch = _activeMatch.value ?: return
        val inn = currentMatch.currentInnings ?: return

        val bowlingStats = inn.bowlingStats.toMutableList()
        if (bowlerId.isNotBlank() && bowlingStats.none { it.playerId == bowlerId }) {
            bowlingStats.add(BowlingStat(playerId = bowlerId))
        }

        val updatedInnings = inn.copy(
            currentBowlerId = bowlerId,
            bowlingStats = bowlingStats
        )
        val updatedInningsList = currentMatch.innings.toMutableList()
        updatedInningsList[currentMatch.currentInningsIndex] = updatedInnings
        val newMatch = currentMatch.copy(innings = updatedInningsList)
        _activeMatch.value = newMatch
        updateMatchInList(newMatch)
        _statusMessage.value = "Bowler changed"
    }

    // --- Edit Player Names & Details ---

    fun editPlayerName(teamId: String, playerId: String, newName: String) {
        if (newName.isBlank()) return
        val currentMatch = _activeMatch.value

        // Update in Custom Teams if present
        val teams = _customTeams.value.map { team ->
            if (team.id == teamId) {
                team.copy(players = team.players.map { if (it.id == playerId) it.copy(name = newName) else it })
            } else team
        }
        _customTeams.value = teams
        repository.saveCustomTeams(teams)

        // Update in current match
        if (currentMatch != null) {
            val updatedTeamA = if (currentMatch.teamA.id == teamId) {
                currentMatch.teamA.copy(players = currentMatch.teamA.players.map { if (it.id == playerId) it.copy(name = newName) else it })
            } else currentMatch.teamA

            val updatedTeamB = if (currentMatch.teamB.id == teamId) {
                currentMatch.teamB.copy(players = currentMatch.teamB.players.map { if (it.id == playerId) it.copy(name = newName) else it })
            } else currentMatch.teamB

            val updatedMatch = currentMatch.copy(teamA = updatedTeamA, teamB = updatedTeamB)
            _activeMatch.value = updatedMatch
            updateMatchInList(updatedMatch)
        }
        _statusMessage.value = "Player name updated to $newName"
    }

    fun updatePlayerDetails(teamId: String, player: Player) {
        val teams = _customTeams.value.map { team ->
            if (team.id == teamId) {
                team.copy(players = team.players.map { if (it.id == player.id) player else it })
            } else team
        }
        _customTeams.value = teams
        repository.saveCustomTeams(teams)

        val currentMatch = _activeMatch.value
        if (currentMatch != null) {
            val updatedTeamA = if (currentMatch.teamA.id == teamId) {
                currentMatch.teamA.copy(players = currentMatch.teamA.players.map { if (it.id == player.id) player else it })
            } else currentMatch.teamA

            val updatedTeamB = if (currentMatch.teamB.id == teamId) {
                currentMatch.teamB.copy(players = currentMatch.teamB.players.map { if (it.id == player.id) player else it })
            } else currentMatch.teamB

            val updatedMatch = currentMatch.copy(teamA = updatedTeamA, teamB = updatedTeamB)
            _activeMatch.value = updatedMatch
            updateMatchInList(updatedMatch)
        }
        _statusMessage.value = "Updated ${player.name}'s details"
    }

    // --- Custom Teams Management ---

    fun saveCustomTeam(team: Team) {
        val currentTeams = _customTeams.value.toMutableList()
        val index = currentTeams.indexOfFirst { it.id == team.id }
        if (index >= 0) {
            currentTeams[index] = team.copy(isCustom = true)
        } else {
            currentTeams.add(team.copy(isCustom = true))
        }
        _customTeams.value = currentTeams
        repository.saveCustomTeams(currentTeams)
        _statusMessage.value = "Team ${team.name} saved successfully"
    }

    fun deleteCustomTeam(teamId: String) {
        val teamToDelete = _customTeams.value.find { it.id == teamId }
        val currentTeams = _customTeams.value.filter { it.id != teamId }
        _customTeams.value = currentTeams
        repository.saveCustomTeams(currentTeams)
        _statusMessage.value = "Team '${teamToDelete?.name ?: "Custom"}' deleted"
    }

    fun deleteAllCustomTeams() {
        _customTeams.value = emptyList()
        repository.saveCustomTeams(emptyList())
        _statusMessage.value = "All custom teams removed"
    }

    fun resetCustomTeamsToDefault() {
        val defaults = repository.getDefaultCustomTeams()
        _customTeams.value = defaults
        repository.saveCustomTeams(defaults)
        _statusMessage.value = "Reset teams to default custom squads"
    }

    fun exportTeamsJson(): String {
        return repository.serializeTeamsToJson(_customTeams.value)
    }

    fun importTeamsJson(jsonString: String, replaceExisting: Boolean): Result<Int> {
        return try {
            val parsedTeams = repository.parseTeamsJson(jsonString)
            if (parsedTeams.isEmpty()) {
                return Result.failure(IllegalArgumentException("No valid teams found in the provided JSON."))
            }
            val finalTeams = if (replaceExisting) {
                parsedTeams
            } else {
                val existingIds = _customTeams.value.map { it.id }.toSet()
                val existingNames = _customTeams.value.map { it.name.lowercase().trim() }.toSet()
                val uniqueNew = parsedTeams.filter { it.id !in existingIds && it.name.lowercase().trim() !in existingNames }
                _customTeams.value + uniqueNew
            }
            _customTeams.value = finalTeams
            repository.saveCustomTeams(finalTeams)
            _statusMessage.value = "Successfully imported ${parsedTeams.size} team(s)"
            Result.success(parsedTeams.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Fix Matches & Fixture Creation ---

    fun createFixture(
        title: String,
        tournament: String,
        venue: String,
        teamA: Team,
        teamB: Team,
        matchType: MatchType,
        customOvers: Int,
        pitchCondition: String,
        tossWinnerId: String,
        tossDecision: TossDecision
    ) {
        val overs = if (matchType == MatchType.CUSTOM) customOvers else matchType.defaultOvers
        val battingTeam = if (tossWinnerId == teamA.id) {
            if (tossDecision == TossDecision.BAT) teamA else teamB
        } else {
            if (tossDecision == TossDecision.BAT) teamB else teamA
        }
        val bowlingTeam = if (battingTeam.id == teamA.id) teamB else teamA

        val striker = battingTeam.players.getOrNull(0)?.id ?: ""
        val nonStriker = battingTeam.players.getOrNull(1)?.id ?: ""
        val bowler = bowlingTeam.players.getOrNull(10)?.id ?: bowlingTeam.players.getOrNull(0)?.id ?: ""

        val initialBattingStats = listOfNotNull(
            striker.takeIf { it.isNotBlank() }?.let { BattingStat(playerId = it) },
            nonStriker.takeIf { it.isNotBlank() }?.let { BattingStat(playerId = it) }
        )
        val initialBowlingStats = listOfNotNull(
            bowler.takeIf { it.isNotBlank() }?.let { BowlingStat(playerId = it) }
        )

        val newInnings = Innings(
            battingTeamId = battingTeam.id,
            bowlingTeamId = bowlingTeam.id,
            strikerId = striker,
            nonStrikerId = nonStriker,
            currentBowlerId = bowler,
            battingStats = initialBattingStats,
            bowlingStats = initialBowlingStats
        )

        val newMatch = CricketMatch(
            id = "match_${System.currentTimeMillis()}",
            title = title,
            tournament = tournament,
            venue = venue,
            teamA = teamA,
            teamB = teamB,
            oversLimit = overs,
            matchType = matchType,
            pitchCondition = pitchCondition,
            tossWinnerId = tossWinnerId,
            tossDecision = tossDecision,
            status = MatchStatus.LIVE,
            innings = listOf(newInnings),
            currentInningsIndex = 0,
            resultText = "${if (tossWinnerId == teamA.id) teamA.name else teamB.name} elected to ${tossDecision.displayName.lowercase()}"
        )

        val list = listOf(newMatch) + _matches.value
        _matches.value = list
        _activeMatch.value = newMatch
        undoStack.clear()
        _canUndo.value = false
        _statusMessage.value = "New fixture created: $title"
    }

    fun deleteFixture(matchId: String) {
        val currentMatches = _matches.value.toMutableList()
        val matchIndex = currentMatches.indexOfFirst { it.id == matchId }
        if (matchIndex >= 0) {
            val deleted = currentMatches.removeAt(matchIndex)
            _matches.value = currentMatches
            if (_activeMatch.value?.id == matchId) {
                _activeMatch.value = currentMatches.firstOrNull()
                undoStack.clear()
                _canUndo.value = false
            }
            _statusMessage.value = "Fixture '${deleted.teamA.shortCode} vs ${deleted.teamB.shortCode}' deleted"
        }
    }

    fun clearCompletedFixtures() {
        val current = _matches.value
        val completedCount = current.count { it.status == MatchStatus.COMPLETED }
        val remaining = current.filter { it.status != MatchStatus.COMPLETED }
        _matches.value = remaining
        if (_activeMatch.value?.status == MatchStatus.COMPLETED) {
            _activeMatch.value = remaining.firstOrNull()
            undoStack.clear()
            _canUndo.value = false
        }
        _statusMessage.value = "Cleared $completedCount completed fixture(s)"
    }

    // --- Dashboard Settings ---

    fun updateTheme(theme: StadiumTheme) {
        val s = _dashboardSettings.value.copy(theme = theme)
        _dashboardSettings.value = s
        repository.saveDashboardSettings(s)
    }

    fun updateCardMode(mode: CardMode) {
        val s = _dashboardSettings.value.copy(cardMode = mode)
        _dashboardSettings.value = s
        repository.saveDashboardSettings(s)
    }

    fun toggleWidget(widgetKey: String) {
        val cur = _dashboardSettings.value
        val updated = when (widgetKey) {
            "hero" -> cur.copy(showHeroScorecard = !cur.showHeroScorecard)
            "recent" -> cur.copy(showRecentBalls = !cur.showRecentBalls)
            "partner" -> cur.copy(showPartnershipMeter = !cur.showPartnershipMeter)
            "bowler" -> cur.copy(showBowlerFigures = !cur.showBowlerFigures)
            "commentary" -> cur.copy(showCommentaryFeed = !cur.showCommentaryFeed)
            "fixtures" -> cur.copy(showMiniFixtures = !cur.showMiniFixtures)
            "sound" -> cur.copy(soundEnabled = !cur.soundEnabled)
            else -> cur
        }
        _dashboardSettings.value = updated
        repository.saveDashboardSettings(updated)
    }

    // --- Google Account Login / Switch User ---

    fun switchGoogleUser(name: String, email: String) {
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        val profile = UserProfile(
            id = "google_user_${System.currentTimeMillis()}",
            displayName = name,
            email = email,
            photoInitials = if (initials.isNotBlank()) initials else "GU",
            isGoogleUser = true
        )
        _userProfile.value = profile
        repository.saveUserProfile(profile)
        _statusMessage.value = "Logged in as ${profile.displayName}"
    }

    // --- Google Drive Cloud Backup & Restore ---

    fun backupToDrive() {
        val match = _activeMatch.value ?: return
        val json = serializeMatchToJson(match)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val sizeKb = (json.length / 1024).coerceAtLeast(1)

        val item = BackupItem(
            matchTitle = "${match.teamA.shortCode} vs ${match.teamB.shortCode} (${match.tournament})",
            timestamp = System.currentTimeMillis(),
            formattedDate = dateFmt,
            fileSizeKb = sizeKb,
            jsonContent = json,
            matchId = match.id
        )

        val list = listOf(item) + _backups.value
        _backups.value = list
        repository.saveBackups(list)
        _statusMessage.value = "Match backed up to Google Drive (JSON)"
    }

    fun restoreFromBackup(backup: BackupItem) {
        try {
            val restored = deserializeMatchFromJson(backup.jsonContent)
            if (restored != null) {
                val list = _matches.value.toMutableList()
                val idx = list.indexOfFirst { it.id == restored.id }
                if (idx >= 0) list[idx] = restored else list.add(0, restored)
                _matches.value = list
                _activeMatch.value = restored
                undoStack.clear()
                _canUndo.value = false
                _statusMessage.value = "Restored match session from cloud backup"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Failed to restore backup"
        }
    }

    fun deleteBackup(backupId: String) {
        val list = _backups.value.filter { it.id != backupId }
        _backups.value = list
        repository.saveBackups(list)
        _statusMessage.value = "Cloud backup deleted"
    }

    // --- PDF Scorecard Download & Sharing ---

    fun downloadPdfScorecard(context: Context): File? {
        val match = _activeMatch.value ?: return null
        val file = ScorecardPdfGenerator.generateScorecardPdf(context, match)
        if (file != null) {
            _statusMessage.value = "Scorecard PDF downloaded: ${file.name}"
            ScorecardPdfGenerator.openOrSharePdf(context, file)
        } else {
            _statusMessage.value = "Could not generate PDF"
        }
        return file
    }

    // --- Helper Serialization Methods ---

    private fun updateMatchInList(updated: CricketMatch) {
        val list = _matches.value.toMutableList()
        val index = list.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            list[index] = updated
        } else {
            list.add(0, updated)
        }
        _matches.value = list
    }

    private fun defaultCommentary(runs: Int, extraType: ExtraType, isWicket: Boolean, dismissal: Dismissal?): String {
        return when {
            isWicket -> "OUT! Dismissal: ${dismissal?.type?.displayName ?: "Wicket"}. Major breakthrough!"
            extraType == ExtraType.WIDE -> "Wide ball called by umpire."
            extraType == ExtraType.NO_BALL -> "No ball! Free hit signaled next ball."
            runs == 6 -> "HUGE SIX! Struck cleanly into the grandstand!"
            runs == 4 -> "FOUR! Driven delightfully through the infield gap."
            runs == 0 -> "Defended softly onto the turf, no run taken."
            runs == 1 -> "Quick single taken with sharp calling between wickets."
            runs == 2 -> "Pushed into the deep gap, brisk two runs completed."
            else -> "$runs runs scored off the delivery."
        }
    }

    private fun serializeMatchToJson(m: CricketMatch): String {
        val obj = JSONObject()
        obj.put("id", m.id)
        obj.put("title", m.title)
        obj.put("tournament", m.tournament)
        obj.put("venue", m.venue)
        obj.put("oversLimit", m.oversLimit)
        obj.put("resultText", m.resultText)
        obj.put("teamA_name", m.teamA.name)
        obj.put("teamA_code", m.teamA.shortCode)
        obj.put("teamB_name", m.teamB.name)
        obj.put("teamB_code", m.teamB.shortCode)
        m.currentInnings?.let { inn ->
            obj.put("runs", inn.totalRuns)
            obj.put("wickets", inn.wickets)
            obj.put("legalBalls", inn.legalBalls)
            obj.put("overs", inn.oversString)
        }
        return obj.toString()
    }

    private fun deserializeMatchFromJson(jsonStr: String): CricketMatch? {
        return try {
            val obj = JSONObject(jsonStr)
            val matchId = obj.getString("id")
            val existing = _matches.value.find { it.id == matchId }
            if (existing != null) return existing

            val teamA = Team(name = obj.getString("teamA_name"), shortCode = obj.getString("teamA_code"))
            val teamB = Team(name = obj.getString("teamB_name"), shortCode = obj.getString("teamB_code"))
            CricketMatch(
                id = matchId,
                title = obj.getString("title"),
                tournament = obj.optString("tournament", "Championship"),
                venue = obj.optString("venue", "Stadium"),
                teamA = teamA,
                teamB = teamB,
                oversLimit = obj.optInt("oversLimit", 20),
                status = MatchStatus.LIVE,
                resultText = obj.optString("resultText", "")
            )
        } catch (e: Exception) {
            null
        }
    }
}
