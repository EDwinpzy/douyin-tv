package com.dongpiandi.douyintv.ui.components

import com.dongpiandi.douyintv.model.DanmakuMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuScheduleTest {
    @Test
    fun keepsEveryLaneWindowSeparated() {
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
            travelDurationMs = 1_000L,
        )

        assertEquals(messages.size, lanes.sumOf { it.size })
        lanes.forEach { lane ->
            lane.zipWithNext().forEach { (previous, next) ->
                assertTrue(next.startMs >= previous.startMs + 1_000L)
            }
        }
    }

    @Test
    fun neverStartsBeforeItsOriginalTimestamp() {
        val messages = listOf(
            DanmakuMessage("one", "第一条", 2_000L),
            DanmakuMessage("two", "第二条", 2_100L),
        )

        val scheduled = scheduleDanmaku(
            messages = messages,
            laneCount = 1,
            travelDurationMs = 500L,
        ).single()

        assertEquals(2_000L, scheduled[0].startMs)
        assertEquals(2_500L, scheduled[1].startMs)
    }
}
