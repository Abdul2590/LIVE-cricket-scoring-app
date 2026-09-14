package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Team

@Composable
fun ExportTeamsDialog(
    teams: List<Team>,
    jsonString: String,
    onDismiss: () -> Unit,
    onCopied: () -> Unit
) {
    val context = LocalContext.current
    val totalPlayers = teams.sumOf { it.players.size }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Teams Data", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Export all ${teams.size} custom team(s) and $totalPlayers players as standard JSON for backup, sharing, or importing onto other devices.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick stats card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${teams.size}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Teams", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider(modifier = Modifier.height(28.dp).width(1.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalPlayers", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                            Text("Total Players", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider(modifier = Modifier.height(28.dp).width(1.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${jsonString.toByteArray().size / 1024 + 1} KB", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("File Size", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Text("JSON Preview:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                // JSON Monospace box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = jsonString,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy to clipboard
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Teams Data JSON", jsonString)
                            clipboard.setPrimaryClip(clip)
                            onCopied()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy JSON", fontSize = 12.sp)
                    }

                    // Share Intent
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "CricLive Custom Teams Export")
                                putExtra(Intent.EXTRA_TEXT, jsonString)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share / Save Teams JSON"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share File", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ImportTeamsDialog(
    onDismiss: () -> Unit,
    onImport: (json: String, replaceExisting: Boolean) -> Unit
) {
    val context = LocalContext.current
    var jsonInput by remember { mutableStateOf("") }
    var replaceExisting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var detectedCount by remember { mutableStateOf(0) }

    val sampleTemplate = """[
  {
    "name": "Royal Lions",
    "shortCode": "RLN",
    "colorHex": 4279979313,
    "players": [
      {
        "name": "Aman Verma",
        "role": "BATSMAN",
        "battingStyle": "RIGHT_HAND",
        "bowlingStyle": "NONE",
        "isCaptain": true,
        "jerseyNumber": 18
      },
      {
        "name": "Karan Patel (wk)",
        "role": "WICKET_KEEPER",
        "isWicketKeeper": true,
        "jerseyNumber": 7
      },
      {
        "name": "Samir Khan",
        "role": "BOWLER",
        "bowlingStyle": "RIGHT_ARM_FAST",
        "jerseyNumber": 99
      }
    ]
  }
]"""

    // Auto validate JSON on change
    LaunchedEffect(jsonInput) {
        if (jsonInput.isBlank()) {
            validationError = null
            detectedCount = 0
        } else {
            try {
                val trimmed = jsonInput.trim()
                val count = if (trimmed.startsWith("{")) {
                    val root = org.json.JSONObject(trimmed)
                    val arr = root.optJSONArray("teams") ?: root.optJSONArray("custom_teams") ?: root.optJSONArray("data")
                    arr?.length() ?: 0
                } else if (trimmed.startsWith("[")) {
                    val arr = org.json.JSONArray(trimmed)
                    arr.length()
                } else {
                    0
                }
                if (count == 0) {
                    validationError = "JSON must be an array of teams [ ... ] or { \"teams\": [ ... ] }"
                    detectedCount = 0
                } else {
                    validationError = null
                    detectedCount = count
                }
            } catch (e: Exception) {
                validationError = "Invalid JSON syntax: ${e.localizedMessage?.take(45)}"
                detectedCount = 0
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Teams Data", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Paste JSON data below to import custom teams, player rosters, and jersey numbers.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Paste from clipboard
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    jsonInput = text
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Clipboard", fontSize = 11.sp)
                    }

                    // Load sample template
                    OutlinedButton(
                        onClick = { jsonInput = sampleTemplate },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Sample", fontSize = 11.sp)
                    }
                }

                // JSON Text Field
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Teams JSON Data") },
                    placeholder = { Text("Paste JSON here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )

                // Validation Status
                if (validationError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(validationError ?: "", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                } else if (detectedCount > 0) {
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ready to import $detectedCount team(s) with squads!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }

                // Merge vs Replace mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Replace Existing Teams", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            if (replaceExisting) "Replaces your current custom teams" else "Merges new teams without deleting existing ones",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = replaceExisting,
                        onCheckedChange = { replaceExisting = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jsonInput.isNotBlank() && validationError == null && detectedCount > 0) {
                        onImport(jsonInput.trim(), replaceExisting)
                    }
                },
                enabled = jsonInput.isNotBlank() && validationError == null && detectedCount > 0
            ) {
                Text("Import Teams ($detectedCount)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmButtonText: String = "Delete",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(message, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
