package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.model.CricketMatch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScorecardPdfGenerator {

    fun generateScorecardPdf(context: Context, match: CricketMatch): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait: 595x842 pt
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Header Background Banner
        paint.color = Color.rgb(13, 92, 58) // Deep stadium green
        canvas.drawRect(0f, 0f, 595f, 65f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CRICLIVE • OFFICIAL MATCH SCORECARD", 30f, 32f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("${match.tournament} • ${match.venue} • ${match.oversLimit} Overs Format", 30f, 50f, paint)

        var y = 85f

        // Match Info Card
        paint.color = Color.rgb(245, 247, 248)
        canvas.drawRoundRect(25f, y, 570f, y + 42f, 8f, 8f, paint)

        paint.color = Color.rgb(25, 30, 36)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${match.teamA.name} vs ${match.teamB.name}", 35f, y + 20f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(80, 90, 100)
        val tossTeam = if (match.tossWinnerId == match.teamA.id) match.teamA.name else match.teamB.name
        val tossInfo = if (match.tossWinnerId.isNotBlank()) "Toss: $tossTeam elected to ${match.tossDecision.displayName.lowercase()}" else "Toss pending"
        val statusInfo = if (match.resultText.isNotBlank()) match.resultText else "Status: ${match.status.displayName}"
        canvas.drawText("$tossInfo | $statusInfo", 35f, y + 34f, paint)

        y += 55f

        val playersMap = match.allPlayersMap

        // Draw each Innings
        match.innings.forEachIndexed { index, innings ->
            if (y > 720f) return@forEachIndexed

            val battingTeam = if (innings.battingTeamId == match.teamA.id) match.teamA else match.teamB
            val bowlingTeam = if (innings.bowlingTeamId == match.teamA.id) match.teamA else match.teamB

            // Innings Header Banner
            paint.color = Color.rgb(230, 238, 233)
            canvas.drawRoundRect(25f, y, 570f, y + 22f, 4f, 4f, paint)

            paint.color = Color.rgb(13, 92, 58)
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val rrStr = String.format(Locale.US, "%.2f", innings.runRate)
            canvas.drawText(
                "INNINGS ${index + 1}: ${battingTeam.name}  ${innings.totalRuns}/${innings.wickets} (${innings.oversString} Ov, RR: $rrStr)",
                35f, y + 15f, paint
            )
            y += 28f

            // Batting Table Header
            paint.color = Color.rgb(40, 50, 60)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BATTER", 35f, y, paint)
            canvas.drawText("DISMISSAL", 165f, y, paint)
            canvas.drawText("R", 390f, y, paint)
            canvas.drawText("B", 425f, y, paint)
            canvas.drawText("4s", 460f, y, paint)
            canvas.drawText("6s", 495f, y, paint)
            canvas.drawText("SR", 530f, y, paint)

            paint.color = Color.rgb(200, 210, 220)
            paint.strokeWidth = 1f
            canvas.drawLine(25f, y + 4f, 570f, y + 4f, paint)
            y += 14f

            // Batting Rows
            innings.battingStats.forEach { stat ->
                if (y > 760f) return@forEach
                val player = playersMap[stat.playerId]
                val name = player?.let {
                    var n = it.name
                    if (it.isCaptain) n += " (c)"
                    if (it.isWicketKeeper) n += " (wk)"
                    n
                } ?: "Batsman"

                val dismissalText = if (stat.isOut) {
                    stat.dismissal?.description(playersMap) ?: "out"
                } else if (stat.playerId == innings.strikerId || stat.playerId == innings.nonStrikerId) {
                    "not out *"
                } else {
                    "not out"
                }

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 9f
                paint.color = Color.rgb(20, 25, 30)
                canvas.drawText(name.take(22), 35f, y, paint)

                paint.color = Color.rgb(90, 100, 110)
                canvas.drawText(dismissalText.take(38), 165f, y, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.rgb(10, 15, 20)
                canvas.drawText("${stat.runs}", 390f, y, paint)

                paint.typeface = Typeface.DEFAULT
                paint.color = Color.rgb(60, 70, 80)
                canvas.drawText("${stat.balls}", 425f, y, paint)
                canvas.drawText("${stat.fours}", 460f, y, paint)
                canvas.drawText("${stat.sixes}", 495f, y, paint)
                val srStr = String.format(Locale.US, "%.1f", stat.strikeRate)
                canvas.drawText(srStr, 530f, y, paint)

                y += 13f
            }

            // Extras & Total
            paint.color = Color.rgb(230, 235, 240)
            canvas.drawLine(25f, y, 570f, y, paint)
            y += 12f

            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(80, 90, 100)
            val ext = innings.extras
            canvas.drawText(
                "Extras: ${ext.total} (wd ${ext.wides}, nb ${ext.noBalls}, b ${ext.byes}, lb ${ext.legByes})",
                35f, y, paint
            )
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.rgb(10, 15, 20)
            canvas.drawText("TOTAL: ${innings.totalRuns}/${innings.wickets} (${innings.oversString} Ov)", 390f, y, paint)
            y += 18f

            // Fall of Wickets
            if (innings.fallOfWickets.isNotEmpty()) {
                paint.textSize = 8.5f
                paint.typeface = Typeface.DEFAULT
                paint.color = Color.rgb(100, 110, 120)
                val fowText = innings.fallOfWickets.joinToString(", ") { fow ->
                    val pName = playersMap[fow.playerId]?.name ?: "Wkt"
                    "${fow.score}-${fow.wicketNumber} ($pName, ${fow.overBall} ov)"
                }
                canvas.drawText("Fall of Wickets: ${fowText.take(110)}", 35f, y, paint)
                y += 16f
            }

            // Bowling Table Header
            paint.color = Color.rgb(40, 50, 60)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BOWLER (${bowlingTeam.name})", 35f, y, paint)
            canvas.drawText("O", 350f, y, paint)
            canvas.drawText("M", 390f, y, paint)
            canvas.drawText("R", 430f, y, paint)
            canvas.drawText("W", 470f, y, paint)
            canvas.drawText("ECON", 520f, y, paint)

            paint.color = Color.rgb(200, 210, 220)
            canvas.drawLine(25f, y + 3f, 570f, y + 3f, paint)
            y += 13f

            innings.bowlingStats.forEach { bStat ->
                if (y > 780f) return@forEach
                val bPlayer = playersMap[bStat.playerId]
                val bName = bPlayer?.name ?: "Bowler"

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 9f
                paint.color = Color.rgb(20, 25, 30)
                canvas.drawText(bName.take(25), 35f, y, paint)
                canvas.drawText(bStat.oversString, 350f, y, paint)
                canvas.drawText("${bStat.maidens}", 390f, y, paint)
                canvas.drawText("${bStat.runsConceded}", 430f, y, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${bStat.wickets}", 470f, y, paint)

                paint.typeface = Typeface.DEFAULT
                val econStr = String.format(Locale.US, "%.2f", bStat.economy)
                canvas.drawText(econStr, 520f, y, paint)

                y += 12f
            }

            y += 12f
        }

        // Footer
        paint.color = Color.rgb(180, 190, 200)
        canvas.drawLine(25f, 810f, 570f, 810f, paint)
        paint.textSize = 8f
        paint.color = Color.rgb(120, 130, 140)
        val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated by CricLive Offline Engine • $dateStr", 35f, 825f, paint)
        canvas.drawText("Official Dossier • Page 1 of 1", 450f, 825f, paint)

        document.finishPage(page)

        return try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
            if (!docsDir.exists()) docsDir.mkdirs()
            val fileName = "Scorecard_${match.teamA.shortCode}_vs_${match.teamB.shortCode}_${System.currentTimeMillis()}.pdf"
            val file = File(docsDir, fileName)
            val out = FileOutputStream(file)
            document.writeTo(out)
            out.flush()
            out.close()
            document.close()
            file
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    fun openOrSharePdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cricket Match Scorecard - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Scorecard PDF"))
        } catch (e: Exception) {
            // fallback
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
            } catch (_: Exception) {}
        }
    }
}
