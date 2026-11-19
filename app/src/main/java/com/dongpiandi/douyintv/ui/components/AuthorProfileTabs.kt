package com.dongpiandi.douyintv.ui.components

internal fun authorProfileTabLabels(
    videoCount: Int,
    collectionsLoaded: Boolean,
    collectionCount: Int,
): List<String> = listOf(
    "视频 $videoCount",
    if (collectionsLoaded) "合集 $collectionCount" else "合集",
)

internal fun collectionEpisodeLabel(index: Int, total: Int): String {
    val episode = index.coerceAtLeast(0) + 1
    return if (total > 0) "第 $episode 集 / 共 $total 集" else "第 $episode 集"
}

internal fun collectionEpisodeCountLabel(loaded: Int, total: Int): String = when {
    total > 0 && loaded in 1 until total -> "已加载 $loaded / $total 集"
    total > 0 -> "共 $total 集"
    else -> "共 $loaded 集"
}
