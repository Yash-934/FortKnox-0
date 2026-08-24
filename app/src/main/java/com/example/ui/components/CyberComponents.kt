package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalCyberColors

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    glowColor: Color = LocalCyberColors.current.glowColor,
    borderColor: Color = LocalCyberColors.current.border,
    backgroundColor: Color = LocalCyberColors.current.surface.copy(alpha = 0.85f),
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
        colors = listOf(
            glowColor.copy(alpha = if (LocalCyberColors.current.isCyberpunk) 0.6f else 0.3f),
            borderColor,
            borderColor,
            glowColor.copy(alpha = if (LocalCyberColors.current.isCyberpunk) 0.3f else 0.15f)
        )
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
    color: Color = LocalCyberColors.current.primary,
    textColor: Color = LocalCyberColors.current.onPrimary,
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
            disabledContainerColor = LocalCyberColors.current.surfaceHigh,
            disabledContentColor = LocalCyberColors.current.textMuted
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
    color: Color = LocalCyberColors.current.primary,
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
            .background(LocalCyberColors.current.surfaceVariant.copy(alpha = 0.3f))
            .border(
                1.dp,
                if (enabled) color.copy(alpha = if (LocalCyberColors.current.isCyberpunk) 0.8f else 0.6f) else LocalCyberColors.current.border,
                cutShape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) color else LocalCyberColors.current.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text.uppercase(),
                color = if (enabled) color else LocalCyberColors.current.textMuted,
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
    color: Color = LocalCyberColors.current.primary,
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
            .background(if (isCopied) LocalCyberColors.current.secondary.copy(alpha = 0.2f) else LocalCyberColors.current.surfaceVariant)
            .border(1.dp, if (isCopied) LocalCyberColors.current.secondary else LocalCyberColors.current.border, cutShape)
    ) {
        Icon(
            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = "Copy",
            tint = if (isCopied) LocalCyberColors.current.secondary else LocalCyberColors.current.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun CyberGridBackground(modifier: Modifier = Modifier) {
    val crossColor = LocalCyberColors.current.primary.copy(alpha = if (LocalCyberColors.current.isCyberpunk) 0.15f else 0.08f)
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSize = 40.dp.toPx()
        val crossSize = 2.dp.toPx()

        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
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
