package com.dongpiandi.douyintv.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
    val sec_uid: String = "",
    val avatar_thumb: UrlListDto? = null,
)

@Serializable
data class UserProfileResponse(
    val status_code: Int = 0,
    val status_msg: String? = null,
    val user: UserProfileDto? = null,
)

@Serializable
data class UserProfileDto(
    val nickname: String = "",
    val unique_id: String = "",
    val sec_uid: String = "",
    val short_id: String = "",
    val avatar_thumb: UrlListDto? = null,
    val follower_count: Long = 0,
    val following_count: Long = 0,
    val total_favorited: Long = 0,
    val signature: String = "",
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
data class FavoriteListResponse(
    val status_code: Int = 0,
    val status_msg: String? = null,
    val aweme_list: List<AwemeDto>? = null,
    val cursor: Long = 0,
    val max_cursor: Long = 0,
    val has_more: Int = 0,
)

@Serializable
data class AuthorPostResponse(
    val status_code: Int = 0,
    val status_msg: String? = null,
    val aweme_list: List<AwemeDto>? = null,
    val cursor: Long = 0,
    val max_cursor: Long = 0,
    val has_more: Int = 0,
)

@Serializable
data class FavoriteActionResponse(
    val status_code: Int = 0,
    val status_msg: String? = null,
)

@Serializable
data class AwemeStatsDto(
    val digg_count: Long = 0,
    val comment_count: Long = 0,
    val share_count: Long = 0,
)

@Serializable
data class MobileCommentListResponse(
    val comments: List<CommentDto>? = null,
    val status_code: MobileStatusDto? = null,
)

@Serializable
data class MobileStatusDto(
    @SerialName("StatusCode")
    val statusCode: Int = 0,
    @SerialName("StatusMsg")
    val statusMsg: String = "",
)

@Serializable
data class CommentDto(
    val cid: String = "",
    val text: String = "",
    val create_time: Long = 0,
    val digg_count: Long = 0,
    val reply_comment_total: Long = 0,
    val ip_label: String = "",
    val user: CommentUserDto? = null,
)

@Serializable
data class CommentUserDto(
    val nickname: String = "",
    val avatar_thumb: UrlListDto? = null,
)

@Serializable
data class DanmakuListResponse(
    val status_code: Int = 0,
    val status_msg: String? = null,
    val total: Long = 0,
    val danmaku_list: List<DanmakuDto>? = null,
)

@Serializable
data class DanmakuDto(
    val danmaku_id: String = "",
    val text: String = "",
    val offset_time: Long = 0,
    val digg_count: Long = 0,
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
