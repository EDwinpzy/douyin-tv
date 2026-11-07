package com.dongpiandi.douyintv.model

data class ShortVideo(
    val id: String,
    val description: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val playUrls: List<String>,
    val coverUrl: String?,
    val durationMs: Long,
    val likeCount: Long,
    val commentCount: Long,
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
