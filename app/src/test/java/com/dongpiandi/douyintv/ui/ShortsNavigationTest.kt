package com.dongpiandi.douyintv.ui

import com.dongpiandi.douyintv.model.ShortVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsNavigationTest {
    private val recommendation = ShortsUiState(
        videos = listOf(sampleVideo("recommendation")),
        source = ShortsSource.RECOMMEND,
    )

    @Test
    fun storesRecommendationBeforeSidebarPlayback() {
        val saved = recommendationReturnAfterPlayback(
            currentShorts = recommendation,
            existingReturn = null,
            nextSource = ShortsSource.FAVORITES,
        )

        assertSame(recommendation, saved)
    }

    @Test
    fun keepsFirstRecommendationSnapshotAcrossSidebarSources() {
        val saved = recommendationReturnAfterPlayback(
            currentShorts = recommendation.copy(source = ShortsSource.AUTHOR),
            existingReturn = recommendation,
            nextSource = ShortsSource.FAVORITES,
        )

        assertSame(recommendation, saved)
    }

    @Test
    fun doesNotStoreRecommendationForSearchPlayback() {
        val saved = recommendationReturnAfterPlayback(
            currentShorts = recommendation,
            existingReturn = null,
            nextSource = ShortsSource.SEARCH,
        )

        assertEquals(null, saved)
    }

    @Test
    fun recognizesSourcesOpenedFromSidebars() {
        assertTrue(ShortsSource.AUTHOR.isSidebarPlayback)
        assertTrue(ShortsSource.FAVORITES.isSidebarPlayback)
        assertFalse(ShortsSource.RECOMMEND.isSidebarPlayback)
        assertFalse(ShortsSource.SEARCH.isSidebarPlayback)
    }

    private fun sampleVideo(id: String) = ShortVideo(
        id = id,
        description = id,
        authorName = "author",
        authorSecUid = "sec",
        authorAvatarUrl = null,
        playUrls = listOf("https://example.com/$id.mp4"),
        coverUrl = null,
        durationMs = 1_000L,
        likeCount = 0,
        commentCount = 0,
    )
}
