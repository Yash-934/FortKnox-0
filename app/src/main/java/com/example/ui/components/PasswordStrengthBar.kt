package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.PasswordGenerator
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberLaserRed
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberSurfaceHigh
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextSecondary

@Composable
fun PasswordStrengthBar(
    analysis: PasswordGenerator.StrengthAnalysis,
    modifier: Modifier = Modifier
) {
    val barColor by animateColorAsState(
        targetValue = when (analysis.rating) {
            PasswordGenerator.StrengthRating.VERY_WEAK -> CyberLaserRed
            PasswordGenerator.StrengthRating.WEAK -> Color(0xFFF97316)
            PasswordGenerator.StrengthRating.FAIR -> CyberGold
            PasswordGenerator.StrengthRating.STRONG -> CyberEmerald
            PasswordGenerator.StrengthRating.MILITARY_GRADE -> CyberCyan
        },
        animationSpec = tween(300),
        label = "strengthColor"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = analysis.scorePercent,
        animationSpec = tween(400),
        label = "strengthProgress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = analysis.rating.label,
                    color = barColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${analysis.entropyBits} bits • Crack: ${analysis.crackTimeEstimate}",
                color = CyberTextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5-segment neon gauge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val filledSegments = (animatedProgress * 5).toInt()
            for (i in 1..5) {
                val isFilled = i <= filledSegments
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(androidx.compose.foundation.shape.CutCornerShape(3.dp))
                        .background(if (isFilled) barColor else CyberSurfaceHigh)
                )
            }
        }

        if (analysis.warnings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⚠️ " + analysis.warnings.first(),
                color = CyberTextMuted,
                fontSize = 10.sp
            )
        }
    }
}
