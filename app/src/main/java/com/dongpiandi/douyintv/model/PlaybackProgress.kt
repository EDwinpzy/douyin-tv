package com.dongpiandi.douyintv.model

data class PlaybackProgress(
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long = 0,
) {
    val fraction: Float
        get() = if (durationMs <= 0L) {
            0f
        } else {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
}

data class PlaybackHistoryEntry(
    val video: ShortVideo,
    val progress: PlaybackProgress,
)
