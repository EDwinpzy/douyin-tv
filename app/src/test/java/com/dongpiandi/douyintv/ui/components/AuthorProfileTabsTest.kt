package com.dongpiandi.douyintv.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorProfileTabsTest {
    @Test
    fun showsCollectionsTabBeforeLoadCompletes() {
        assertTrue(
            shouldShowAuthorCollectionsTab(
                collectionsLoaded = false,
                collectionCount = 0,
            ),
        )
    }

    @Test
    fun showsCollectionsTabWhenCollectionsExist() {
        assertTrue(
            shouldShowAuthorCollectionsTab(
                collectionsLoaded = true,
                collectionCount = 15,
            ),
        )
    }

    @Test
    fun hidesCollectionsTabWhenLoadedEmpty() {
        assertFalse(
            shouldShowAuthorCollectionsTab(
                collectionsLoaded = true,
                collectionCount = 0,
            ),
        )
    }
}
