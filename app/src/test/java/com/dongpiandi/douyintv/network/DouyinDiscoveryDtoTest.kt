package com.dongpiandi.douyintv.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DouyinDiscoveryDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun parsesAuthorMixListStatsAndCover() {
        val response = json.decodeFromString<MixListResponse>(
            """
            {
              "status_code": 0,
              "has_more": 1,
              "cursor": 18,
              "mix_infos": [
                {
                  "mix_id": "mix-1",
                  "mix_name": "回忆录",
                  "cover_url": {
                    "url_list": ["https://example.com/mix-cover.jpeg"]
                  },
                  "statis": {
                    "play_vv": 3196577,
                    "updated_to_episode": 33
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, response.has_more)
        assertEquals(18L, response.cursor)
        val mix = response.mix_infos.orEmpty().single()
        assertEquals("mix-1", mix.mix_id)
        assertEquals("回忆录", mix.mix_name)
        assertEquals(3196577L, mix.statis?.play_vv)
        assertEquals(33, mix.statis?.updated_to_episode)
        assertEquals(
            "https://example.com/mix-cover.jpeg",
            mix.cover_url?.url_list?.single(),
        )
    }

    @Test
    fun parsesPublicLikedVideosPage() {
        val response = json.decodeFromString<VideoListResponse>(
            """
            {
              "status_code": 0,
              "has_more": 1,
              "max_cursor": 1720000000000,
              "aweme_list": [
                {
                  "aweme_id": "liked-1",
                  "desc": "公开喜欢的视频",
                  "author": {
                    "nickname": "作者",
                    "sec_uid": "MS4wLjABAAAA-liked"
                  },
                  "video": {
                    "play_addr": {
                      "url_list": ["https://example.com/liked.mp4"]
                    },
                    "duration": 12000
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, response.has_more)
        assertEquals(1720000000000L, response.max_cursor)
        assertEquals("liked-1", response.aweme_list.orEmpty().single().aweme_id)
    }

    @Test
    fun parsesSearchResultsAndNextPageToken() {
        val response = json.decodeFromString<SearchResponse>(
            """
            {
              "status_code": 0,
              "data": [
                {
                  "type": 1,
                  "aweme_info": {
                    "aweme_id": "search-1",
                    "desc": "搜索结果视频",
                    "author": {
                      "nickname": "搜索作者",
                      "sec_uid": "MS4wLjABAAAA-search"
                    },
                    "video": {
                      "play_addr": {
                        "url_list": ["https://example.com/search.mp4"]
                      },
                      "duration": 15000
                    }
                  }
                }
              ],
              "business_config": {
                "has_more": 1,
                "next_page": {
                  "cursor": 20,
                  "search_id": "search-id-1",
                  "backtrace": "trace-1"
                }
              }
            }
            """.trimIndent(),
        )

        val item = response.data.orEmpty().single()
        assertEquals("search-1", item.aweme_info?.aweme_id)
        assertEquals(1, response.business_config?.has_more)
        assertEquals(20L, response.business_config?.next_page?.cursor)
        assertEquals("search-id-1", response.business_config?.next_page?.search_id)
        assertTrue(response.business_config?.next_page?.backtrace == "trace-1")
    }

    @Test
    fun parsesSearchVerificationState() {
        val response = json.decodeFromString<SearchResponse>(
            """
            {
              "status_code": 0,
              "data": [],
              "search_nil_info": {
                "search_nil_type": "verify_check"
              }
            }
            """.trimIndent(),
        )

        assertEquals("verify_check", response.search_nil_info?.search_nil_type)
        assertTrue(response.data.orEmpty().isEmpty())
    }

    @Test
    fun parsesAuthorLikingCount() {
        val response = json.decodeFromString<UserProfileResponse>(
            """
            {
              "status_code": 0,
              "user": {
                "nickname": "作者",
                "sec_uid": "MS4wLjABAAAA-author",
                "favoriting_count": 286
              }
            }
            """.trimIndent(),
        )

        assertEquals(286L, response.user?.favoriting_count)
    }

    @Test
    fun parsesAuthorFollowMetadata() {
        val feed = json.decodeFromString<ChannelFeedResponse>(
            """
            {
              "status_code": 0,
              "aweme_list": [
                {
                  "aweme_id": "follow-1",
                  "author": {
                    "nickname": "作者",
                    "uid": "uid-author",
                    "sec_uid": "MS4wLjABAAAA-author",
                    "follow_status": 2
                  },
                  "video": {
                    "play_addr": {
                      "url_list": ["https://example.com/follow.mp4"]
                    }
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val author = feed.aweme_list.single().author
        assertEquals("uid-author", author?.uid)
        assertEquals(2, author?.follow_status)

        val followAction = json.decodeFromString<FollowUserResponse>(
            """{"status_code":0,"follow_status":1}""",
        )
        assertEquals(1, followAction.follow_status)
    }
}
