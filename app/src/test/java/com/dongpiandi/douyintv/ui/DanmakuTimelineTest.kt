package com.dongpiandi.douyintv.ui

import com.dongpiandi.douyintv.model.DanmakuMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuTimelineTest {
    @Test
    fun preservesOriginalOffsetsWithoutFillingGaps() {
        val official = listOf(
            DanmakuMessage("first", "开头弹幕", 1_000),
            DanmakuMessage("last", "结尾弹幕", 25_000),
        )

        val timeline = buildDanmakuTimeline(
            official = official,
            durationMs = 30_000,
        )

        assertEquals(official, timeline)
    }

    @Test
    fun removesExactDuplicatesButKeepsRealRepeatedTextLater() {
        val official = listOf(
            DanmakuMessage("one", "重复弹幕", 1_000),
            DanmakuMessage("one", "重复弹幕", 1_000),
            DanmakuMessage("two", "重复弹幕", 8_000),
        )

        val timeline = buildDanmakuTimeline(
            official = official,
            durationMs = 30_000,
        )

        assertEquals(2, timeline.size)
        assertEquals(listOf(1_000L, 8_000L), timeline.map(DanmakuMessage::offsetMs))
    }

    @Test
    fun dropsMessagesOutsideTheTimeline() {
        val official = listOf(
            DanmakuMessage("valid", "有效弹幕", 1_000),
            DanmakuMessage("late", "越界弹幕", 31_000),
        )

        val timeline = buildDanmakuTimeline(
            official = official,
            durationMs = 30_000,
        )

        assertEquals(listOf("valid"), timeline.map(DanmakuMessage::id))
    }

    @Test
    fun keepsTimelineEmptyWhenThereAreNoOfficialMessages() {
        assertEquals(
            emptyList<DanmakuMessage>(),
            buildDanmakuTimeline(official = emptyList(), durationMs = 30_000),
        )
    }
}
