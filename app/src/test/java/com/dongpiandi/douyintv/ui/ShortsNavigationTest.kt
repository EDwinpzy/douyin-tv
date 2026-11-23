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
        assertTrue(ShortsSource.WATCH_LATER.isSidebarPlayback)
        assertFalse(ShortsSource.RECOMMEND.isSidebarPlayback)
        assertFalse(ShortsSource.SEARCH.isSidebarPlayback)
    }

    @Test
    fun remembersMyHomeTabAndSelectedVideoForReturn() {
        assertEquals(
            SidebarPlaybackReturn(
                kind = SidebarKind.MY_HOME,
                myHomeTab = MyHomeTab.FAVORITES,
                videoId = "favorite",
            ),
            myHomeSidebarPlaybackReturn(
                tab = MyHomeTab.FAVORITES,
                videoId = "favorite",
            ),
        )
    }

    @Test
    fun remembersAuthorVideoForReturn() {
        assertEquals(
            SidebarPlaybackReturn(
                kind = SidebarKind.AUTHOR,
                authorTab = 0,
                videoId = "author-video",
            ),
            authorSidebarVideoPlaybackReturn("author-video"),
        )
    }

    @Test
    fun remembersCollectionCardForReturn() {
        assertEquals(
            SidebarPlaybackReturn(
                kind = SidebarKind.AUTHOR,
                authorTab = 1,
                collectionId = "collection",
            ),
            authorSidebarCollectionPlaybackReturn("collection"),
        )
    }

    @Test
    fun reversesAuthorPlaybackQueueAndKeepsSelection() {
        val queue = buildShortsPlaybackQueue(
            video = sampleVideo("b"),
            sourceVideos = listOf(
                sampleVideo("a"),
                sampleVideo("b"),
                sampleVideo("c"),
            ),
            reverseOrder = true,
        )

        assertEquals(listOf("c", "b", "a"), queue.videos.map(ShortVideo::id))
        assertEquals(1, queue.index)
    }

    @Test
    fun collectionStartsFromOriginalLastVideoWhenReversed() {
        val videos = listOf(
            sampleVideo("first"),
            sampleVideo("middle"),
            sampleVideo("last"),
        )
        val queue = buildShortsPlaybackQueue(
            video = videos.last(),
            sourceVideos = videos,
            reverseOrder = true,
        )

        assertEquals("last", queue.videos.first().id)
        assertEquals(0, queue.index)
    }

    @Test
    fun preservesSourceOrderForOtherPlaybackQueues() {
        val queue = buildShortsPlaybackQueue(
            video = sampleVideo("b"),
            sourceVideos = listOf(
                sampleVideo("a"),
                sampleVideo("b"),
                sampleVideo("c"),
            ),
            reverseOrder = false,
        )

        assertEquals(listOf("a", "b", "c"), queue.videos.map(ShortVideo::id))
        assertEquals(1, queue.index)
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
