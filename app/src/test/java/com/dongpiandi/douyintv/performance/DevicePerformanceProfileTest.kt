package com.dongpiandi.douyintv.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePerformanceProfileTest {
    @Test
    fun constrainedProjectorGetsLowTierIntervals() {
        val profile = DevicePerformanceProfile.forDevice(
            isLowRamDevice = false,
            memoryClassMb = 192,
            totalMemoryBytes = 2L * 1024L * 1024L * 1024L,
            supports64Bit = false,
        )

        assertTrue(profile.lowTier)
        assertEquals(66L, profile.danmakuFrameIntervalMs)
        assertEquals(5_000L, profile.playbackProgressIntervalMs)
        assertEquals(2, profile.preloadForwardCount)
    }

    @Test
    fun modernTvKeepsStandardQuality() {
        val profile = DevicePerformanceProfile.forDevice(
            isLowRamDevice = false,
            memoryClassMb = 512,
            totalMemoryBytes = 6L * 1024L * 1024L * 1024L,
            supports64Bit = true,
        )

        assertFalse(profile.lowTier)
        assertEquals(33L, profile.danmakuFrameIntervalMs)
        assertEquals(3_000L, profile.playbackProgressIntervalMs)
        assertEquals(4, profile.preloadForwardCount)
    }
}
