package com.dongpiandi.douyintv.ui

import com.dongpiandi.douyintv.model.DanmakuMessage
import com.dongpiandi.douyintv.model.ShortComment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuTimelineTest {
    @Test
    fun fillsAfterLastOfficialMessageUntilVideoEnds() {
        val official = listOf(
            DanmakuMessage("official-1", "官方弹幕", 1_000),
        )

        val timeline = buildDanmakuTimeline(
            official = official,
            comments = listOf(comment("评论弹幕")),
            durationMs = 30_000,
        )

        val filler = timeline.filter { it.id.startsWith("comment-danmaku-") }
        assertTrue(filler.isNotEmpty())
        assertTrue(filler.all { it.offsetMs > 1_000 })
        assertTrue(filler.last().offsetMs > 25_000)
    }

    @Test
    fun startsFillingFromTheBeginningWithoutOfficialDanmaku() {
        val timeline = buildDanmakuTimeline(
            official = emptyList(),
            comments = listOf(comment("第一条")),
            durationMs = 10_000,
        )

        assertEquals(600L, timeline.first().offsetMs)
        assertTrue(timeline.last().offsetMs >= 8_000)
        assertTrue(timeline.all { it.offsetMs <= 9_200 })
    }

    @Test
    fun keepsOfficialMessagesOnlyWhenCommentsAreUnavailable() {
        val official = listOf(
            DanmakuMessage("official-1", "官方弹幕", 1_000),
        )

        assertEquals(
            official,
            buildDanmakuTimeline(
                official = official,
                comments = emptyList(),
                durationMs = 30_000,
            ),
        )
    }

    private fun comment(text: String) = ShortComment(
        id = text,
        text = text,
        nickname = "",
        avatarUrl = null,
        likeCount = 0,
        replyCount = 0,
        ipLabel = "",
    )
}
