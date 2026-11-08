package com.dongpiandi.douyintv.ui.components

import com.dongpiandi.douyintv.model.DanmakuMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuScheduleTest {
    @Test
    fun defaultsToThreeVisibleLanes() {
        val messages = List(6) { index ->
            DanmakuMessage(
                id = "danmaku-$index",
                text = "第 $index 条弹幕",
                offsetMs = index * 250L,
            )
        }

        val lanes = scheduleDanmaku(messages)

        assertEquals(3, lanes.size)
        assertEquals(messages.size, lanes.sumOf { it.size })
    }

    @Test
    fun preservesEveryOriginalTimestamp() {
        val messages = List(12) { index ->
            DanmakuMessage(
                id = "danmaku-$index",
                text = "第 $index 条弹幕",
                offsetMs = index * 250L,
            )
        }

        val lanes = scheduleDanmaku(
            messages = messages,
            laneCount = 3,
        )

        assertEquals(messages.size, lanes.sumOf { it.size })
        assertEquals(
            messages.map(DanmakuMessage::offsetMs),
            lanes.flatten()
                .sortedBy { scheduled -> scheduled.message.offsetMs }
                .map { scheduled -> scheduled.startMs },
        )
    }

    @Test
    fun spreadsSameTimeMessagesAcrossLanesWithoutShiftingThem() {
        val messages = listOf(
            DanmakuMessage("one", "第一条", 2_000L),
            DanmakuMessage("two", "第二条", 2_100L),
            DanmakuMessage("three", "第三条", 2_200L),
            DanmakuMessage("four", "第四条", 2_300L),
        )

        val scheduled = scheduleDanmaku(
            messages = messages,
            laneCount = 3,
        )

        assertEquals(messages.size, scheduled.sumOf { it.size })
        assertEquals(
            messages.map(DanmakuMessage::offsetMs),
            scheduled.flatten()
                .sortedBy { item -> item.message.offsetMs }
                .map { item -> item.startMs },
        )
    }
}
