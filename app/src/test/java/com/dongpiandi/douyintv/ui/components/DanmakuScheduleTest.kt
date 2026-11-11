package com.dongpiandi.douyintv.ui.components

import com.dongpiandi.douyintv.model.DanmakuMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuScheduleTest {
    @Test
    fun defaultsToFiveVisibleLanes() {
        val messages = List(6) { index ->
            DanmakuMessage(
                id = "danmaku-$index",
                text = "第 $index 条弹幕",
                offsetMs = index * 250L,
            )
        }

        val lanes = scheduleDanmaku(messages)

        assertEquals(5, lanes.size)
        assertEquals(5, lanes.sumOf { lane -> lane.size })
    }

    @Test
    fun preservesOriginalTimestampsForScheduledMessages() {
        val messages = List(12) { index ->
            DanmakuMessage(
                id = "danmaku-$index",
                text = "第 $index 条弹幕",
                offsetMs = index * 20_000L,
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
    fun spreadsNearbyMessagesAcrossLanesAndSkipsWhenAllLanesAreBusy() {
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

        assertEquals(3, scheduled.sumOf { it.size })
        assertEquals(
            messages.take(3).map(DanmakuMessage::offsetMs),
            scheduled.flatten()
                .sortedBy { item -> item.message.offsetMs }
                .map { item -> item.startMs },
        )
    }

    @Test
    fun keepsALaneOccupiedUntilItsMessageFinishesScrolling() {
        val messages = listOf(
            DanmakuMessage("first", "第一条", 1_000L),
            DanmakuMessage("second", "第二条", 1_500L),
            DanmakuMessage("third", "第三条", 19_000L),
        )

        val lanes = scheduleDanmaku(
            messages = messages,
            laneCount = 1,
        )

        assertEquals(2, lanes.single().size)
        assertEquals("first", lanes.single()[0].message.id)
        assertEquals("third", lanes.single()[1].message.id)
        assertEquals("first", activeDanmaku(lanes.single(), 1_500L).single().message.id)
        assertEquals("third", activeDanmaku(lanes.single(), 19_500L).single().message.id)
    }
}
