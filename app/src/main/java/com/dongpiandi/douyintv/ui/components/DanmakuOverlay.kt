package com.dongpiandi.douyintv.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.dongpiandi.douyintv.model.DanmakuMessage
import kotlinx.coroutines.delay

private const val LANE_COUNT = 3
private const val TRAVEL_DURATION_MS = 11_000L
private const val DEFAULT_REFRESH_INTERVAL_MS = 33L

internal data class ScheduledDanmaku(
    val message: DanmakuMessage,
    val startMs: Long,
)

internal fun scheduleDanmaku(
    messages: List<DanmakuMessage>,
    laneCount: Int = LANE_COUNT,
    travelDurationMs: Long = TRAVEL_DURATION_MS,
): List<List<ScheduledDanmaku>> {
    if (laneCount <= 0) return emptyList()
    val lanes = List(laneCount) { mutableListOf<ScheduledDanmaku>() }
    val laneFreeAt = LongArray(laneCount)
    val travel = travelDurationMs.coerceAtLeast(1L)
    val ordered = messages.asSequence()
        .filter { it.text.isNotBlank() }
        .distinctBy(DanmakuMessage::id)
        .sortedWith(compareBy(DanmakuMessage::offsetMs, DanmakuMessage::id))

    ordered.forEach { message ->
        var laneIndex = 0
        for (candidate in laneFreeAt.indices) {
            if (laneFreeAt[candidate] < laneFreeAt[laneIndex]) {
                laneIndex = candidate
            }
        }
        val startMs = maxOf(message.offsetMs.coerceAtLeast(0), laneFreeAt[laneIndex])
        lanes[laneIndex] += ScheduledDanmaku(message, startMs)
        laneFreeAt[laneIndex] = startMs + travel
    }
    return lanes
}

@Composable
fun DanmakuOverlay(
    messages: List<DanmakuMessage>,
    player: Player,
    modifier: Modifier = Modifier,
    refreshIntervalMs: Long = DEFAULT_REFRESH_INTERVAL_MS,
) {
    var positionMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0)) }
    val safeRefreshIntervalMs = refreshIntervalMs.coerceAtLeast(16L)

    LaunchedEffect(player, messages.isEmpty(), safeRefreshIntervalMs) {
        if (messages.isEmpty()) return@LaunchedEffect
        while (true) {
            val nextPosition = player.currentPosition.coerceAtLeast(0)
            if (nextPosition != positionMs) {
                positionMs = nextPosition
            }
            delay(if (player.isPlaying) safeRefreshIntervalMs else 120L)
        }
    }

    val lanes = remember(messages) { scheduleDanmaku(messages) }
    val density = LocalDensity.current
    val textSize = with(density) { 20.sp.toPx() }
    val horizontalPadding = with(density) { 8.dp.toPx() }
    val shadowOffset = with(density) { 1.5.dp.toPx() }
    val textPaint = remember(textSize) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            this.textSize = textSize
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }
    val shadowPaint = remember(textSize, shadowOffset) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(188, 0, 0, 0)
            this.textSize = textSize
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }
    val textWidthCache = remember(messages, textPaint) { mutableMapOf<String, Float>() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        if (lanes.isEmpty()) return@Canvas
        val currentPosition = positionMs
        val laneHeight = size.height / lanes.size

        lanes.forEachIndexed { laneIndex, laneMessages ->
            val scheduled = laneMessages.lastOrNull { item ->
                currentPosition >= item.startMs &&
                    currentPosition < item.startMs + TRAVEL_DURATION_MS
            } ?: return@forEachIndexed
            val elapsed = (currentPosition - scheduled.startMs).coerceAtLeast(0)
            val progress = (elapsed.toFloat() / TRAVEL_DURATION_MS).coerceIn(0f, 1f)
            val text = scheduled.message.text.replace('\n', ' ').let {
                if (it.length > 42) it.take(41) + "…" else it
            }
            val textWidth = textWidthCache.getOrPut(text) {
                textPaint.measureText(text)
            }
            val travel = size.width + textWidth + horizontalPadding * 2
            val x = size.width + horizontalPadding - progress * travel
            val baseline = laneIndex * laneHeight + laneHeight / 2f -
                (textPaint.ascent() + textPaint.descent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(
                text,
                x + shadowOffset,
                baseline + shadowOffset,
                shadowPaint,
            )
            drawContext.canvas.nativeCanvas.drawText(text, x, baseline, textPaint)
        }
    }
}
