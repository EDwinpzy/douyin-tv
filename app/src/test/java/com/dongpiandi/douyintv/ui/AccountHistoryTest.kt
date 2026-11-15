package com.dongpiandi.douyintv.ui

import com.dongpiandi.douyintv.model.PlaybackHistoryEntry
import com.dongpiandi.douyintv.model.PlaybackProgress
import com.dongpiandi.douyintv.model.ShortVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountHistoryTest {
    @Test
    fun mergesAccountOrderFirstAndKeepsLocalProgress() {
        val local = listOf(
            entry("a", positionMs = 3_000L),
            entry("local", positionMs = 1_000L),
        )
        val account = listOf(
            video("b"),
            video("a", playUrls = listOf("https://example.com/fresh-a.mp4")),
        )

        val merged = mergeAccountHistory(local, account)

        assertEquals(listOf("b", "a", "local"), merged.map { it.video.id })
        assertEquals(0L, merged[0].progress.positionMs)
        assertEquals(3_000L, merged[1].progress.positionMs)
        assertEquals("https://example.com/fresh-a.mp4", merged[1].video.playUrls.single())
        assertEquals(1_000L, merged[2].progress.positionMs)
    }

    @Test
    fun deduplicatesAccountItemsAndDropsBlankIds() {
        val local = listOf(entry("b", positionMs = 2_000L))
        val account = listOf(video("b"), video("b"), video(""))

        val merged = mergeAccountHistory(local, account)

        assertEquals(1, merged.size)
        assertEquals("b", merged.single().video.id)
        assertEquals(2_000L, merged.single().progress.positionMs)
    }

    @Test
    fun keepsLocalListWhenAccountHistoryIsEmpty() {
        val local = listOf(entry("local", positionMs = 1_000L))

        assertSame(local, mergeAccountHistory(local, emptyList()))
    }

    @Test
    fun usesCloudPlaybackProgressForResume() {
        val local = listOf(entry("local", positionMs = 2_000L))
        val account = listOf(
            video("local", remoteProgressMs = 7_000L),
            video("cloud", remoteProgressMs = 4_000L),
        )

        val merged = mergeAccountHistory(local, account)

        assertEquals(7_000L, merged[0].progress.positionMs)
        assertEquals(4_000L, merged[1].progress.positionMs)
        assertEquals(6_000L, resumePositionMs(null, 6_000L, 10_000L))
        assertEquals(0L, resumePositionMs(null, 9_500L, 10_000L))
    }

    @Test
    fun writesPlaybackProgressOnlyOnFlushAndResetsCompletedVideos() {
        assertFalse(
            PlaybackProgressSyncPolicy.shouldWrite(
                positionMs = 2_000L,
                durationMs = 200_000L,
                nowMs = 100_000L,
                lastWriteForVideoAtMs = null,
                force = false,
            ),
        )
        assertTrue(
            PlaybackProgressSyncPolicy.shouldWrite(
                positionMs = 8_000L,
                durationMs = 200_000L,
                nowMs = 110_000L,
                lastWriteForVideoAtMs = null,
                force = true,
            ),
        )
        assertFalse(
            PlaybackProgressSyncPolicy.shouldWrite(
                positionMs = 8_000L,
                durationMs = 200_000L,
                nowMs = 101_000L,
                lastWriteForVideoAtMs = 100_000L,
                force = true,
            ),
        )
        assertEquals(
            0L,
            PlaybackProgressSyncPolicy.persistedPositionMs(199_500L, 200_000L),
        )
        assertEquals(
            18_000L,
            PlaybackProgressSyncPolicy.persistedPositionMs(18_000L, 20_000L),
        )
        assertTrue(
            PlaybackProgressSyncPolicy.shouldWrite(
                positionMs = 8_000L,
                durationMs = 20_000L,
                nowMs = 200_000L,
                lastWriteForVideoAtMs = null,
                force = true,
            ),
        )
        assertFalse(
            PlaybackProgressSyncPolicy.shouldWrite(
                positionMs = 1_500L,
                durationMs = 120_000L,
                nowMs = 200_000L,
                lastWriteForVideoAtMs = null,
                force = true,
            ),
        )
    }

    @Test
    fun computesWatchThresholdFromDuration() {
        assertEquals(2_000L, AccountHistoryWritePolicy.thresholdMs(3_000L))
        assertEquals(3_000L, AccountHistoryWritePolicy.thresholdMs(15_000L))
        assertEquals(5_000L, AccountHistoryWritePolicy.thresholdMs(60_000L))
        assertEquals(500L, AccountHistoryWritePolicy.thresholdMs(500L))
        assertTrue(
            AccountHistoryWritePolicy.shouldWrite(
                positionMs = 1_500L,
                durationMs = 1_500L,
                nowMs = 100_000L,
                lastWriteAtMs = 0L,
                lastWriteForVideoAtMs = null,
            ),
        )
    }

    @Test
    fun requiresThresholdAndCooldownsBeforeWriting() {
        assertFalse(
            AccountHistoryWritePolicy.shouldWrite(
                positionMs = 1_999L,
                durationMs = 15_000L,
                nowMs = 100_000L,
                lastWriteAtMs = 0L,
                lastWriteForVideoAtMs = null,
            ),
        )
        assertTrue(
            AccountHistoryWritePolicy.shouldWrite(
                positionMs = 3_000L,
                durationMs = 15_000L,
                nowMs = 100_000L,
                lastWriteAtMs = 0L,
                lastWriteForVideoAtMs = null,
            ),
        )
        assertFalse(
            AccountHistoryWritePolicy.shouldWrite(
                positionMs = 6_000L,
                durationMs = 15_000L,
                nowMs = 120_000L,
                lastWriteAtMs = 100_000L,
                lastWriteForVideoAtMs = null,
            ),
        )
        assertFalse(
            AccountHistoryWritePolicy.shouldWrite(
                positionMs = 6_000L,
                durationMs = 15_000L,
                nowMs = 200_000L,
                lastWriteAtMs = 100_000L,
                lastWriteForVideoAtMs = 190_000L,
            ),
        )
        assertTrue(
            AccountHistoryWritePolicy.shouldWrite(
                positionMs = 6_000L,
                durationMs = 15_000L,
                nowMs = 800_000L,
                lastWriteAtMs = 100_000L,
                lastWriteForVideoAtMs = 190_000L,
            ),
        )
    }

    private fun entry(id: String, positionMs: Long): PlaybackHistoryEntry =
        PlaybackHistoryEntry(
            video = video(id),
            progress = PlaybackProgress(
                positionMs = positionMs,
                durationMs = 10_000L,
            ),
        )

    private fun video(
        id: String,
        playUrls: List<String> = listOf("https://example.com/$id.mp4"),
        remoteProgressMs: Long = 0L,
    ): ShortVideo = ShortVideo(
        id = id,
        description = "",
        authorName = "",
        authorSecUid = "",
        authorAvatarUrl = null,
        playUrls = playUrls,
        coverUrl = null,
        durationMs = 10_000L,
        likeCount = 0L,
        commentCount = 0L,
        remoteProgressMs = remoteProgressMs,
    )
}
