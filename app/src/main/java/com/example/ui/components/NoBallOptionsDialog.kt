package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExtraType

@Composable
fun NoBallOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (runsBat: Int, extraRuns: Int, commentary: String) -> Unit
) {
    var mode by remember { mutableStateOf(0) } // 0 = Off Bat, 1 = Byes / Leg Byes
    var runsBat by remember { mutableStateOf(0) }
    var byeType by remember { mutableStateOf("BYE") } // "BYE" or "LEG_BYE"
    var byeRuns by remember { mutableStateOf(1) }
    var customComment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFD84315),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("NB", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("No Ball Delivery", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "FREE HIT NEXT DELIVERY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD84315)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Bowler overstepped or bowled an illegal delivery (1 penalty run awarded). Select any additional runs scored on this ball:",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mode Tabs: Off Bat vs Byes
                TabRow(selectedTabIndex = mode) {
                    Tab(
                        selected = mode == 0,
                        onClick = { mode = 0 },
                        text = { Text("Runs Off Bat 🏏", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                    )
                    Tab(
                        selected = mode == 1,
                        onClick = { mode = 1 },
                        text = { Text("Byes / Leg Byes 🏃", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                    )
                }

                if (mode == 0) {
                    // Bat runs
                    Text("Runs Scored By Batsman:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                    val batRunOptions = listOf(
                        Triple(0, "NB Only (+1)", "1 run (penalty only)"),
                        Triple(1, "NB + 1 Single (+2)", "2 runs (strike rotates)"),
                        Triple(2, "NB + 2 Double (+3)", "3 runs total"),
                        Triple(3, "NB + 3 Three (+4)", "4 runs (strike rotates)"),
                        Triple(4, "NB + 4 FOUR! (+5)", "5 runs (boundary scored!)"),
                        Triple(6, "NB + 6 SIX! (+7)", "7 runs (maximum scored!)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        batRunOptions.forEach { (runs, label, sub) ->
                            val isSelected = runsBat == runs
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isSelected && runs == 6 -> Color(0xFFE65100).copy(alpha = 0.2f)
                                    isSelected && runs == 4 -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    if (runs == 6) Color(0xFFE65100) else if (runs == 4) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                ) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { runsBat = runs }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { runsBat = runs }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Byes / Leg byes on No Ball
                    Text("Type of Extras on No Ball:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = byeType == "BYE",
                            onClick = { byeType = "BYE" },
                            label = { Text("Byes on NB") }
                        )
                        FilterChip(
                            selected = byeType == "LEG_BYE",
                            onClick = { byeType = "LEG_BYE" },
                            label = { Text("Leg Byes on NB") }
                        )
                    }

                    Text("Runs Run (1 NB penalty + Byes):", fontSize = 12.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4).forEach { r ->
                            FilterChip(
                                selected = byeRuns == r,
                                onClick = { byeRuns = r },
                                label = { Text("$r ${if (r == 1) "run" else "runs"} (+${r + 1} tot)") }
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Summary: 1 No Ball penalty + $byeRuns $byeType = ${byeRuns + 1} runs total. Bowler charged 1 run.",
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Summary Badge
                val totalRunsCalculated = if (mode == 0) (runsBat + 1) else (byeRuns + 1)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFD84315).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL RUNS ON DELIVERY:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD84315))
                        Text("+$totalRunsCalculated Runs", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFFD84315))
                    }
                }

                OutlinedTextField(
                    value = customComment,
                    onValueChange = { customComment = it },
                    label = { Text("Commentary note (Optional)") },
                    placeholder = { Text("e.g. Front foot overstep, batsman pulled to deep midwicket") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mode == 0) {
                        onConfirm(runsBat, 1, customComment.trim())
                    } else {
                        // 1 NB penalty + byeRuns
                        val extraRunsTotal = 1 + byeRuns
                        val comment = if (customComment.isNotBlank()) {
                            customComment.trim()
                        } else {
                            "No ball! 1 penalty + $byeRuns $byeType runs taken."
                        }
                        onConfirm(0, extraRunsTotal, comment)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315))
            ) {
                Text("Confirm No Ball")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
