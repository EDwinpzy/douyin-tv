package com.dongpiandi.douyintv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dongpiandi.douyintv.performance.LocalDevicePerformanceProfile

@Immutable
internal data class GlassStyle(
    val top: Color = Color(0xE61B2231),
    val bottom: Color = Color(0xE60B0E16),
    val border: Color = Color.White.copy(alpha = 0.18f),
    val sheen: Color = Color.White.copy(alpha = 0.09f),
    val shadowAlpha: Float = 0.34f,
)

@Composable
internal fun Modifier.glassSurface(
    shape: Shape,
    style: GlassStyle = GlassStyle(),
    elevation: Dp = 8.dp,
): Modifier {
    val lowTier = LocalDevicePerformanceProfile.current.lowTier
    val shadowed = if (lowTier) {
        this
    } else {
        this.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = style.shadowAlpha),
            spotColor = Color.Black.copy(alpha = style.shadowAlpha),
        )
    }
    val base = shadowed.background(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (lowTier) 0.09f else 0.1f),
                0.06f to style.top,
                0.34f to style.top.copy(alpha = style.top.alpha * 0.94f),
                0.74f to style.bottom,
                1f to style.bottom.copy(alpha = style.bottom.alpha * 0.92f),
            ),
            shape = shape,
        )

    if (lowTier) {
        return base.border(1.dp, style.border, shape)
    }

    return base
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    style.sheen.copy(
                        alpha = style.sheen.alpha * 1.55f,
                    ),
                    style.sheen.copy(alpha = style.sheen.alpha * 0.34f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.018f),
                ),
            ),
            shape = shape,
        )
        .background(
            brush = Brush.verticalGradient(
                0.64f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.16f),
            ),
            shape = shape,
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    style.border,
                    style.border.copy(alpha = style.border.alpha * 0.32f),
                    Color.Black.copy(alpha = 0.2f),
                ),
            ),
            shape = shape,
        )
}
