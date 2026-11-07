package com.dongpiandi.douyintv.network

import kotlinx.serialization.Serializable

@Serializable
data class ChannelFeedResponse(
    val status_code: Int = 0,
    val status_msg: String? = null,
    val aweme_list: List<AwemeDto> = emptyList(),
)

@Serializable
data class AwemeDto(
    val aweme_id: String = "",
    val desc: String = "",
    val aweme_type: Int = 0,
    val author: AuthorDto? = null,
    val video: VideoDto? = null,
    val statistics: AwemeStatsDto? = null,
    val images: List<ImageDto>? = null,
)

@Serializable
data class AuthorDto(
    val nickname: String = "",
    val avatar_thumb: UrlListDto? = null,
)

@Serializable
data class VideoDto(
    val play_addr: UrlListDto? = null,
    val cover: UrlListDto? = null,
    val duration: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
data class UrlListDto(
    val url_list: List<String> = emptyList(),
    val uri: String = "",
)

@Serializable
data class AwemeStatsDto(
    val digg_count: Long = 0,
    val comment_count: Long = 0,
    val share_count: Long = 0,
)

@Serializable
data class ImageDto(
    val url_list: List<String> = emptyList(),
)

@Serializable
data class LiveFeedResponse(
    val status_code: Int = 0,
    val data: List<LiveFeedCardDto> = emptyList(),
)

@Serializable
data class LiveFeedCardDto(
    val data: LiveRoomDto? = null,
)

@Serializable
data class LiveEnterResponse(
    val status_code: Int = 0,
    val data: LiveEnterDataDto? = null,
)

@Serializable
data class LiveEnterDataDto(
    val status_code: Int = 0,
    val data: List<LiveRoomDto> = emptyList(),
)

@Serializable
data class LiveRoomDto(
    val id_str: String = "",
    val title: String = "",
    val status: Int = 0,
    val cover: UrlListDto? = null,
    val owner: LiveOwnerDto? = null,
    val room_view_stats: LiveViewStatsDto? = null,
    val stats: LiveStatsDto? = null,
    val stream_url: LiveStreamDto? = null,
)

@Serializable
data class LiveOwnerDto(
    val nickname: String = "",
    val web_rid: String = "",
    val avatar: List<String> = emptyList(),
)

@Serializable
data class LiveViewStatsDto(
    val display_short: String = "",
    val display_middle: String = "",
    val display_long: String = "",
    val is_hidden: Boolean = false,
)

@Serializable
data class LiveStatsDto(
    val user_count_str: String = "",
    val total_user_str: String = "",
)

@Serializable
data class LiveStreamDto(
    val hls_pull_url_map: Map<String, String> = emptyMap(),
    val hls_pull_url: String = "",
    val flv_pull_url: Map<String, String> = emptyMap(),
)
