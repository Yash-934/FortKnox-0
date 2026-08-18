package com.example.ui.components
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberPink
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceHigh
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary


@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    glowColor: Color = CyberCyan,
    borderColor: Color = CyberBorder,
    backgroundColor: Color = CyberSurface.copy(alpha = 0.85f),
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    val cutShape = CutCornerShape(
        topStart = 0.dp,
        topEnd = cornerRadius,
        bottomEnd = 0.dp,
        bottomStart = cornerRadius
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(glowColor.copy(alpha = 0.6f), borderColor, borderColor, glowColor.copy(alpha = 0.3f))
    )

    Box(
        modifier = modifier
            .clip(cutShape)
            .background(backgroundColor)
            .border(1.dp, borderBrush, cutShape)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = CyberCyan,
    textColor: Color = CyberBackground,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fontSize: TextUnit = 13.sp,
    cutCorner: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
) {
    val cutShape = CutCornerShape(
        topStart = cutCorner,
        topEnd = 0.dp,
        bottomEnd = cutCorner,
        bottomStart = 0.dp
    )
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.clip(cutShape),
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor,
            disabledContainerColor = CyberSurfaceHigh,
            disabledContentColor = CyberTextMuted
        ),
        shape = cutShape
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CyberOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = CyberCyan,
    enabled: Boolean = true,
    fontSize: TextUnit = 13.sp,
    cutCorner: Dp = 10.dp
) {
    val cutShape = CutCornerShape(
        topStart = cutCorner,
        topEnd = 0.dp,
        bottomEnd = cutCorner,
        bottomStart = 0.dp
    )
    Box(
        modifier = modifier
            .clip(cutShape)
            .background(CyberSurfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, if (enabled) color.copy(alpha = 0.8f) else CyberBorder, cutShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = if (enabled) color else CyberTextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text.uppercase(),
                color = if (enabled) color else CyberTextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CyberBadge(
    text: String,
    color: Color = CyberCyan,
    modifier: Modifier = Modifier
) {
    val cutShape = CutCornerShape(
        topStart = 0.dp,
        topEnd = 6.dp,
        bottomEnd = 0.dp,
        bottomStart = 6.dp
    )
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = cutShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun CopyIconButton(
    textToCopy: String,
    onCopy: () -> Unit,
    isCopied: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cutShape = CutCornerShape(
        topStart = 8.dp,
        topEnd = 0.dp,
        bottomEnd = 8.dp,
        bottomStart = 0.dp
    )
    IconButton(
        onClick = onCopy,
        modifier = modifier
            .size(36.dp)
            .clip(cutShape)
            .background(if (isCopied) CyberEmerald.copy(alpha = 0.2f) else CyberSurfaceVariant)
            .border(1.dp, if (isCopied) CyberEmerald else CyberBorder, cutShape)
    ) {
        Icon(
            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = "Copy",
            tint = if (isCopied) CyberEmerald else CyberCyan,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun CyberGridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val crossColor = com.example.ui.theme.CyberCyanMuted.copy(alpha = 0.25f)
        val gridSize = 40.dp.toPx()
        val crossSize = 2.dp.toPx()
        
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                // Draw small crosses instead of full lines
                drawLine(
                    color = crossColor,
                    start = androidx.compose.ui.geometry.Offset(x - crossSize, y),
                    end = androidx.compose.ui.geometry.Offset(x + crossSize, y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = crossColor,
                    start = androidx.compose.ui.geometry.Offset(x, y - crossSize),
                    end = androidx.compose.ui.geometry.Offset(x, y + crossSize),
                    strokeWidth = 1f
                )
                y += gridSize
            }
            x += gridSize
        }
    }
}
