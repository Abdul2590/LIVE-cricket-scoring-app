package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.CelebrationType
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun CelebrationOverlay(
    celebration: CelebrationType?,
    onDismiss: () -> Unit
) {
    if (celebration == null) return

    LaunchedEffect(celebration) {
        delay(if (celebration.isMajor) 2800L else 1800L)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        // Confetti particles
        ConfettiParticles(isMajor = celebration.isMajor)

        // Banner Card
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -60 }),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (celebration) {
                    CelebrationType.SIX -> Color(0xFFD84315)
                    CelebrationType.FOUR -> Color(0xFF2E7D32)
                    CelebrationType.FIFTY, CelebrationType.CENTURY -> Color(0xFFF57F17)
                    CelebrationType.WICKET -> Color(0xFFC2185B)
                    CelebrationType.MATCH_WON -> Color(0xFF1565C0)
                    else -> Color(0xFF37474F)
                },
                tonalElevation = 10.dp,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 380.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = celebration.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = celebration.subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continue Scoring", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfettiParticles(isMajor: Boolean) {
    val count = if (isMajor) 60 else 30
    val colors = listOf(
        Color(0xFFFFD700), Color(0xFFFF5252), Color(0xFF40C4FF),
        Color(0xFF69F0AE), Color(0xFFFF4081), Color(0xFFEEFF41)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = Random(42)
        val w = size.width
        val h = size.height
        for (i in 0 until count) {
            val cx = random.nextFloat() * w
            val cy = random.nextFloat() * h
            val radius = random.nextFloat() * 7f + 3f
            val color = colors[random.nextInt(colors.size)]
            drawCircle(
                color = color.copy(alpha = 0.85f),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}
