package com.dongpiandi.douyintv.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTest {
    @Test
    fun clampsFractionToPlayableRange() {
        assertEquals(0.5f, PlaybackProgress(5_000, 10_000).fraction, 0.0001f)
        assertEquals(1f, PlaybackProgress(12_000, 10_000).fraction, 0.0001f)
        assertEquals(0f, PlaybackProgress(5_000, 0).fraction, 0.0001f)
    }
}
