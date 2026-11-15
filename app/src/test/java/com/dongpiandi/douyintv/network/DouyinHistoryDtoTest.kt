package com.dongpiandi.douyintv.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DouyinHistoryDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun acceptsChinesePercentageTextInHistoryProgress() {
        val response = json.decodeFromString<VideoListResponse>(
            """
            {
              "status_code": 0,
              "aweme_list": [
                {
                  "aweme_id": "history-1",
                  "history_progress_rate": "已看73%",
                  "history_info": {
                    "play_progress": "已看45%"
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val aweme = response.aweme_list.orEmpty().single()
        assertEquals(0.73, parseProgressRate(aweme.history_progress_rate)!!, 0.0001)
        assertEquals(
            0.45,
            parseProgressRate(aweme.history_info?.play_progress)!!,
            0.0001,
        )
    }

    @Test
    fun acceptsNumericAndPlainPercentageProgress() {
        assertEquals(0.52, parseProgressRate(JsonPrimitive(0.52))!!, 0.0001)
        assertEquals(0.52, parseProgressRate(JsonPrimitive("52%"))!!, 0.0001)
        assertNull(parseProgressRate(JsonPrimitive("暂无进度")))
    }
}
