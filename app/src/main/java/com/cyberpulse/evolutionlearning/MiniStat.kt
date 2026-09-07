package com.cyberpulse.evolutionlearning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

@Composable
internal fun MiniStat(text: String, accent: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
