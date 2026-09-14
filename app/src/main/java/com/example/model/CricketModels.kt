package com.example.model

import java.util.UUID

enum class PlayerRole(val displayName: String) {
    BATSMAN("Batsman"),
    BOWLER("Bowler"),
    ALL_ROUNDER("All-Rounder"),
    WICKET_KEEPER("Wicketkeeper")
}

enum class BattingStyle(val displayName: String) {
    RIGHT_HAND("Right-hand bat"),
    LEFT_HAND("Left-hand bat")
}

enum class BowlingStyle(val displayName: String) {
    RIGHT_ARM_FAST("Right-arm Fast"),
    RIGHT_ARM_MEDIUM("Right-arm Medium"),
    RIGHT_ARM_SPIN("Right-arm Off Spin"),
    LEFT_ARM_FAST("Left-arm Fast"),
    LEFT_ARM_SPIN("Left-arm Orthodox"),
    NONE("None")
}

data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: PlayerRole = PlayerRole.BATSMAN,
    val battingStyle: BattingStyle = BattingStyle.RIGHT_HAND,
    val bowlingStyle: BowlingStyle = BowlingStyle.NONE,
    val isCaptain: Boolean = false,
    val isWicketKeeper: Boolean = false,
    val jerseyNumber: Int = 0
)

data class Team(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shortCode: String,
    val colorHex: Long = 0xFF1B5E20,
    val players: List<Player> = emptyList(),
    val isCustom: Boolean = false
)

enum class MatchType(val defaultOvers: Int, val displayName: String) {
    T20(20, "T20 Match"),
    T10(10, "T10 Super Blitz"),
    ODI(50, "ODI One Day"),
    CUSTOM(5, "Custom Overs")
}

enum class TossDecision(val displayName: String) {
    BAT("Batting first"),
    BOWL("Bowling first")
}

enum class MatchStatus(val displayName: String) {
    UPCOMING("Upcoming"),
    LIVE("Live"),
    COMPLETED("Completed")
}

enum class ExtraType(val shortName: String, val displayName: String) {
    NONE("", "None"),
    WIDE("WD", "Wide"),
    NO_BALL("NB", "No Ball"),
    BYE("B", "Bye"),
    LEG_BYE("LB", "Leg Bye"),
    PENALTY("PEN", "Penalty")
}

enum class WicketType(val displayName: String) {
    BOWLED("Bowled"),
    CAUGHT("Caught"),
    LBW("LBW"),
    RUN_OUT("Run Out"),
    STUMPED("Stumped"),
    HIT_WICKET("Hit Wicket")
}

data class Dismissal(
    val type: WicketType,
    val outPlayerId: String,
    val bowlerId: String? = null,
    val fielderName: String? = null
) {
    fun description(playersMap: Map<String, Player>): String {
        val outName = playersMap[outPlayerId]?.name ?: "Batsman"
        val bowlerName = bowlerId?.let { playersMap[it]?.name }
        return when (type) {
            WicketType.BOWLED -> "b $bowlerName"
            WicketType.CAUGHT -> {
                if (!fielderName.isNullOrBlank()) "c $fielderName b $bowlerName" else "c & b $bowlerName"
            }
            WicketType.LBW -> "lbw b $bowlerName"
            WicketType.RUN_OUT -> {
                if (!fielderName.isNullOrBlank()) "run out ($fielderName)" else "run out"
            }
            WicketType.STUMPED -> {
                if (!fielderName.isNullOrBlank()) "st $fielderName b $bowlerName" else "stumped b $bowlerName"
            }
            WicketType.HIT_WICKET -> "hit wicket b $bowlerName"
        }
    }
}

data class BallDelivery(
    val id: String = UUID.randomUUID().toString(),
    val overNumber: Int,
    val ballNumberInOver: Int, // 1..6 for legal balls
    val bowlerId: String,
    val strikerId: String,
    val nonStrikerId: String,
    val runsBat: Int = 0,
    val extraType: ExtraType = ExtraType.NONE,
    val extraRuns: Int = 0,
    val isWicket: Boolean = false,
    val dismissal: Dismissal? = null,
    val commentary: String = "",
    val isFreeHit: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalBallRuns: Int get() = runsBat + extraRuns
    val isLegalBall: Boolean get() = extraType != ExtraType.WIDE && extraType != ExtraType.NO_BALL
    val shortNotation: String
        get() = when {
            isWicket && totalBallRuns > 0 -> "W+$totalBallRuns"
            isWicket -> "W"
            extraType == ExtraType.WIDE -> if (extraRuns > 1) "${extraRuns}WD" else "WD"
            extraType == ExtraType.NO_BALL -> when {
                runsBat > 0 -> "NB+$runsBat"
                extraRuns > 1 -> "NB+${extraRuns - 1}B"
                else -> "NB"
            }
            extraType == ExtraType.BYE -> "${extraRuns}B"
            extraType == ExtraType.LEG_BYE -> "${extraRuns}LB"
            runsBat == 4 -> "4"
            runsBat == 6 -> "6"
            runsBat == 0 -> "•"
            else -> "$runsBat"
        }
}

data class BattingStat(
    val playerId: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val dismissal: Dismissal? = null
) {
    val strikeRate: Double
        get() = if (balls > 0) (runs.toDouble() / balls) * 100.0 else 0.0
}

data class BowlingStat(
    val playerId: String,
    val legalBalls: Int = 0,
    val maidens: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0
) {
    val oversString: String
        get() {
            val overs = legalBalls / 6
            val balls = legalBalls % 6
            return "$overs.$balls"
        }
    val economy: Double
        get() = if (legalBalls > 0) (runsConceded.toDouble() / legalBalls) * 6.0 else 0.0
}

data class ExtrasBreakdown(
    val wides: Int = 0,
    val noBalls: Int = 0,
    val byes: Int = 0,
    val legByes: Int = 0,
    val penalty: Int = 0
) {
    val total: Int get() = wides + noBalls + byes + legByes + penalty
}

data class FallOfWicket(
    val wicketNumber: Int,
    val score: Int,
    val overBall: String,
    val playerId: String
)

data class Innings(
    val id: String = UUID.randomUUID().toString(),
    val battingTeamId: String,
    val bowlingTeamId: String,
    val totalRuns: Int = 0,
    val wickets: Int = 0,
    val legalBalls: Int = 0,
    val extras: ExtrasBreakdown = ExtrasBreakdown(),
    val strikerId: String = "",
    val nonStrikerId: String = "",
    val currentBowlerId: String = "",
    val battingStats: List<BattingStat> = emptyList(),
    val bowlingStats: List<BowlingStat> = emptyList(),
    val deliveries: List<BallDelivery> = emptyList(),
    val fallOfWickets: List<FallOfWicket> = emptyList(),
    val isCompleted: Boolean = false
) {
    val oversString: String
        get() {
            val overs = legalBalls / 6
            val balls = legalBalls % 6
            return "$overs.$balls"
        }
    val runRate: Double
        get() = if (legalBalls > 0) (totalRuns.toDouble() / legalBalls) * 6.0 else 0.0

    val recentBalls: List<BallDelivery>
        get() = deliveries.takeLast(12)

    val currentOverDeliveries: List<BallDelivery>
        get() {
            if (deliveries.isEmpty()) return emptyList()
            val lastOverNum = deliveries.last().overNumber
            return deliveries.filter { it.overNumber == lastOverNum }
        }
}

data class CricketMatch(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val tournament: String = "Club Championship",
    val venue: String = "National Cricket Stadium",
    val date: String = "Today",
    val teamA: Team,
    val teamB: Team,
    val oversLimit: Int = 20,
    val matchType: MatchType = MatchType.T20,
    val pitchCondition: String = "Dry Pitch (Batting Friendly)",
    val tossWinnerId: String = "",
    val tossDecision: TossDecision = TossDecision.BAT,
    val status: MatchStatus = MatchStatus.LIVE,
    val innings: List<Innings> = emptyList(),
    val currentInningsIndex: Int = 0,
    val resultText: String = ""
) {
    val currentInnings: Innings?
        get() = innings.getOrNull(currentInningsIndex)

    val battingTeam: Team
        get() {
            val battingId = currentInnings?.battingTeamId ?: teamA.id
            return if (teamA.id == battingId) teamA else teamB
        }

    val bowlingTeam: Team
        get() {
            val bowlingId = currentInnings?.bowlingTeamId ?: teamB.id
            return if (teamA.id == bowlingId) teamA else teamB
        }

    fun getPlayer(playerId: String): Player? {
        return teamA.players.find { it.id == playerId } ?: teamB.players.find { it.id == playerId }
    }

    val allPlayersMap: Map<String, Player>
        get() = (teamA.players + teamB.players).associateBy { it.id }
}

enum class StadiumTheme(val title: String, val subtitle: String, val primaryHex: Long, val accentHex: Long, val bgHex: Long) {
    EMERALD_TURF("Emerald Turf", "Traditional Lord's & MCG Atmosphere", 0xFF0D5C3A, 0xFFFFD700, 0xFF0A2218),
    SAPPHIRE_IPL("Sapphire IPL", "Electric Wankhede Night Glow", 0xFF0D47A1, 0xFF00E5FF, 0xFF0A192F),
    CRIMSON_HEAT("Crimson Heat", "Fiery Eden Gardens Derby", 0xFFB71C1C, 0xFFFF6D00, 0xFF230508),
    CYBER_AMBER("Cyber Amber", "High-Contrast Floodlight Arena", 0xFFE65100, 0xFFFFD600, 0xFF1E1914),
    ONYX_DARK("Onyx Dark", "Stealth Broadcast Studio Mode", 0xFF212121, 0xFF76FF03, 0xFF121212)
}

enum class CardMode(val title: String, val description: String) {
    BROADCAST_SCOREBUG("Broadcast Scorebug", "TV banner with prominent strike and bowler figures"),
    DETAILED_STATS("Detailed Statistics", "Comprehensive metrics with partnership and strike rates"),
    COMPACT_STRIP("Compact Strip", "Space-efficient strip for fast ball entry and clean overview")
}

data class DashboardSettings(
    val theme: StadiumTheme = StadiumTheme.EMERALD_TURF,
    val cardMode: CardMode = CardMode.BROADCAST_SCOREBUG,
    val showHeroScorecard: Boolean = true,
    val showRecentBalls: Boolean = true,
    val showPartnershipMeter: Boolean = true,
    val showBowlerFigures: Boolean = true,
    val showCommentaryFeed: Boolean = true,
    val showMiniFixtures: Boolean = true,
    val soundEnabled: Boolean = true
)

data class UserProfile(
    val id: String = "user_default",
    val displayName: String = "Official Match Scorer",
    val email: String = "scorer.cricket@gmail.com",
    val photoInitials: String = "CR",
    val isGoogleUser: Boolean = true,
    val backupDriveFolder: String = "CricLive/Backups"
)

data class BackupItem(
    val id: String = UUID.randomUUID().toString(),
    val matchTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String,
    val fileSizeKb: Int,
    val jsonContent: String,
    val matchId: String
)
