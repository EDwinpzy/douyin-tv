package com.dongpiandi.douyintv.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortsQueuePolicyTest {
    @Test
    fun initialQueueResets() {
        assertEquals(
            ShortsQueueUpdate.RESET,
            ShortsQueuePolicy.updateFor(listOf("a", "b"), emptyList()),
        )
    }

    @Test
    fun appendedPageReusesExistingQueue() {
        assertEquals(
            ShortsQueueUpdate.APPEND,
            ShortsQueuePolicy.updateFor(listOf("a", "b", "c"), listOf("a", "b")),
        )
    }

    @Test
    fun sameQueueOnlyChangesSelection() {
        assertEquals(
            ShortsQueueUpdate.REUSE,
            ShortsQueuePolicy.updateFor(listOf("a", "b", "c"), listOf("a", "b", "c")),
        )
    }

    @Test
    fun replacedOrShrunkFeedResetsQueue() {
        assertEquals(
            ShortsQueueUpdate.RESET,
            ShortsQueuePolicy.updateFor(listOf("x", "b"), listOf("a", "b")),
        )
        assertEquals(
            ShortsQueueUpdate.RESET,
            ShortsQueuePolicy.updateFor(listOf("a"), listOf("a", "b")),
        )
    }
}
