package com.dongpiandi.douyintv.ui

internal val ShortsSource.isSidebarPlayback: Boolean
    get() = this == ShortsSource.FAVORITES || this == ShortsSource.AUTHOR

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
