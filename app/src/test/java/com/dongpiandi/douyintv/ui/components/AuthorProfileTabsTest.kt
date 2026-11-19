package com.dongpiandi.douyintv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorProfileTabsTest {
    @Test
    fun keepsCollectionsTabBeforeLoadCompletes() {
        assertEquals(
            listOf("视频 12", "合集"),
            authorProfileTabLabels(
                videoCount = 12,
                collectionsLoaded = false,
                collectionCount = 0,
            ),
        )
    }

    @Test
    fun keepsEmptyCollectionsTabAfterLoad() {
        assertEquals(
            listOf("视频 12", "合集 0"),
            authorProfileTabLabels(
                videoCount = 12,
                collectionsLoaded = true,
                collectionCount = 0,
            ),
        )
    }

    @Test
    fun showsCollectionCountWhenCollectionsExist() {
        assertEquals(
            listOf("视频 12", "合集 15"),
            authorProfileTabLabels(
                videoCount = 12,
                collectionsLoaded = true,
                collectionCount = 15,
            ),
        )
    }

    @Test
    fun labelsEveryCollectionEpisode() {
        assertEquals("第 1 集 / 共 8 集", collectionEpisodeLabel(index = 0, total = 8))
        assertEquals("第 8 集 / 共 8 集", collectionEpisodeLabel(index = 7, total = 8))
    }

    @Test
    fun showsLoadedEpisodeProgress() {
        assertEquals("已加载 8 / 21 集", collectionEpisodeCountLabel(loaded = 8, total = 21))
        assertEquals("共 8 集", collectionEpisodeCountLabel(loaded = 8, total = 8))
        assertEquals("共 8 集", collectionEpisodeCountLabel(loaded = 8, total = 0))
    }
}
