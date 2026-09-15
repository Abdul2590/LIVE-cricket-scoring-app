package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CricketRepository(private val context: Context) {

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

    // User Profile (Local Storage Profile)
    fun loadUserProfile(): UserProfile {
        val email = prefs.getString("user_email", "rehman.shaikh4@gmail.com") ?: "rehman.shaikh4@gmail.com"
        val rawName = prefs.getString("user_name", "Rehman Shaikh") ?: "Rehman Shaikh"
        val cleanName = rawName.replace("(Lead Scorer)", "").replace("Official Match Umpire", "").replace("Club Admin", "").trim().ifBlank { "Rehman Shaikh" }
        val role = prefs.getString("user_role", "Lead Scorer & Match Official") ?: "Lead Scorer & Match Official"
        val initials = cleanName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "RS" }

        return UserProfile(
            id = prefs.getString("user_id", "local_user_1") ?: "local_user_1",
            displayName = cleanName,
            role = role,
            email = email,
            photoInitials = initials,
            autoSaveEnabled = prefs.getBoolean("user_auto_save_enabled", true),
            lastAutoSaveTime = prefs.getString("user_last_auto_save", "Never") ?: "Never",
            totalSavedMatches = prefs.getInt("user_total_saved_matches", 0),
            localStorageLocation = "Internal App Storage",
            storageStatusDescription = prefs.getString("user_storage_status_desc", "Auto-save to local storage active") ?: "Auto-save to local storage active"
        )
    }

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString("user_id", profile.id)
            .putString("user_name", profile.displayName)
            .putString("user_role", profile.role)
            .putString("user_email", profile.email)
            .putString("user_initials", profile.photoInitials)
            .putBoolean("user_auto_save_enabled", profile.autoSaveEnabled)
            .putString("user_last_auto_save", profile.lastAutoSaveTime)
            .putInt("user_total_saved_matches", profile.totalSavedMatches)
            .putString("user_storage_status_desc", profile.storageStatusDescription)
            .apply()
    }

    // Auto-save full application state to Local Storage
    fun saveAllAppDataToLocalStorage(payloadJson: String): Boolean {
        return try {
            prefs.edit()
                .putString("local_storage_all_app_payload", payloadJson)
                .putLong("local_storage_last_save_timestamp", System.currentTimeMillis())
                .apply()

            // Also persist to a dedicated JSON file in context.filesDir
            val backupDir = File(context.filesDir, "criclive_data")
            if (!backupDir.exists()) backupDir.mkdirs()
            val backupFile = File(backupDir, "app_local_backup.json")
            backupFile.writeText(payloadJson)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadAllAppDataFromLocalStorage(): String? {
        val fromPrefs = prefs.getString("local_storage_all_app_payload", null)
        if (fromPrefs != null) return fromPrefs
        return try {
            val backupFile = File(File(context.filesDir, "criclive_data"), "app_local_backup.json")
            if (backupFile.exists()) backupFile.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    fun getLocalBackupFile(): File {
        val backupDir = File(context.filesDir, "criclive_data")
        if (!backupDir.exists()) backupDir.mkdirs()
        return File(backupDir, "app_local_backup.json")
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

    // Matches Persistence across App Restarts
    fun loadMatches(): List<CricketMatch> {
        val jsonStr = prefs.getString("saved_matches_v2", null)
        if (jsonStr.isNullOrBlank()) {
            val presets = getPresetMatches()
            saveMatches(presets)
            return presets
        }
        return try {
            val matches = parseMatchesJson(jsonStr)
            if (matches.isNotEmpty()) matches else {
                val presets = getPresetMatches()
                saveMatches(presets)
                presets
            }
        } catch (e: Exception) {
            val presets = getPresetMatches()
            saveMatches(presets)
            presets
        }
    }

    fun saveMatches(matches: List<CricketMatch>) {
        try {
            val jsonStr = serializeMatchesToJson(matches)
            prefs.edit().putString("saved_matches_v2", jsonStr).apply()
        } catch (_: Exception) {
        }
    }

    fun serializeMatchesToJson(matches: List<CricketMatch>): String {
        val rootArr = JSONArray()
        matches.forEach { m ->
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("title", m.title)
            mObj.put("tournament", m.tournament)
            mObj.put("venue", m.venue)
            mObj.put("date", m.date)
            mObj.put("oversLimit", m.oversLimit)
            mObj.put("matchType", m.matchType.name)
            mObj.put("pitchCondition", m.pitchCondition)
            mObj.put("tossWinnerId", m.tossWinnerId)
            mObj.put("tossDecision", m.tossDecision.name)
            mObj.put("status", m.status.name)
            mObj.put("currentInningsIndex", m.currentInningsIndex)
            mObj.put("resultText", m.resultText)

            // Team A
            mObj.put("teamA", serializeTeamToObj(m.teamA))
            // Team B
            mObj.put("teamB", serializeTeamToObj(m.teamB))

            // Innings
            val innArr = JSONArray()
            m.innings.forEach { inn ->
                val innObj = JSONObject()
                innObj.put("id", inn.id)
                innObj.put("battingTeamId", inn.battingTeamId)
                innObj.put("bowlingTeamId", inn.bowlingTeamId)
                innObj.put("totalRuns", inn.totalRuns)
                innObj.put("wickets", inn.wickets)
                innObj.put("legalBalls", inn.legalBalls)
                innObj.put("strikerId", inn.strikerId)
                innObj.put("nonStrikerId", inn.nonStrikerId)
                innObj.put("currentBowlerId", inn.currentBowlerId)
                innObj.put("lastOverBowlerId", inn.lastOverBowlerId)
                innObj.put("isWaitingForNewBowler", inn.isWaitingForNewBowler)
                innObj.put("isCompleted", inn.isCompleted)

                // Extras
                val extObj = JSONObject()
                extObj.put("wides", inn.extras.wides)
                extObj.put("noBalls", inn.extras.noBalls)
                extObj.put("byes", inn.extras.byes)
                extObj.put("legByes", inn.extras.legByes)
                extObj.put("penalty", inn.extras.penalty)
                innObj.put("extras", extObj)

                // Batting Stats
                val batArr = JSONArray()
                inn.battingStats.forEach { bs ->
                    val bsObj = JSONObject()
                    bsObj.put("playerId", bs.playerId)
                    bsObj.put("runs", bs.runs)
                    bsObj.put("balls", bs.balls)
                    bsObj.put("fours", bs.fours)
                    bsObj.put("sixes", bs.sixes)
                    bsObj.put("isOut", bs.isOut)
                    bs.dismissal?.let { d ->
                        val dObj = JSONObject()
                        dObj.put("type", d.type.name)
                        dObj.put("outPlayerId", d.outPlayerId)
                        d.bowlerId?.let { dObj.put("bowlerId", it) }
                        d.fielderName?.let { dObj.put("fielderName", it) }
                        bsObj.put("dismissal", dObj)
                    }
                    batArr.put(bsObj)
                }
                innObj.put("battingStats", batArr)

                // Bowling Stats
                val bowlArr = JSONArray()
                inn.bowlingStats.forEach { bws ->
                    val bwsObj = JSONObject()
                    bwsObj.put("playerId", bws.playerId)
                    bwsObj.put("legalBalls", bws.legalBalls)
                    bwsObj.put("maidens", bws.maidens)
                    bwsObj.put("runsConceded", bws.runsConceded)
                    bwsObj.put("wickets", bws.wickets)
                    bwsObj.put("wides", bws.wides)
                    bwsObj.put("noBalls", bws.noBalls)
                    bowlArr.put(bwsObj)
                }
                innObj.put("bowlingStats", bowlArr)

                // Deliveries
                val delArr = JSONArray()
                inn.deliveries.forEach { del ->
                    val dObj = JSONObject()
                    dObj.put("id", del.id)
                    dObj.put("overNumber", del.overNumber)
                    dObj.put("ballNumberInOver", del.ballNumberInOver)
                    dObj.put("bowlerId", del.bowlerId)
                    dObj.put("strikerId", del.strikerId)
                    dObj.put("nonStrikerId", del.nonStrikerId)
                    dObj.put("runsBat", del.runsBat)
                    dObj.put("extraType", del.extraType.name)
                    dObj.put("extraRuns", del.extraRuns)
                    dObj.put("isWicket", del.isWicket)
                    dObj.put("commentary", del.commentary)
                    dObj.put("isFreeHit", del.isFreeHit)
                    dObj.put("timestamp", del.timestamp)
                    del.dismissal?.let { dm ->
                        val dmObj = JSONObject()
                        dmObj.put("type", dm.type.name)
                        dmObj.put("outPlayerId", dm.outPlayerId)
                        dm.bowlerId?.let { dmObj.put("bowlerId", it) }
                        dm.fielderName?.let { dmObj.put("fielderName", it) }
                        dObj.put("dismissal", dmObj)
                    }
                    delArr.put(dObj)
                }
                innObj.put("deliveries", delArr)

                // Fall of wickets
                val fowArr = JSONArray()
                inn.fallOfWickets.forEach { f ->
                    val fObj = JSONObject()
                    fObj.put("wicketNumber", f.wicketNumber)
                    fObj.put("score", f.score)
                    fObj.put("overBall", f.overBall)
                    fObj.put("playerId", f.playerId)
                    fowArr.put(fObj)
                }
                innObj.put("fallOfWickets", fowArr)

                innArr.put(innObj)
            }
            mObj.put("innings", innArr)
            rootArr.put(mObj)
        }
        return rootArr.toString()
    }

    private fun serializeTeamToObj(team: Team): JSONObject {
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
        return tObj
    }

    fun parseMatchesJson(jsonStr: String): List<CricketMatch> {
        val rootArr = JSONArray(jsonStr)
        val matches = mutableListOf<CricketMatch>()
        for (i in 0 until rootArr.length()) {
            val mObj = rootArr.getJSONObject(i)
            val teamA = parseTeamFromObj(mObj.getJSONObject("teamA"))
            val teamB = parseTeamFromObj(mObj.getJSONObject("teamB"))

            val innList = mutableListOf<Innings>()
            val innArr = mObj.optJSONArray("innings")
            if (innArr != null) {
                for (k in 0 until innArr.length()) {
                    val innObj = innArr.getJSONObject(k)
                    val extObj = innObj.optJSONObject("extras")
                    val extras = if (extObj != null) {
                        ExtrasBreakdown(
                            wides = extObj.optInt("wides", 0),
                            noBalls = extObj.optInt("noBalls", 0),
                            byes = extObj.optInt("byes", 0),
                            legByes = extObj.optInt("legByes", 0),
                            penalty = extObj.optInt("penalty", 0)
                        )
                    } else ExtrasBreakdown()

                    val batList = mutableListOf<BattingStat>()
                    val batArr = innObj.optJSONArray("battingStats")
                    if (batArr != null) {
                        for (b in 0 until batArr.length()) {
                            val bsObj = batArr.getJSONObject(b)
                            val disObj = bsObj.optJSONObject("dismissal")
                            val dis = if (disObj != null) {
                                Dismissal(
                                    type = try { WicketType.valueOf(disObj.getString("type")) } catch (_: Exception) { WicketType.BOWLED },
                                    outPlayerId = disObj.getString("outPlayerId"),
                                    bowlerId = disObj.optString("bowlerId").takeIf { it.isNotBlank() },
                                    fielderName = disObj.optString("fielderName").takeIf { it.isNotBlank() }
                                )
                            } else null
                            batList.add(
                                BattingStat(
                                    playerId = bsObj.getString("playerId"),
                                    runs = bsObj.optInt("runs", 0),
                                    balls = bsObj.optInt("balls", 0),
                                    fours = bsObj.optInt("fours", 0),
                                    sixes = bsObj.optInt("sixes", 0),
                                    isOut = bsObj.optBoolean("isOut", false),
                                    dismissal = dis
                                )
                            )
                        }
                    }

                    val bowlList = mutableListOf<BowlingStat>()
                    val bowlArr = innObj.optJSONArray("bowlingStats")
                    if (bowlArr != null) {
                        for (bw in 0 until bowlArr.length()) {
                            val bwObj = bowlArr.getJSONObject(bw)
                            bowlList.add(
                                BowlingStat(
                                    playerId = bwObj.getString("playerId"),
                                    legalBalls = bwObj.optInt("legalBalls", 0),
                                    maidens = bwObj.optInt("maidens", 0),
                                    runsConceded = bwObj.optInt("runsConceded", 0),
                                    wickets = bwObj.optInt("wickets", 0),
                                    wides = bwObj.optInt("wides", 0),
                                    noBalls = bwObj.optInt("noBalls", 0)
                                )
                            )
                        }
                    }

                    val delList = mutableListOf<BallDelivery>()
                    val delArr = innObj.optJSONArray("deliveries")
                    if (delArr != null) {
                        for (d in 0 until delArr.length()) {
                            val dObj = delArr.getJSONObject(d)
                            val dmObj = dObj.optJSONObject("dismissal")
                            val dm = if (dmObj != null) {
                                Dismissal(
                                    type = try { WicketType.valueOf(dmObj.getString("type")) } catch (_: Exception) { WicketType.BOWLED },
                                    outPlayerId = dmObj.getString("outPlayerId"),
                                    bowlerId = dmObj.optString("bowlerId").takeIf { it.isNotBlank() },
                                    fielderName = dmObj.optString("fielderName").takeIf { it.isNotBlank() }
                                )
                            } else null
                            delList.add(
                                BallDelivery(
                                    id = dObj.optString("id", UUID.randomUUID().toString()),
                                    overNumber = dObj.optInt("overNumber", 0),
                                    ballNumberInOver = dObj.optInt("ballNumberInOver", 1),
                                    bowlerId = dObj.optString("bowlerId", ""),
                                    strikerId = dObj.optString("strikerId", ""),
                                    nonStrikerId = dObj.optString("nonStrikerId", ""),
                                    runsBat = dObj.optInt("runsBat", 0),
                                    extraType = try { ExtraType.valueOf(dObj.optString("extraType", ExtraType.NONE.name)) } catch (_: Exception) { ExtraType.NONE },
                                    extraRuns = dObj.optInt("extraRuns", 0),
                                    isWicket = dObj.optBoolean("isWicket", false),
                                    dismissal = dm,
                                    commentary = dObj.optString("commentary", ""),
                                    isFreeHit = dObj.optBoolean("isFreeHit", false),
                                    timestamp = dObj.optLong("timestamp", System.currentTimeMillis())
                                )
                            )
                        }
                    }

                    val fowList = mutableListOf<FallOfWicket>()
                    val fowArr = innObj.optJSONArray("fallOfWickets")
                    if (fowArr != null) {
                        for (f in 0 until fowArr.length()) {
                            val fObj = fowArr.getJSONObject(f)
                            fowList.add(
                                FallOfWicket(
                                    wicketNumber = fObj.getInt("wicketNumber"),
                                    score = fObj.getInt("score"),
                                    overBall = fObj.getString("overBall"),
                                    playerId = fObj.getString("playerId")
                                )
                            )
                        }
                    }

                    innList.add(
                        Innings(
                            id = innObj.optString("id", UUID.randomUUID().toString()),
                            battingTeamId = innObj.optString("battingTeamId", teamA.id),
                            bowlingTeamId = innObj.optString("bowlingTeamId", teamB.id),
                            totalRuns = innObj.optInt("totalRuns", 0),
                            wickets = innObj.optInt("wickets", 0),
                            legalBalls = innObj.optInt("legalBalls", 0),
                            extras = extras,
                            strikerId = innObj.optString("strikerId", ""),
                            nonStrikerId = innObj.optString("nonStrikerId", ""),
                            currentBowlerId = innObj.optString("currentBowlerId", ""),
                            lastOverBowlerId = innObj.optString("lastOverBowlerId", ""),
                            isWaitingForNewBowler = innObj.optBoolean("isWaitingForNewBowler", false),
                            battingStats = batList,
                            bowlingStats = bowlList,
                            deliveries = delList,
                            fallOfWickets = fowList,
                            isCompleted = innObj.optBoolean("isCompleted", false)
                        )
                    )
                }
            }

            matches.add(
                CricketMatch(
                    id = mObj.getString("id"),
                    title = mObj.getString("title"),
                    tournament = mObj.optString("tournament", "Championship"),
                    venue = mObj.optString("venue", "Stadium"),
                    date = mObj.optString("date", "Today"),
                    teamA = teamA,
                    teamB = teamB,
                    oversLimit = mObj.optInt("oversLimit", 20),
                    matchType = try { MatchType.valueOf(mObj.optString("matchType", MatchType.T20.name)) } catch (_: Exception) { MatchType.T20 },
                    pitchCondition = mObj.optString("pitchCondition", "Dry Pitch"),
                    tossWinnerId = mObj.optString("tossWinnerId", ""),
                    tossDecision = try { TossDecision.valueOf(mObj.optString("tossDecision", TossDecision.BAT.name)) } catch (_: Exception) { TossDecision.BAT },
                    status = try { MatchStatus.valueOf(mObj.optString("status", MatchStatus.LIVE.name)) } catch (_: Exception) { MatchStatus.LIVE },
                    innings = innList,
                    currentInningsIndex = mObj.optInt("currentInningsIndex", 0),
                    resultText = mObj.optString("resultText", "")
                )
            )
        }
        return matches
    }

    private fun parsePlayerRole(rawRole: String?, rawName: String = "", isWkFlag: Boolean = false, bowlingStyle: BowlingStyle = BowlingStyle.NONE, squadIndex: Int = 0): PlayerRole {
        if (isWkFlag) return PlayerRole.WICKET_KEEPER
        val nameLower = rawName.lowercase()
        if (nameLower.contains("(wk)") || nameLower.contains("[wk]") || nameLower.contains("wicketkeeper") || nameLower.contains("wicket-keeper") || nameLower.contains("wicket keeper") || nameLower.contains("(keeper)")) {
            return PlayerRole.WICKET_KEEPER
        }
        if (nameLower.contains("(bowler)") || nameLower.contains("(bowl)") || nameLower.contains("[bowler]") || nameLower.contains("(bowl)")) {
            return PlayerRole.BOWLER
        }
        if (nameLower.contains("(all-rounder)") || nameLower.contains("(allrounder)") || nameLower.contains("(ar)") || nameLower.contains("(all rounder)")) {
            return PlayerRole.ALL_ROUNDER
        }
        if (nameLower.contains("(batsman)") || nameLower.contains("(bat)")) {
            return PlayerRole.BATSMAN
        }

        val str = (rawRole ?: "").trim().lowercase()
        if (str.isNotBlank()) {
            when {
                str.contains("wicket") || str.contains("wk") || str == "keeper" || str == "wkeeper" -> return PlayerRole.WICKET_KEEPER
                str.contains("all_rounder") || str.contains("all-rounder") || str.contains("allrounder") || str.contains("all rounder") || str == "ar" -> return PlayerRole.ALL_ROUNDER
                str.contains("bowler") || str.contains("bowl") || str.contains("paceman") || str.contains("spinner") || str.contains("fast") || str.contains("seamer") -> return PlayerRole.BOWLER
                str.contains("batsman") || str.contains("batter") || str.contains("bat") || str.contains("opening") || str.contains("middle order") -> return PlayerRole.BATSMAN
            }
        }

        if (bowlingStyle != BowlingStyle.NONE) {
            return PlayerRole.BOWLER
        }

        // Default balance for raw string lists (0-3 batsman, 4-6 all-rounder, 7+ bowler)
        if (rawRole.isNullOrBlank()) {
            return when {
                squadIndex >= 6 -> PlayerRole.BOWLER
                squadIndex in 4..5 -> PlayerRole.ALL_ROUNDER
                else -> PlayerRole.BATSMAN
            }
        }

        return PlayerRole.BATSMAN
    }

    private fun parseBattingStyle(rawStyle: String?): BattingStyle {
        val str = (rawStyle ?: "").trim().lowercase()
        return if (str.contains("left") || str == "lhb" || str == "l") BattingStyle.LEFT_HAND else BattingStyle.RIGHT_HAND
    }

    private fun parseBowlingStyle(rawStyle: String?, role: PlayerRole = PlayerRole.BATSMAN): BowlingStyle {
        val str = (rawStyle ?: "").trim().lowercase()
        return when {
            str.contains("left") && (str.contains("fast") || str.contains("pace") || str.contains("medium")) -> BowlingStyle.LEFT_ARM_FAST
            str.contains("left") && (str.contains("spin") || str.contains("orthodox") || str.contains("chinaman")) -> BowlingStyle.LEFT_ARM_SPIN
            str.contains("right") && (str.contains("fast") || str.contains("pace") || str.contains("express")) -> BowlingStyle.RIGHT_ARM_FAST
            str.contains("right") && (str.contains("medium") || str.contains("seam")) -> BowlingStyle.RIGHT_ARM_MEDIUM
            str.contains("right") && (str.contains("spin") || str.contains("off") || str.contains("leg")) -> BowlingStyle.RIGHT_ARM_SPIN
            str.contains("fast") || str.contains("pace") -> BowlingStyle.RIGHT_ARM_FAST
            str.contains("medium") -> BowlingStyle.RIGHT_ARM_MEDIUM
            str.contains("spin") -> BowlingStyle.RIGHT_ARM_SPIN
            role == PlayerRole.BOWLER -> BowlingStyle.RIGHT_ARM_FAST
            role == PlayerRole.ALL_ROUNDER -> BowlingStyle.RIGHT_ARM_MEDIUM
            else -> BowlingStyle.NONE
        }
    }

    private fun parseTeamFromObj(tObj: JSONObject): Team {
        val pArr = tObj.optJSONArray("players")
        val players = mutableListOf<Player>()
        if (pArr != null) {
            for (j in 0 until pArr.length()) {
                val pItem = pArr.get(j)
                if (pItem is JSONObject) {
                    val rawRole = pItem.optString("role", "").ifBlank { pItem.optString("player_role", "").ifBlank { pItem.optString("type", "") } }
                    val rawName = pItem.optString("name", "").ifBlank { pItem.optString("playerName", "Player ${j + 1}") }
                    val rawBat = pItem.optString("battingStyle", "").ifBlank { pItem.optString("batting", "") }
                    val rawBowl = pItem.optString("bowlingStyle", "").ifBlank { pItem.optString("bowling", "") }
                    val isCaptain = pItem.optBoolean("isCaptain", false) || pItem.optBoolean("captain", false) || rawName.lowercase().contains("(c)")
                    val isWk = pItem.optBoolean("isWicketKeeper", false) || pItem.optBoolean("isWk", false) || pItem.optBoolean("wicketkeeper", false) || rawName.lowercase().contains("(wk)")

                    val bStyle = parseBattingStyle(rawBat)
                    val bwStyle = parseBowlingStyle(rawBowl)
                    val role = parsePlayerRole(rawRole, rawName, isWk, bwStyle, j)

                    players.add(
                        Player(
                            id = pItem.optString("id", UUID.randomUUID().toString()),
                            name = rawName,
                            role = role,
                            battingStyle = bStyle,
                            bowlingStyle = if (role == PlayerRole.BATSMAN && bwStyle == BowlingStyle.NONE) BowlingStyle.NONE else if (bwStyle == BowlingStyle.NONE && (role == PlayerRole.BOWLER || role == PlayerRole.ALL_ROUNDER)) parseBowlingStyle("", role) else bwStyle,
                            isCaptain = isCaptain,
                            isWicketKeeper = isWk || role == PlayerRole.WICKET_KEEPER,
                            jerseyNumber = pItem.optInt("jerseyNumber", pItem.optInt("jersey_number", j + 1))
                        )
                    )
                }
            }
        }
        return Team(
            id = tObj.optString("id", UUID.randomUUID().toString()),
            name = tObj.optString("name", "Team"),
            shortCode = tObj.optString("shortCode", "TEA"),
            colorHex = tObj.optLong("colorHex", 0xFF1B5E20),
            players = players,
            isCustom = tObj.optBoolean("isCustom", false)
        )
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
                        val rawRole = pItem.optString("role", "").ifBlank { pItem.optString("player_role", "").ifBlank { pItem.optString("type", "") } }
                        val rawName = pItem.optString("name", "").ifBlank { pItem.optString("playerName", "Player ${j + 1}") }
                        val rawBat = pItem.optString("battingStyle", "").ifBlank { pItem.optString("batting", "") }
                        val rawBowl = pItem.optString("bowlingStyle", "").ifBlank { pItem.optString("bowling", "") }
                        val isCaptain = pItem.optBoolean("isCaptain", false) || pItem.optBoolean("captain", false) || rawName.lowercase().contains("(c)")
                        val isWk = pItem.optBoolean("isWicketKeeper", false) || pItem.optBoolean("isWk", false) || pItem.optBoolean("wicketkeeper", false) || rawName.lowercase().contains("(wk)")

                        val bStyle = parseBattingStyle(rawBat)
                        val bwStyle = parseBowlingStyle(rawBowl)
                        val role = parsePlayerRole(rawRole, rawName, isWk, bwStyle, j)

                        players.add(
                            Player(
                                id = pItem.optString("id", UUID.randomUUID().toString()),
                                name = rawName,
                                role = role,
                                battingStyle = bStyle,
                                bowlingStyle = if (role == PlayerRole.BATSMAN && bwStyle == BowlingStyle.NONE) BowlingStyle.NONE else if (bwStyle == BowlingStyle.NONE && (role == PlayerRole.BOWLER || role == PlayerRole.ALL_ROUNDER)) parseBowlingStyle("", role) else bwStyle,
                                isCaptain = isCaptain,
                                isWicketKeeper = isWk || role == PlayerRole.WICKET_KEEPER,
                                jerseyNumber = pItem.optInt("jerseyNumber", pItem.optInt("jersey_number", j + 1))
                            )
                        )
                    } else if (pItem is String && pItem.isNotBlank()) {
                        val nameStr = pItem.trim()
                        val isCaptain = nameStr.lowercase().contains("(c)")
                        val isWk = nameStr.lowercase().contains("(wk)")
                        val role = parsePlayerRole(null, nameStr, isWk, BowlingStyle.NONE, j)
                        val bwStyle = if (role == PlayerRole.BOWLER) BowlingStyle.RIGHT_ARM_FAST else if (role == PlayerRole.ALL_ROUNDER) BowlingStyle.RIGHT_ARM_MEDIUM else BowlingStyle.NONE
                        players.add(
                            Player(
                                id = UUID.randomUUID().toString(),
                                name = nameStr,
                                role = role,
                                battingStyle = BattingStyle.RIGHT_HAND,
                                bowlingStyle = bwStyle,
                                isCaptain = isCaptain,
                                isWicketKeeper = isWk || role == PlayerRole.WICKET_KEEPER,
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
