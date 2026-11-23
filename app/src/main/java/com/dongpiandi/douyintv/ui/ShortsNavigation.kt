package com.dongpiandi.douyintv.ui

import com.dongpiandi.douyintv.model.ShortVideo

internal val ShortsSource.isSidebarPlayback: Boolean
    get() = this == ShortsSource.FAVORITES ||
        this == ShortsSource.WATCH_LATER ||
        this == ShortsSource.AUTHOR

internal data class ShortsPlaybackQueue(
    val videos: List<ShortVideo>,
    val index: Int,
)

internal fun buildShortsPlaybackQueue(
    video: ShortVideo,
    sourceVideos: List<ShortVideo>,
    reverseOrder: Boolean,
): ShortsPlaybackQueue {
    val videos = when {
        sourceVideos.isEmpty() -> listOf(video)
        reverseOrder -> sourceVideos.reversed()
        else -> sourceVideos
    }
    val index = videos.indexOfFirst { it.id == video.id }.takeIf { it >= 0 } ?: 0
    return ShortsPlaybackQueue(videos = videos, index = index)
}

internal fun recommendationReturnAfterPlayback(
    currentShorts: ShortsUiState,
    existingReturn: ShortsUiState?,
    nextSource: ShortsSource,
): ShortsUiState? {
    if (!nextSource.isSidebarPlayback) return existingReturn
    return existingReturn ?: currentShorts.takeIf {
        it.source == ShortsSource.RECOMMEND
    }
}

internal fun myHomeSidebarPlaybackReturn(
    tab: MyHomeTab,
    videoId: String,
): SidebarPlaybackReturn = SidebarPlaybackReturn(
    kind = SidebarKind.MY_HOME,
    myHomeTab = tab,
    videoId = videoId,
)

internal fun authorSidebarVideoPlaybackReturn(videoId: String): SidebarPlaybackReturn =
    SidebarPlaybackReturn(
        kind = SidebarKind.AUTHOR,
        authorTab = 0,
        videoId = videoId,
    )

internal fun authorSidebarCollectionPlaybackReturn(
    collectionId: String,
): SidebarPlaybackReturn = SidebarPlaybackReturn(
    kind = SidebarKind.AUTHOR,
    authorTab = 1,
    collectionId = collectionId,
)
