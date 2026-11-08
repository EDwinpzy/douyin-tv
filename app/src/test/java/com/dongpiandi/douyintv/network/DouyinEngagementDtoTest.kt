package com.dongpiandi.douyintv.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DouyinEngagementDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun parsesMobileCommentResponseStatusAndFields() {
        val response = json.decodeFromString<MobileCommentListResponse>(
            """
            {
              "comments": [
                {
                  "cid": "comment-1",
                  "text": "第一条评论",
                  "digg_count": 18,
                  "reply_comment_total": 3,
                  "ip_label": "上海",
                  "user": {
                    "nickname": "测试用户",
                    "avatar_thumb": {
                      "url_list": ["https://example.com/avatar.jpeg"]
                    }
                  }
                }
              ],
              "status_code": {
                "StatusCode": 0,
                "StatusMsg": ""
              }
            }
            """.trimIndent(),
        )

        assertEquals(0, response.status_code?.statusCode)
        val comment = response.comments.orEmpty().single()
        assertEquals("comment-1", comment.cid)
        assertEquals("第一条评论", comment.text)
        assertEquals("测试用户", comment.user?.nickname)
        assertEquals(18L, comment.digg_count)
    }

    @Test
    fun parsesDanmakuResponseWithOptionalList() {
        val response = json.decodeFromString<DanmakuListResponse>(
            """
            {
              "status_code": 0,
              "danmaku_list": [
                {
                  "danmaku_id": "danmaku-1",
                  "text": "弹幕内容",
                  "offset_time": 1200
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(0, response.status_code)
        val danmaku = response.danmaku_list.orEmpty().single()
        assertEquals("弹幕内容", danmaku.text)
        assertEquals(1200L, danmaku.offset_time)
        assertFalse(danmaku.danmaku_id.isBlank())
    }

    @Test
    fun parsesFavoritePageCursorAndAuthorSecUid() {
        val response = json.decodeFromString<FavoriteListResponse>(
            """
            {
              "status_code": 0,
              "has_more": 1,
              "max_cursor": 1720000000000,
              "aweme_list": [
                {
                  "aweme_id": "favorite-1",
                  "desc": "收藏视频",
                  "author": {
                    "nickname": "作者",
                    "sec_uid": "MS4wLjABAAAA-test"
                  },
                  "video": {
                    "play_addr": {
                      "url_list": ["https://example.com/video.mp4"]
                    },
                    "duration": 32000
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(0, response.status_code)
        assertEquals(1, response.has_more)
        assertEquals(1720000000000L, response.max_cursor)
        assertEquals(
            "MS4wLjABAAAA-test",
            response.aweme_list.orEmpty().single().author?.sec_uid,
        )
    }

    @Test
    fun parsesFavoriteActionFailureMessage() {
        val response = json.decodeFromString<FavoriteActionResponse>(
            """{"status_code": 8, "status_msg": "请先登录"}""",
        )

        assertEquals(8, response.status_code)
        assertEquals("请先登录", response.status_msg)
    }
}
