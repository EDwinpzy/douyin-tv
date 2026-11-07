package com.dongpiandi.douyintv.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.dongpiandi.douyintv.model.DanmakuMessage
import kotlinx.coroutines.delay

private const val LANE_COUNT = 5
private const val TRAVEL_DURATION_MS = 7_000L
private val LaneHeight = 32.dp

@Composable
fun DanmakuOverlay(
    messages: List<DanmakuMessage>,
    player: Player,
    modifier: Modifier = Modifier,
) {
    var positionMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0)) }

    LaunchedEffect(player, messages.isEmpty()) {
        if (messages.isEmpty()) return@LaunchedEffect
        while (true) {
            withFrameNanos {
                val nextPosition = player.currentPosition.coerceAtLeast(0)
                if (nextPosition != positionMs) {
                    positionMs = nextPosition
                }
            }
            if (!player.isPlaying) {
                delay(200)
            }
        }
    }

    val lanes = remember(messages) {
        val grouped = Array(LANE_COUNT) { mutableListOf<DanmakuMessage>() }
        messages
            .asSequence()
            .filter { it.text.isNotBlank() }
            .sortedBy(DanmakuMessage::offsetMs)
            .forEachIndexed { index, message ->
                grouped[index % LANE_COUNT] += message
            }
        grouped.map { lane -> lane.sortedBy(DanmakuMessage::offsetMs) }
    }
    val active = remember(lanes, positionMs) {
        lanes.mapNotNull { lane ->
            lane.lastOrNull { message ->
                positionMs >= message.offsetMs &&
                    positionMs < message.offsetMs + TRAVEL_DURATION_MS
            }
        }
    }

    val density = LocalDensity.current
    val textSize = with(density) { 15.sp.toPx() }
    val laneHeight = with(density) { LaneHeight.toPx() }
    val pillHeight = with(density) { 25.dp.toPx() }
    val horizontalPadding = with(density) { 10.dp.toPx() }
    val verticalInset = with(density) { 3.dp.toPx() }
    val cornerRadius = with(density) { 12.dp.toPx() }
    val textPaint = remember(textSize) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            this.textSize = textSize
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(LaneHeight * LANE_COUNT)
            .clipToBounds(),
    ) {
        active.forEachIndexed { lane, message ->
            val elapsed = (positionMs - message.offsetMs).coerceAtLeast(0)
            val progress = (elapsed.toFloat() / TRAVEL_DURATION_MS).coerceIn(0f, 1f)
            val text = message.text.replace('\n', ' ').let {
                if (it.length > 42) it.take(41) + "…" else it
            }
            val textWidth = textPaint.measureText(text)
            val x = size.width - progress * (size.width + textWidth + horizontalPadding * 2)
            val top = lane * laneHeight + verticalInset
            val baseline = top + pillHeight / 2f -
                (textPaint.ascent() + textPaint.descent()) / 2f
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.58f),
                topLeft = Offset(x - horizontalPadding, top),
                size = Size(textWidth + horizontalPadding * 2, pillHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            )
            drawContext.canvas.nativeCanvas.drawText(text, x, baseline, textPaint)
        }
    }
}
