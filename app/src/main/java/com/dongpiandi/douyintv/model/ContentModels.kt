package com.dongpiandi.douyintv.model

data class ShortVideo(
    val id: String,
    val description: String,
    val authorName: String,
    val authorSecUid: String,
    val authorAvatarUrl: String?,
    val playUrls: List<String>,
    val coverUrl: String?,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val durationMs: Long,
    val likeCount: Long,
    val commentCount: Long,
) {
    val aspectRatio: Float?
        get() = if (videoWidth > 0 && videoHeight > 0) {
            videoWidth.toFloat() / videoHeight.toFloat()
        } else {
            null
        }
}

data class ShortComment(
    val id: String,
    val text: String,
    val nickname: String,
    val avatarUrl: String?,
    val likeCount: Long,
    val replyCount: Long,
    val ipLabel: String,
)

data class DanmakuMessage(
    val id: String,
    val text: String,
    val offsetMs: Long,
)

data class CommentPage(
    val comments: List<ShortComment>,
    val cursor: Long,
    val hasMore: Boolean,
    val total: Long,
)

data class VideoPage(
    val videos: List<ShortVideo>,
    val cursor: Long,
    val hasMore: Boolean,
)

data class VideoCollection(
    val id: String,
    val name: String,
    val description: String,
    val coverUrl: String?,
    val videoCount: Int,
    val playCount: Long,
)

data class CollectionPage(
    val collections: List<VideoCollection>,
    val cursor: Long,
    val hasMore: Boolean,
)

data class SearchPage(
    val videos: List<ShortVideo>,
    val cursor: Long,
    val hasMore: Boolean,
    val searchId: String,
    val backtrace: String,
    val verificationRequired: Boolean = false,
)

data class UserProfile(
    val nickname: String,
    val uniqueId: String,
    val secUid: String,
    val avatarUrl: String?,
    val followerCount: Long,
    val followingCount: Long,
    val totalFavorited: Long,
    val likingCount: Long,
    val signature: String,
)

data class LiveRoom(
    val id: String,
    val webRid: String,
    val title: String,
    val ownerName: String,
    val ownerAvatarUrl: String?,
    val coverUrl: String?,
    val viewerText: String,
)

data class LivePlayback(
    val roomId: String,
    val streamUrl: String,
    val title: String,
    val ownerName: String,
)
