package com.dongpiandi.douyintv.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DouyinMediaSourceFactoryTest {
    @Test
    fun resolvesOnlyDouyinPlaybackEntrypointsWithSession() {
        assertTrue(
            requiresSessionMediaResolution(
                "https://www.douyin.com/aweme/v1/play/?video_id=1".toHttpUrl(),
            ),
        )
        assertTrue(
            requiresSessionMediaResolution(
                "https://www.iesdouyin.com/aweme/v1/playwm/?video_id=1".toHttpUrl(),
            ),
        )
        assertFalse(
            requiresSessionMediaResolution(
                "https://www.douyin.com/aweme/v1/web/aweme/detail/".toHttpUrl(),
            ),
        )
        assertFalse(
            requiresSessionMediaResolution(
                "https://v3-web.douyinvod.com/video.mp4".toHttpUrl(),
            ),
        )
    }

    @Test
    fun attachesCookieOnlyToDouyinHosts() {
        val origin = "https://www.douyin.com/aweme/v1/play/?video_id=1".toHttpUrl()
        val videoHost = "https://video-web-cn.douyin.com/video.mp4".toHttpUrl()
        val cdn = "https://v3-web.douyinvod.com/video.mp4".toHttpUrl()
        val external = "https://example.com/video.mp4".toHttpUrl()

        assertTrue(shouldAttachSessionCookie(origin, origin.host))
        assertTrue(shouldAttachSessionCookie(videoHost, origin.host))
        assertFalse(shouldAttachSessionCookie(cdn, origin.host))
        assertFalse(shouldAttachSessionCookie(external, origin.host))
    }

    @Test
    fun ignoresUntrustedPlaybackPaths() {
        assertFalse(
            requiresSessionMediaResolution(
                "https://example.com/aweme/v1/play/?video_id=1".toHttpUrl(),
            ),
        )
    }
}
