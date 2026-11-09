package com.dongpiandi.douyintv.ui.screens

internal enum class ShortsQueueUpdate {
    RESET,
    APPEND,
    REUSE,
}

internal object ShortsQueuePolicy {
    fun updateFor(videoIds: List<String>, queuedVideoIds: List<String>): ShortsQueueUpdate {
        if (queuedVideoIds.isEmpty()) return ShortsQueueUpdate.RESET
        if (videoIds == queuedVideoIds) return ShortsQueueUpdate.REUSE
        if (videoIds.size > queuedVideoIds.size &&
            videoIds.take(queuedVideoIds.size) == queuedVideoIds
        ) {
            return ShortsQueueUpdate.APPEND
        }
        return ShortsQueueUpdate.RESET
    }
}
