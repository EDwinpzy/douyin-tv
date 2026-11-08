package com.dongpiandi.douyintv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortVideoAspectRatioTest {
    @Test
    fun usesVideoDimensionsForStableFrameSize() {
        val landscape = video(width = 1920, height = 1080)
        val portrait = video(width = 1080, height = 1920)

        assertEquals(16f / 9f, landscape.aspectRatio ?: 0f, 0.0001f)
        assertEquals(9f / 16f, portrait.aspectRatio ?: 0f, 0.0001f)
    }

    @Test
    fun leavesRatioUnknownWhenDimensionsAreMissing() {
        assertNull(video(width = 0, height = 1080).aspectRatio)
        assertNull(video(width = 1920, height = 0).aspectRatio)
    }

    private fun video(width: Int, height: Int) = ShortVideo(
        id = "video",
        description = "",
        authorName = "",
        authorSecUid = "",
        authorAvatarUrl = null,
        playUrls = listOf("https://example.com/video.mp4"),
        coverUrl = null,
        videoWidth = width,
        videoHeight = height,
        durationMs = 10_000,
        likeCount = 0,
        commentCount = 0,
    )
}
