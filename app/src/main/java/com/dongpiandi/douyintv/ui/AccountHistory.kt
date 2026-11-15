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
        val localEntry = localById[video.id]
        val durationMs = localEntry?.progress?.durationMs
            ?.takeIf { it > 0L }
            ?: video.durationMs.coerceAtLeast(0L)
        val positionMs = resumePositionMs(
            local = localEntry?.progress,
            remotePositionMs = video.remoteProgressMs,
            durationMs = durationMs,
        )
        merged += PlaybackHistoryEntry(
            video = video,
            progress = PlaybackProgress(
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAtMs = localEntry?.progress?.updatedAtMs ?: 0L,
            ),
        )
    }
    local.forEach { entry ->
        if (seen.add(entry.video.id)) merged += entry
    }
    return merged
}

internal fun resumablePositionMs(progress: PlaybackProgress?): Long {
    if (progress == null || progress.durationMs <= 0L) return 0L
    if (progress.positionMs < 1_000L) return 0L
    if (progress.positionMs >= progress.durationMs - 2_000L) return 0L
    return progress.positionMs
}

internal fun resumePositionMs(
    local: PlaybackProgress?,
    remotePositionMs: Long,
    durationMs: Long,
): Long {
    val resolvedDuration = durationMs.takeIf { it > 0L }
        ?: local?.durationMs
        ?: 0L
    if (
        local != null &&
        resolvedDuration > 0L &&
        local.positionMs >= resolvedDuration - 2_000L
    ) {
        return 0L
    }
    return maxOf(
        resumablePositionMs(local),
        resumablePositionMs(
            PlaybackProgress(
                positionMs = remotePositionMs.coerceAtLeast(0L),
                durationMs = resolvedDuration,
            ),
        ),
    )
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

internal object PlaybackProgressSyncPolicy {
    const val MIN_PROGRESS_MS = 2_000L
    const val FORCE_MIN_INTERVAL_MS = 2_000L

    fun shouldWrite(
        positionMs: Long,
        durationMs: Long,
        nowMs: Long,
        lastWriteForVideoAtMs: Long?,
        force: Boolean,
    ): Boolean {
        if (durationMs <= 0L || positionMs <= 0L) return false
        if (!force) return false
        if (positionMs < MIN_PROGRESS_MS) return false
        if (lastWriteForVideoAtMs == null) return true
        return nowMs - lastWriteForVideoAtMs >= FORCE_MIN_INTERVAL_MS
    }

    fun persistedPositionMs(positionMs: Long, durationMs: Long): Long =
        if (durationMs > 0L && positionMs >= durationMs - 1_000L) 0L else positionMs
}
