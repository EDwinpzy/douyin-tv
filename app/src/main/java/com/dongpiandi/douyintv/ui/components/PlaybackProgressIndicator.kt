package com.dongpiandi.douyintv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.dongpiandi.douyintv.ui.theme.Accent
import kotlinx.coroutines.delay

@Composable
fun PlaybackProgressIndicator(
    player: Player,
    videoId: String,
    fallbackDurationMs: Long,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
) {
    var progress by remember(videoId) { mutableFloatStateOf(0f) }
    var durationMs by remember(videoId) { mutableLongStateOf(0L) }

    LaunchedEffect(player, videoId, fallbackDurationMs) {
        while (true) {
            val duration = player.duration.takeIf { it > 0L } ?: fallbackDurationMs
            durationMs = duration.coerceAtLeast(0L)
            progress = if (duration > 0L) {
                (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            delay(1_000L)
        }
    }

    if (durationMs > 0L) {
        VideoProgressBar(
            progress = progress,
            modifier = modifier,
            height = height,
        )
    }
}

@Composable
fun VideoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.17f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(Accent),
        )
    }
}
