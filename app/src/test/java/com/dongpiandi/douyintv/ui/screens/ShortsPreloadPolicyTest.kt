package com.dongpiandi.douyintv.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsPreloadPolicyTest {
    @Test
    fun forwardIndicesPreloadsFourFollowingVideos() {
        assertEquals(1..4, ShortsPreloadPolicy.forwardIndices(currentIndex = 0, size = 10))
    }

    @Test
    fun forwardIndicesStopsAtEndOfList() {
        assertEquals(7..9, ShortsPreloadPolicy.forwardIndices(currentIndex = 6, size = 10))
        assertTrue(ShortsPreloadPolicy.forwardIndices(currentIndex = 9, size = 10).isEmpty())
    }

    @Test
    fun retainedRangeKeepsOnePreviousAndFourFollowingVideos() {
        assertEquals(2..7, ShortsPreloadPolicy.retainedIndices(currentIndex = 3, size = 10))
        assertEquals(0..4, ShortsPreloadPolicy.retainedIndices(currentIndex = 0, size = 10))
    }

    @Test
    fun targetDurationCoversPreviousAndForwardWindow() {
        assertEquals(8_000L, ShortsPreloadPolicy.targetDurationMs(-1))
        assertEquals(8_000L, ShortsPreloadPolicy.targetDurationMs(1))
        assertEquals(6_000L, ShortsPreloadPolicy.targetDurationMs(2))
        assertEquals(4_000L, ShortsPreloadPolicy.targetDurationMs(3))
        assertEquals(3_000L, ShortsPreloadPolicy.targetDurationMs(4))
        assertNull(ShortsPreloadPolicy.targetDurationMs(0))
        assertNull(ShortsPreloadPolicy.targetDurationMs(-2))
        assertNull(ShortsPreloadPolicy.targetDurationMs(5))
    }

    @Test
    fun lowTierWindowStopsAfterTwoFollowingVideos() {
        assertEquals(
            1..2,
            ShortsPreloadPolicy.forwardIndices(
                currentIndex = 0,
                size = 10,
                forwardCount = 2,
            ),
        )
        assertEquals(
            2..5,
            ShortsPreloadPolicy.retainedIndices(
                currentIndex = 3,
                size = 10,
                forwardCount = 2,
            ),
        )
    }
}
