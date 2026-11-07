@file:androidx.media3.common.util.UnstableApi

package com.dongpiandi.douyintv.ui.screens

import android.content.Context
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRendererCapabilitiesList
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.dongpiandi.douyintv.model.ShortVideo
import com.dongpiandi.douyintv.network.DouyinMediaSourceFactory

internal class ShortsPlaybackEngine(context: Context) {
    private val appContext = context.applicationContext
    private val renderersFactory = DefaultRenderersFactory(appContext)
        .setEnableDecoderFallback(true)
    private val trackSelector = DefaultTrackSelector(appContext)
    private val bandwidthMeter = DefaultBandwidthMeter.Builder(appContext).build()
    private val allocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)
    private val loadControl = DefaultLoadControl.Builder()
        .setAllocator(allocator)
        .build()
    private val mediaSourceFactory = DouyinMediaSourceFactory.create(
        context = appContext,
        referer = DOUYIN_REFERER,
    )
    private val managedItems = linkedMapOf<String, ManagedItem>()
    private var currentIndex = C.INDEX_UNSET

    val player: ExoPlayer
    private val preloadManager: DefaultPreloadManager

    init {
        val playbackLooper = Looper.getMainLooper()
        player = ExoPlayer.Builder(appContext)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .setPlaybackLooper(playbackLooper)
            .build()
            .apply {
                setHandleAudioBecomingNoisy(true)
            }
        preloadManager = DefaultPreloadManager(
            TargetPreloadStatusControl<Int> { index ->
                ShortsPreloadPolicy.targetDurationMs(index - currentIndex)?.let { durationMs ->
                    DefaultPreloadManager.Status(
                        DefaultPreloadManager.Status.STAGE_LOADED_TO_POSITION_MS,
                        durationMs,
                    )
                }
            },
            mediaSourceFactory,
            trackSelector,
            bandwidthMeter,
            DefaultRendererCapabilitiesList.Factory(renderersFactory),
            allocator,
            playbackLooper,
        )
    }

    fun updatePreloadWindow(videos: List<ShortVideo>, index: Int) {
        if (videos.isEmpty() || index !in videos.indices) {
            clearPreloadWindow()
            return
        }
        currentIndex = index
        preloadManager.setCurrentPlayingIndex(index)

        val retainedIndices = ShortsPreloadPolicy.retainedIndices(index, videos.size)
        val iterator = managedItems.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next().value
            if (item.index !in retainedIndices) {
                preloadManager.remove(item.mediaItem)
                iterator.remove()
            }
        }

        for (nextIndex in ShortsPreloadPolicy.forwardIndices(index, videos.size)) {
            val video = videos[nextIndex]
            val url = video.playUrls.firstOrNull()?.takeIf(String::isNotBlank) ?: continue
            val mediaItem = mediaItem(video, url, urlIndex = 0)
            val existing = managedItems[mediaItem.mediaId]
            if (existing?.mediaItem == mediaItem) continue
            if (existing != null) {
                preloadManager.remove(existing.mediaItem)
            }
            preloadManager.add(mediaItem, nextIndex)
            managedItems[mediaItem.mediaId] = ManagedItem(mediaItem, nextIndex)
        }
        preloadManager.invalidate()
    }

    fun prepare(video: ShortVideo, urlIndex: Int) {
        val url = video.playUrls.getOrNull(urlIndex) ?: return
        val mediaItem = mediaItem(video, url, urlIndex)
        val preloadedSource = managedItems[mediaItem.mediaId]
            ?.takeIf { it.mediaItem == mediaItem }
            ?.let { preloadManager.getMediaSource(mediaItem) }
        player.setMediaSource(
            preloadedSource ?: mediaSourceFactory.createMediaSource(mediaItem),
        )
    }

    fun release() {
        player.release()
        preloadManager.release()
    }

    private fun clearPreloadWindow() {
        currentIndex = C.INDEX_UNSET
        managedItems.values.forEach { preloadManager.remove(it.mediaItem) }
        managedItems.clear()
        preloadManager.invalidate()
    }

    private fun mediaItem(video: ShortVideo, url: String, urlIndex: Int): MediaItem =
        MediaItem.Builder()
            .setMediaId("${video.id}:$urlIndex")
            .setUri(url)
            .build()

    private data class ManagedItem(
        val mediaItem: MediaItem,
        val index: Int,
    )

    private companion object {
        const val DOUYIN_REFERER = "https://www.douyin.com/"
    }
}
