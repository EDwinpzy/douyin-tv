package com.dongpiandi.douyintv.ui

internal val ShortsSource.isSidebarPlayback: Boolean
    get() = this == ShortsSource.FAVORITES ||
        this == ShortsSource.WATCH_LATER ||
        this == ShortsSource.AUTHOR

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
