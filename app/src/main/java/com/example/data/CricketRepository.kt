package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CricketRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("criclive_storage", Context.MODE_PRIVATE)

    // Saved Custom Teams
    fun loadCustomTeams(): List<Team> {
        val jsonStr = prefs.getString("saved_custom_teams", null)
        if (jsonStr.isNullOrBlank()) {
            val defaults = getDefaultCustomTeams()
            saveCustomTeams(defaults)
            return defaults
        }
        return try {
            parseTeamsJson(jsonStr)
        } catch (e: Exception) {
            getDefaultCustomTeams()
        }
    }

    fun serializeTeamsToJson(teams: List<Team>): String {
        val jsonArray = JSONArray()
        teams.forEach { team ->
            val tObj = JSONObject()
            tObj.put("id", team.id)
            tObj.put("name", team.name)
            tObj.put("shortCode", team.shortCode)
            tObj.put("colorHex", team.colorHex)
            tObj.put("isCustom", team.isCustom)

            val pArray = JSONArray()
            team.players.forEach { p ->
                val pObj = JSONObject()
                pObj.put("id", p.id)
                pObj.put("name", p.name)
                pObj.put("role", p.role.name)
                pObj.put("battingStyle", p.battingStyle.name)
                pObj.put("bowlingStyle", p.bowlingStyle.name)
                pObj.put("isCaptain", p.isCaptain)
                pObj.put("isWicketKeeper", p.isWicketKeeper)
                pObj.put("jerseyNumber", p.jerseyNumber)
                pArray.put(pObj)
            }
            tObj.put("players", pArray)
            jsonArray.put(tObj)
        }
        return jsonArray.toString(2)
    }

    fun saveCustomTeams(teams: List<Team>) {
        val jsonStr = serializeTeamsToJson(teams)
        prefs.edit().putString("saved_custom_teams", jsonStr).apply()
    }

    // Dashboard Settings
    fun loadDashboardSettings(): DashboardSettings {
        val themeName = prefs.getString("dash_theme", StadiumTheme.EMERALD_TURF.name) ?: StadiumTheme.EMERALD_TURF.name
        val modeName = prefs.getString("dash_mode", CardMode.BROADCAST_SCOREBUG.name) ?: CardMode.BROADCAST_SCOREBUG.name
        val theme = try { StadiumTheme.valueOf(themeName) } catch (_: Exception) { StadiumTheme.EMERALD_TURF }
        val cardMode = try { CardMode.valueOf(modeName) } catch (_: Exception) { CardMode.BROADCAST_SCOREBUG }

        return DashboardSettings(
            theme = theme,
            cardMode = cardMode,
            showHeroScorecard = prefs.getBoolean("dash_hero", true),
            showRecentBalls = prefs.getBoolean("dash_recent", true),
            showPartnershipMeter = prefs.getBoolean("dash_partner", true),
            showBowlerFigures = prefs.getBoolean("dash_bowler", true),
            showCommentaryFeed = prefs.getBoolean("dash_commentary", true),
            showMiniFixtures = prefs.getBoolean("dash_fixtures", true),
            soundEnabled = prefs.getBoolean("dash_sound", true)
        )
    }

    fun saveDashboardSettings(settings: DashboardSettings) {
        prefs.edit()
            .putString("dash_theme", settings.theme.name)
            .putString("dash_mode", settings.cardMode.name)
            .putBoolean("dash_hero", settings.showHeroScorecard)
            .putBoolean("dash_recent", settings.showRecentBalls)
            .putBoolean("dash_partner", settings.showPartnershipMeter)
            .putBoolean("dash_bowler", settings.showBowlerFigures)
            .putBoolean("dash_commentary", settings.showCommentaryFeed)
            .putBoolean("dash_fixtures", settings.showMiniFixtures)
            .putBoolean("dash_sound", settings.soundEnabled)
            .apply()
    }

    // User Profile (Google Account)
    fun loadUserProfile(): UserProfile {
        return UserProfile(
            id = prefs.getString("user_id", "user_google_1") ?: "user_google_1",
            displayName = prefs.getString("user_name", "Rehman Shaikh (Lead Scorer)") ?: "Rehman Shaikh (Lead Scorer)",
            email = prefs.getString("user_email", "rehman.shaikh4@gmail.com") ?: "rehman.shaikh4@gmail.com",
            photoInitials = prefs.getString("user_initials", "RS") ?: "RS",
            isGoogleUser = prefs.getBoolean("user_is_google", true)
        )
    }

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString("user_id", profile.id)
            .putString("user_name", profile.displayName)
            .putString("user_email", profile.email)
            .putString("user_initials", profile.photoInitials)
            .putBoolean("user_is_google", profile.isGoogleUser)
            .apply()
    }

    // Backups
    fun loadBackups(): List<BackupItem> {
        val jsonStr = prefs.getString("cloud_backups", null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<BackupItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BackupItem(
                        id = obj.getString("id"),
                        matchTitle = obj.getString("matchTitle"),
                        timestamp = obj.getLong("timestamp"),
                        formattedDate = obj.getString("formattedDate"),
                        fileSizeKb = obj.getInt("fileSizeKb"),
                        jsonContent = obj.getString("jsonContent"),
                        matchId = obj.getString("matchId")
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveBackups(list: List<BackupItem>) {
        val arr = JSONArray()
        list.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("matchTitle", b.matchTitle)
            obj.put("timestamp", b.timestamp)
            obj.put("formattedDate", b.formattedDate)
            obj.put("fileSizeKb", b.fileSizeKb)
            obj.put("jsonContent", b.jsonContent)
            obj.put("matchId", b.matchId)
            arr.put(obj)
        }
        prefs.edit().putString("cloud_backups", arr.toString()).apply()
    }

    fun parseTeamsJson(jsonStr: String): List<Team> {
        val trimmed = jsonStr.trim()
        val arr = if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            when {
                root.has("teams") -> root.getJSONArray("teams")
                root.has("custom_teams") -> root.getJSONArray("custom_teams")
                root.has("data") -> root.getJSONArray("data")
                else -> JSONArray()
            }
        } else {
            JSONArray(trimmed)
        }

        val teams = mutableListOf<Team>()
        for (i in 0 until arr.length()) {
            val tObj = arr.getJSONObject(i)
            val pArr = tObj.optJSONArray("players")
            val players = mutableListOf<Player>()
            if (pArr != null) {
                for (j in 0 until pArr.length()) {
                    val pItem = pArr.get(j)
                    if (pItem is JSONObject) {
                        players.add(
                            Player(
                                id = pItem.optString("id", UUID.randomUUID().toString()),
                                name = pItem.optString("name", "Player ${j + 1}"),
                                role = try { PlayerRole.valueOf(pItem.optString("role", PlayerRole.BATSMAN.name)) } catch (_: Exception) { PlayerRole.BATSMAN },
                                battingStyle = try { BattingStyle.valueOf(pItem.optString("battingStyle", BattingStyle.RIGHT_HAND.name)) } catch (_: Exception) { BattingStyle.RIGHT_HAND },
                                bowlingStyle = try { BowlingStyle.valueOf(pItem.optString("bowlingStyle", BowlingStyle.NONE.name)) } catch (_: Exception) { BowlingStyle.NONE },
                                isCaptain = pItem.optBoolean("isCaptain", false),
                                isWicketKeeper = pItem.optBoolean("isWicketKeeper", false),
                                jerseyNumber = pItem.optInt("jerseyNumber", j + 1)
                            )
                        )
                    } else if (pItem is String && pItem.isNotBlank()) {
                        players.add(
                            Player(
                                id = UUID.randomUUID().toString(),
                                name = pItem.trim(),
                                jerseyNumber = j + 1
                            )
                        )
                    }
                }
            }

            val teamName = tObj.optString("name", "Team ${i + 1}")
            val shortCode = tObj.optString("shortCode", teamName.take(3).uppercase())
            teams.add(
                Team(
                    id = tObj.optString("id", UUID.randomUUID().toString()),
                    name = teamName,
                    shortCode = shortCode,
                    colorHex = tObj.optLong("colorHex", 0xFF1B5E20),
                    players = players,
                    isCustom = true
                )
            )
        }
        return teams
    }

    fun getDefaultCustomTeams(): List<Team> {
        return listOf(
            Team(
                id = "custom_team_1",
                name = "Apex Gladiators",
                shortCode = "AGL",
                colorHex = 0xFF8E24AA,
                isCustom = true,
                players = listOf(
                    Player("agl_1", "Aryan Sharma", PlayerRole.BATSMAN, BattingStyle.RIGHT_HAND, BowlingStyle.NONE, isCaptain = true, jerseyNumber = 18),
                    Player("agl_2", "David Warner Jr.", PlayerRole.BATSMAN, BattingStyle.LEFT_HAND, BowlingStyle.NONE, jerseyNumber = 31),
                    Player("agl_3", "Marcus Stoinis", PlayerRole.ALL_ROUNDER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_MEDIUM, jerseyNumber = 17),
                    Player("agl_4", "Rishabh Pant (wk)", PlayerRole.WICKET_KEEPER, BattingStyle.LEFT_HAND, BowlingStyle.NONE, isWicketKeeper = true, jerseyNumber = 77),
                    Player("agl_5", "Hardik Pandya", PlayerRole.ALL_ROUNDER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_FAST, jerseyNumber = 33),
                    Player("agl_6", "Rashid Khan", PlayerRole.ALL_ROUNDER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_SPIN, jerseyNumber = 19),
                    Player("agl_7", "Jasprit Bumrah", PlayerRole.BOWLER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_FAST, jerseyNumber = 93),
                    Player("agl_8", "Shaheen Afridi", PlayerRole.BOWLER, BattingStyle.LEFT_HAND, BowlingStyle.LEFT_ARM_FAST, jerseyNumber = 10),
                    Player("agl_9", "Kagiso Rabada", PlayerRole.BOWLER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_FAST, jerseyNumber = 25),
                    Player("agl_10", "Adam Zampa", PlayerRole.BOWLER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_SPIN, jerseyNumber = 88),
                    Player("agl_11", "Glenn Maxwell", PlayerRole.ALL_ROUNDER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_SPIN, jerseyNumber = 32)
                )
            ),
            Team(
                id = "custom_team_2",
                name = "Strikers United",
                shortCode = "STU",
                colorHex = 0xFF0288D1,
                isCustom = true,
                players = listOf(
                    Player("stu_1", "Babar Azam", PlayerRole.BATSMAN, BattingStyle.RIGHT_HAND, BowlingStyle.NONE, isCaptain = true, jerseyNumber = 56),
                    Player("stu_2", "Travis Head", PlayerRole.BATSMAN, BattingStyle.LEFT_HAND, BowlingStyle.RIGHT_ARM_SPIN, jerseyNumber = 62),
                    Player("stu_3", "Virat Kohli", PlayerRole.BATSMAN, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_MEDIUM, jerseyNumber = 18),
                    Player("stu_4", "Jos Buttler (wk)", PlayerRole.WICKET_KEEPER, BattingStyle.RIGHT_HAND, BowlingStyle.NONE, isWicketKeeper = true, jerseyNumber = 63),
                    Player("stu_5", "Heinrich Klaasen", PlayerRole.BATSMAN, BattingStyle.RIGHT_HAND, BowlingStyle.NONE, jerseyNumber = 45),
                    Player("stu_6", "Andre Russell", PlayerRole.ALL_ROUNDER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_FAST, jerseyNumber = 12),
                    Player("stu_7", "Ravindra Jadeja", PlayerRole.ALL_ROUNDER, BattingStyle.LEFT_HAND, BowlingStyle.LEFT_ARM_SPIN, jerseyNumber = 8),
                    Player("stu_8", "Mitchell Starc", PlayerRole.BOWLER, BattingStyle.LEFT_HAND, BowlingStyle.LEFT_ARM_FAST, jerseyNumber = 56),
                    Player("stu_9", "Trent Boult", PlayerRole.BOWLER, BattingStyle.RIGHT_HAND, BowlingStyle.LEFT_ARM_FAST, jerseyNumber = 18),
                    Player("stu_10", "Kuldeep Yadav", PlayerRole.BOWLER, BattingStyle.LEFT_HAND, BowlingStyle.LEFT_ARM_SPIN, jerseyNumber = 23),
                    Player("stu_11", "Jofra Archer", PlayerRole.BOWLER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_FAST, jerseyNumber = 22)
                )
            )
        )
    }

    fun getPresetMatches(): List<CricketMatch> {
        val teamInd = Team(
            id = "team_ind",
            name = "India",
            shortCode = "IND",
            colorHex = 0xFF1976D2,
            players = listOf(
                Player("ind_1", "Rohit Sharma", PlayerRole.BATSMAN, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_SPIN, isCaptain = true, jerseyNumber = 45),
                Player("ind_2", "Shubman Gill", PlayerRole.BATSMAN, BattingStyle.RIGHT_HAND, BowlingStyle.NONE, jerseyNumber = 77),
                Player("ind_3", "Virat Kohli", PlayerRole.BATSMAN, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_MEDIUM, jerseyNumber = 18),
                Player("ind_4", "Suryakumar Yadav", PlayerRole.BATSMAN, BattingStyle.RIGHT_HAND, BowlingStyle.NONE, jerseyNumber = 63),
                Player("ind_5", "Rishabh Pant", PlayerRole.WICKET_KEEPER, BattingStyle.LEFT_HAND, BowlingStyle.NONE, isWicketKeeper = true, jerseyNumber = 17),
                Player("ind_6", "Hardik Pandya", PlayerRole.ALL_ROUNDER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_FAST, jerseyNumber = 33),
                Player("ind_7", "Ravindra Jadeja", PlayerRole.ALL_ROUNDER, BattingStyle.LEFT_HAND, BowlingStyle.LEFT_ARM_SPIN, jerseyNumber = 8),
                Player("ind_8", "Axar Patel", PlayerRole.ALL_ROUNDER, BattingStyle.LEFT_HAND, BowlingStyle.LEFT_ARM_SPIN, jerseyNumber = 20),
                Player("ind_9", "Kuldeep Yadav", PlayerRole.BOWLER, BattingStyle.LEFT_HAND, BowlingStyle.LEFT_ARM_SPIN, jerseyNumber = 23),
                Player("ind_10", "Jasprit Bumrah", PlayerRole.BOWLER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_FAST, jerseyNumber = 93),
                Player("ind_11", "Arshdeep Singh", PlayerRole.BOWLER, BattingStyle.LEFT_HAND, BowlingStyle.LEFT_ARM_FAST, jerseyNumber = 2)
            )
        )

        val teamAus = Team(
            id = "team_aus",
            name = "Australia",
            shortCode = "AUS",
            colorHex = 0xFFFBC02D,
            players = listOf(
                Player("aus_1", "Travis Head", PlayerRole.BATSMAN, BattingStyle.LEFT_HAND, BowlingStyle.RIGHT_ARM_SPIN, jerseyNumber = 62),
                Player("aus_2", "David Warner", PlayerRole.BATSMAN, BattingStyle.LEFT_HAND, BowlingStyle.NONE, jerseyNumber = 31),
                Player("aus_3", "Mitchell Marsh", PlayerRole.ALL_ROUNDER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_MEDIUM, isCaptain = true, jerseyNumber = 8),
                Player("aus_4", "Glenn Maxwell", PlayerRole.ALL_ROUNDER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_SPIN, jerseyNumber = 32),
                Player("aus_5", "Marcus Stoinis", PlayerRole.ALL_ROUNDER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_MEDIUM, jerseyNumber = 17),
                Player("aus_6", "Tim David", PlayerRole.BATSMAN, BattingStyle.RIGHT_HAND, BowlingStyle.NONE, jerseyNumber = 85),
                Player("aus_7", "Matthew Wade", PlayerRole.WICKET_KEEPER, BattingStyle.LEFT_HAND, BowlingStyle.NONE, isWicketKeeper = true, jerseyNumber = 13),
                Player("aus_8", "Pat Cummins", PlayerRole.BOWLER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_FAST, jerseyNumber = 30),
                Player("aus_9", "Mitchell Starc", PlayerRole.BOWLER, BattingStyle.LEFT_HAND, BowlingStyle.LEFT_ARM_FAST, jerseyNumber = 56),
                Player("aus_10", "Adam Zampa", PlayerRole.BOWLER, BattingStyle.RIGHT_HAND, BowlingStyle.RIGHT_ARM_SPIN, jerseyNumber = 88),
                Player("aus_11", "Josh Hazlewood", PlayerRole.BOWLER, BattingStyle.LEFT_HAND, BowlingStyle.RIGHT_ARM_FAST, jerseyNumber = 38)
            )
        )

        // Initial Innings for India
        val initBattingStats = listOf(
            BattingStat(playerId = "ind_1", runs = 38, balls = 24, fours = 4, sixes = 2, isOut = false),
            BattingStat(playerId = "ind_2", runs = 29, balls = 18, fours = 3, sixes = 1, isOut = false)
        )
        val initBowlingStats = listOf(
            BowlingStat(playerId = "aus_9", legalBalls = 12, maidens = 0, runsConceded = 18, wickets = 0),
            BowlingStat(playerId = "aus_8", legalBalls = 12, maidens = 0, runsConceded = 24, wickets = 0),
            BowlingStat(playerId = "aus_10", legalBalls = 12, maidens = 0, runsConceded = 26, wickets = 0)
        )

        val deliveries = listOf(
            BallDelivery(overNumber = 5, ballNumberInOver = 1, bowlerId = "aus_10", strikerId = "ind_1", nonStrikerId = "ind_2", runsBat = 1),
            BallDelivery(overNumber = 5, ballNumberInOver = 2, bowlerId = "aus_10", strikerId = "ind_2", nonStrikerId = "ind_1", runsBat = 0),
            BallDelivery(overNumber = 5, ballNumberInOver = 3, bowlerId = "aus_10", strikerId = "ind_2", nonStrikerId = "ind_1", runsBat = 4),
            BallDelivery(overNumber = 5, ballNumberInOver = 4, bowlerId = "aus_10", strikerId = "ind_2", nonStrikerId = "ind_1", runsBat = 1),
            BallDelivery(overNumber = 5, ballNumberInOver = 5, bowlerId = "aus_10", strikerId = "ind_1", nonStrikerId = "ind_2", runsBat = 6),
            BallDelivery(overNumber = 5, ballNumberInOver = 6, bowlerId = "aus_10", strikerId = "ind_1", nonStrikerId = "ind_2", runsBat = 1)
        )

        val initialInnings = Innings(
            id = "inn_ind_1",
            battingTeamId = teamInd.id,
            bowlingTeamId = teamAus.id,
            totalRuns = 71,
            wickets = 0,
            legalBalls = 36, // 6.0 overs
            extras = ExtrasBreakdown(wides = 3, noBalls = 1),
            strikerId = "ind_1",
            nonStrikerId = "ind_2",
            currentBowlerId = "aus_10",
            battingStats = initBattingStats,
            bowlingStats = initBowlingStats,
            deliveries = deliveries
        )

        val liveMatch = CricketMatch(
            id = "match_icc_final",
            title = "ICC Men's T20 Championship Final",
            tournament = "ICC T20 World Cup 2026",
            venue = "Kensington Oval, Barbados",
            date = "Today • Night Game",
            teamA = teamInd,
            teamB = teamAus,
            oversLimit = 20,
            matchType = MatchType.T20,
            pitchCondition = "Hard Pitch (True Bounce, Fast Outfield)",
            tossWinnerId = teamInd.id,
            tossDecision = TossDecision.BAT,
            status = MatchStatus.LIVE,
            innings = listOf(initialInnings),
            currentInningsIndex = 0,
            resultText = "India opt to bat first"
        )

        // Upcoming Preset Match
        val teamMum = Team("team_mum", "Mumbai Titans", "MUM", 0xFF0D47A1)
        val teamChn = Team("team_chn", "Chennai Kings", "CHN", 0xFFF57F17)
        val upcomingMatch = CricketMatch(
            id = "match_ipl_derby",
            title = "Super Derby Clash",
            tournament = "Indian Premier League Derby",
            venue = "Wankhede Stadium, Mumbai",
            date = "Tomorrow • 19:30 IST",
            teamA = teamMum,
            teamB = teamChn,
            oversLimit = 20,
            matchType = MatchType.T20,
            pitchCondition = "Red Soil (Turn & Bounce)",
            status = MatchStatus.UPCOMING
        )

        // Completed T10 Match
        val teamLah = Team("team_lah", "Lahore Thunder", "LAH", 0xFF2E7D32)
        val teamPsh = Team("team_psh", "Peshawar Stars", "PSH", 0xFFD84315)
        val completedMatch = CricketMatch(
            id = "match_t10_blitz",
            title = "T10 Super Blitz Final",
            tournament = "Abu Dhabi T10 Blitz",
            venue = "Zayed Cricket Stadium",
            date = "Yesterday",
            teamA = teamLah,
            teamB = teamPsh,
            oversLimit = 10,
            matchType = MatchType.T10,
            status = MatchStatus.COMPLETED,
            resultText = "Lahore Thunder won by 18 runs"
        )

        return listOf(liveMatch, upcomingMatch, completedMatch)
    }
}
