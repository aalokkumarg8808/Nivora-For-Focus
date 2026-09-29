package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun SeedlingPlant(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    progress: Float = 1f, // 0f to 1f growth stage
    isDark: Boolean = false
) {
    // Gentle breathing / swaying animation
    val infiniteTransition = rememberInfiniteTransition(label = "plantSway")
    val swayOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    val effectiveDark = isDark || PlantTheme.isDark
    val softGreenVein = PlantTheme.colors.accentSoft

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val centerX = w / 2f
        val centerY = h / 2f

        // Growth scaling
        val growthScale = 0.6f + (progress.coerceIn(0f, 1f) * 0.4f)
        val seedColor = if (effectiveDark) Color(0xFFC48858) else Color(0xFFE08D4C)
        val leafPrimary = if (effectiveDark) Color(0xFF22C55E) else PlantPrimary
        val leafSecondary = if (effectiveDark) Color(0xFF16A34A) else PlantPrimaryDark
        val outlineColor = if (effectiveDark) Color(0xFF0F1714) else Color(0xFF1A2621)

        // Ground / Seed shadow
        drawOval(
            color = if (isDark) Color(0x33000000) else Color(0x1F000000),
            topLeft = Offset(centerX - w * 0.22f, h * 0.85f),
            size = Size(w * 0.44f, h * 0.10f)
        )

        // Seed base (oval shaped nut/seed)
        val seedWidth = w * 0.28f * growthScale
        val seedHeight = h * 0.22f * growthScale
        val seedTop = h * 0.72f - (seedHeight * 0.5f)

        drawOval(
            color = seedColor,
            topLeft = Offset(centerX - seedWidth / 2f, seedTop),
            size = Size(seedWidth, seedHeight)
        )
        // Seed outline
        drawOval(
            color = outlineColor,
            topLeft = Offset(centerX - seedWidth / 2f, seedTop),
            size = Size(seedWidth, seedHeight),
            style = Stroke(width = 3.5f)
        )

        // Stem
        val stemTopY = h * 0.42f + (1f - growthScale) * (h * 0.15f)
        val stemPath = Path().apply {
            moveTo(centerX, seedTop + seedHeight * 0.2f)
            cubicTo(
                centerX + swayOffset,
                seedTop - h * 0.10f,
                centerX + swayOffset * 1.5f,
                stemTopY + h * 0.10f,
                centerX + swayOffset * 2f,
                stemTopY
            )
        }
        drawPath(
            path = stemPath,
            color = leafSecondary,
            style = Stroke(width = 7.5f)
        )
        drawPath(
            path = stemPath,
            color = outlineColor,
            style = Stroke(width = 2f)
        )

        // Leaves sprouting from top of stem
        val stemTipX = centerX + swayOffset * 2f
        val stemTipY = stemTopY

        // Right Leaf (curved upward-right)
        val rightLeafPath = Path().apply {
            moveTo(stemTipX, stemTipY + 4f)
            cubicTo(
                stemTipX + w * 0.10f,
                stemTipY - h * 0.05f,
                stemTipX + w * 0.38f * growthScale,
                stemTipY - h * 0.20f * growthScale,
                stemTipX + w * 0.42f * growthScale,
                stemTipY - h * 0.05f * growthScale
            )
            cubicTo(
                stemTipX + w * 0.32f * growthScale,
                stemTipY + h * 0.14f * growthScale,
                stemTipX + w * 0.14f,
                stemTipY + h * 0.06f,
                stemTipX,
                stemTipY + 4f
            )
            close()
        }

        drawPath(path = rightLeafPath, color = leafPrimary, style = Fill)
        drawPath(path = rightLeafPath, color = outlineColor, style = Stroke(width = 3.5f))

        // Left Leaf (curved upward-left)
        val leftLeafPath = Path().apply {
            moveTo(stemTipX, stemTipY + 4f)
            cubicTo(
                stemTipX - w * 0.10f,
                stemTipY - h * 0.02f,
                stemTipX - w * 0.35f * growthScale,
                stemTipY - h * 0.14f * growthScale,
                stemTipX - w * 0.40f * growthScale,
                stemTipY + h * 0.02f * growthScale
            )
            cubicTo(
                stemTipX - w * 0.30f * growthScale,
                stemTipY + h * 0.18f * growthScale,
                stemTipX - w * 0.12f,
                stemTipY + h * 0.08f,
                stemTipX,
                stemTipY + 4f
            )
            close()
        }

        drawPath(path = leftLeafPath, color = leafPrimary, style = Fill)
        drawPath(path = leftLeafPath, color = outlineColor, style = Stroke(width = 3.5f))

        // Leaf vein highlight
        val veinPath = Path().apply {
            moveTo(stemTipX, stemTipY)
            lineTo(stemTipX + w * 0.25f * growthScale, stemTipY - h * 0.05f * growthScale)
        }
        drawPath(path = veinPath, color = softGreenVein, style = Stroke(width = 2.5f))
    }
}
