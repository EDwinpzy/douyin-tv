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
}
