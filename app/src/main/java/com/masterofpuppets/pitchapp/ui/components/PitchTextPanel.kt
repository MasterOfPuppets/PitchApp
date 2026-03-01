package com.masterofpuppets.pitchapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PitchTextPanel(
    noteName: String,
    octave: Int,
    frequency: Double,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.height(110.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = noteName,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor
                )
                Text(
                    text = if (noteName != "--") octave.toString() else "0",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = if (noteName != "--") textColor else Color.Transparent,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
            }
        }
        val freqText = if (frequency > 0.0) {
            String.format("%.1f Hz", frequency)
        } else {
            "-- Hz"
        }
        Box(
            modifier = Modifier.height(40.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = freqText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = textColor.copy(alpha = 0.7f), // Slightly dimmed for visual hierarchy
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}