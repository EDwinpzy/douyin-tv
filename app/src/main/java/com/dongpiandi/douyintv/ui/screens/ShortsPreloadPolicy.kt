package com.dongpiandi.douyintv.ui.screens

internal object ShortsPreloadPolicy {
    const val FORWARD_COUNT = 4

    private const val RETAIN_PREVIOUS_COUNT = 1

    fun forwardIndices(
        currentIndex: Int,
        size: Int,
        forwardCount: Int = FORWARD_COUNT,
    ): IntRange {
        if (size <= 0 || currentIndex !in 0 until size) return IntRange.EMPTY
        if (forwardCount <= 0) return IntRange.EMPTY
        val first = currentIndex + 1
        val last = (currentIndex + forwardCount).coerceAtMost(size - 1)
        return if (first > last) IntRange.EMPTY else first..last
    }

    fun retainedIndices(
        currentIndex: Int,
        size: Int,
        forwardCount: Int = FORWARD_COUNT,
    ): IntRange {
        if (size <= 0 || currentIndex !in 0 until size) return IntRange.EMPTY
        val first = (currentIndex - RETAIN_PREVIOUS_COUNT).coerceAtLeast(0)
        val last = (currentIndex + forwardCount.coerceAtLeast(0)).coerceAtMost(size - 1)
        return first..last
    }

    fun targetDurationMs(distance: Int): Long? = when (distance) {
        -1 -> 8_000L
        1 -> 8_000L
        2 -> 6_000L
        3 -> 4_000L
        4 -> 3_000L
        else -> null
    }
}
