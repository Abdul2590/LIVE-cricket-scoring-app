package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AndroidStatusBar(
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val currentTime = remember {
        val sdf = SimpleDateFormat("h:mm", Locale.getDefault())
        sdf.format(Date())
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Current Time
        Text(
            text = currentTime,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )

        // Center: Camera Punch-hole Cutout
        Surface(
            shape = CircleShape,
            color = Color.Black,
            modifier = Modifier.size(11.dp)
        ) {}

        // Right: 5G & Battery Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "5G",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = contentColor
            )
            Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Icon(
                imageVector = Icons.Default.BatteryFull,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
