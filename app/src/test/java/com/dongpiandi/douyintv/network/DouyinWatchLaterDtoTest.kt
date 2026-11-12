package com.dongpiandi.douyintv.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class DouyinWatchLaterDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun parsesWrappedWatchLaterItems() {
        val response = json.decodeFromString<WatchLaterListResponse>(
            """
            {
              "status_code": 0,
              "offset": 20,
              "has_more": true,
              "total": 21,
              "items": [
                {
                  "aweme": {
                    "aweme_id": "later-1",
                    "desc": "稍后再看一",
                    "video": {
                      "play_addr": {
                        "url_list": ["https://example.com/later-1.mp4"]
                      }
                    }
                  }
                },
                {
                  "aweme_info": {
                    "aweme_id": "later-2",
                    "desc": "稍后再看二",
                    "video": {
                      "play_addr": {
                        "url_list": ["https://example.com/later-2.mp4"]
                      }
                    }
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(true, response.hasMore)
        assertEquals(21L, response.total)
        assertEquals(
            listOf("later-1", "later-2"),
            response.videoDtos(json).map(AwemeDto::aweme_id),
        )
        assertEquals(20L, response.nextOffset(requestOffset = 0, pageSize = 2))
    }

    @Test
    fun parsesFlatAwemeListAndFallsBackToPageSize() {
        val response = json.decodeFromString<WatchLaterListResponse>(
            """
            {
              "status_code": 0,
              "has_more": 0,
              "aweme_list": [
                {
                  "aweme_id": "later-flat",
                  "desc": "平铺稍后再看",
                  "video": {
                    "play_addr": {
                      "url_list": ["https://example.com/later-flat.mp4"]
                    }
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf("later-flat"),
            response.videoDtos(json).map(AwemeDto::aweme_id),
        )
        assertEquals(false, response.hasMore)
        assertEquals(20L, response.nextOffset(requestOffset = 0, pageSize = 20))
        assertEquals(41L, response.nextOffset(requestOffset = 20, pageSize = 21))
    }

    @Test
    fun keepsDescendingTimestampOffsetForNextPage() {
        val response = json.decodeFromString<WatchLaterListResponse>(
            """
            {
              "status_code": 0,
              "has_more": true,
              "offset": 1794400000000
            }
            """.trimIndent(),
        )

        assertEquals(
            1794400000000L,
            response.nextOffset(requestOffset = 1794498393000, pageSize = 20),
        )
    }
}
