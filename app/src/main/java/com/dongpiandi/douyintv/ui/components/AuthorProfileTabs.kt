package com.dongpiandi.douyintv.ui.components

internal fun authorProfileTabLabels(
    videoCount: Int,
    collectionsLoaded: Boolean,
    collectionCount: Int,
): List<String> = listOf(
    "视频 $videoCount",
    if (collectionsLoaded) "合集 $collectionCount" else "合集",
)
