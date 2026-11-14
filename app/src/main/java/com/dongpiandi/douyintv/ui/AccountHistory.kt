package com.dongpiandi.douyintv.ui

import com.dongpiandi.douyintv.model.PlaybackHistoryEntry
import com.dongpiandi.douyintv.model.PlaybackProgress
import com.dongpiandi.douyintv.model.ShortVideo

internal fun mergeAccountHistory(
    local: List<PlaybackHistoryEntry>,
    account: List<ShortVideo>,
): List<PlaybackHistoryEntry> {
    if (account.isEmpty()) return local
    val localById = local.associateBy { it.video.id }
    val seen = HashSet<String>(local.size + account.size)
    val merged = ArrayList<PlaybackHistoryEntry>(local.size + account.size)
    account.forEach { video ->
        if (video.id.isBlank() || !seen.add(video.id)) return@forEach
        merged += localById[video.id]?.copy(video = video) ?: PlaybackHistoryEntry(
            video = video,
            progress = PlaybackProgress(
                positionMs = 0L,
                durationMs = video.durationMs.coerceAtLeast(0L),
            ),
        )
    }
    local.forEach { entry ->
        if (seen.add(entry.video.id)) merged += entry
    }
    return merged
}

internal object AccountHistoryWritePolicy {
    const val MIN_WATCH_MS = 2_000L
    const val MAX_WATCH_MS = 5_000L
    const val WRITE_INTERVAL_MS = 30_000L
    const val VIDEO_COOLDOWN_MS = 10 * 60_000L

    fun thresholdMs(durationMs: Long): Long =
        minOf(MAX_WATCH_MS, durationMs, maxOf(MIN_WATCH_MS, durationMs / 5))

    fun shouldWrite(
        positionMs: Long,
        durationMs: Long,
        nowMs: Long,
        lastWriteAtMs: Long,
        lastWriteForVideoAtMs: Long?,
    ): Boolean {
        if (durationMs <= 0L || positionMs < thresholdMs(durationMs)) return false
        if (lastWriteAtMs > 0L && nowMs - lastWriteAtMs < WRITE_INTERVAL_MS) {
            return false
        }
        if (
            lastWriteForVideoAtMs != null &&
            nowMs - lastWriteForVideoAtMs < VIDEO_COOLDOWN_MS
        ) {
            return false
        }
        return true
    }
}
