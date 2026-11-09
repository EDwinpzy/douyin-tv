@file:androidx.media3.common.util.UnstableApi

package com.dongpiandi.douyintv.ui.screens

import android.content.Context
import android.os.HandlerThread
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRendererCapabilitiesList
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.dongpiandi.douyintv.data.SessionStore
import com.dongpiandi.douyintv.model.ShortVideo
import com.dongpiandi.douyintv.network.DouyinMediaSourceFactory

internal class ShortsPlaybackEngine(
    context: Context,
    private val preloadForwardCount: Int = ShortsPreloadPolicy.FORWARD_COUNT,
) {
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
        cookieHeader = SessionStore(appContext).cookieHeader(),
        cacheMedia = true,
    )
    private val managedItems = linkedMapOf<String, ManagedItem>()
    private var currentIndex = C.INDEX_UNSET
    private var queuedVideoIds = emptyList<String>()
    private var queuedUrlIndex = -1
    private val playbackThread = HandlerThread("ShortsPlayback").apply { start() }

    val player: ExoPlayer
    private val preloadManager: DefaultPreloadManager

    init {
        val playbackLooper = playbackThread.looper
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

        val retainedIndices = ShortsPreloadPolicy.retainedIndices(
            currentIndex = index,
            size = videos.size,
            forwardCount = preloadForwardCount,
        )
        val iterator = managedItems.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next().value
            if (item.index !in retainedIndices) {
                preloadManager.remove(item.mediaItem)
                iterator.remove()
            }
        }

        val previousIndex = index - 1
        if (previousIndex >= 0) {
            addPreloadItem(videos[previousIndex], previousIndex)
        }
        for (nextIndex in ShortsPreloadPolicy.forwardIndices(
            currentIndex = index,
            size = videos.size,
            forwardCount = preloadForwardCount,
        )) {
            addPreloadItem(videos[nextIndex], nextIndex)
        }
        preloadManager.invalidate()
    }

    fun syncQueue(videos: List<ShortVideo>, index: Int, urlIndex: Int) {
        if (videos.isEmpty() || index !in videos.indices) return
        val mediaItems = videos.mapIndexed { videoIndex, video ->
            val itemUrlIndex = if (videoIndex == index) urlIndex else 0
            val url = video.playUrls.getOrNull(itemUrlIndex) ?: return
            mediaItem(video, url, itemUrlIndex)
        }
        val videoIds = videos.map(ShortVideo::id)
        when (ShortsQueuePolicy.updateFor(videoIds, queuedVideoIds)) {
            ShortsQueueUpdate.RESET -> {
                player.setMediaItems(mediaItems, index, 0L)
                queuedUrlIndex = urlIndex
                player.prepare()
            }

            ShortsQueueUpdate.APPEND -> player.addMediaItems(mediaItems.drop(queuedVideoIds.size))
            ShortsQueueUpdate.REUSE -> {
                if (player.currentMediaItemIndex != index) {
                    player.seekTo(index, 0L)
                }
            }
        }
        queuedVideoIds = videoIds
        if (urlIndex != queuedUrlIndex && player.currentMediaItemIndex == index) {
            player.replaceMediaItem(index, mediaItems[index])
            queuedUrlIndex = urlIndex
        }
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
    }

    fun release() {
        clearPreloadWindow()
        preloadManager.release()
        player.release()
        playbackThread.quitSafely()
    }

    private fun clearPreloadWindow() {
        currentIndex = C.INDEX_UNSET
        managedItems.values.forEach { preloadManager.remove(it.mediaItem) }
        managedItems.clear()
        preloadManager.invalidate()
    }

    private fun addPreloadItem(video: ShortVideo, index: Int) {
        val url = video.playUrls.firstOrNull()?.takeIf(String::isNotBlank) ?: return
        val mediaItem = mediaItem(video, url, urlIndex = 0)
        val existing = managedItems[mediaItem.mediaId]
        if (existing?.mediaItem == mediaItem) return
        if (existing != null) {
            preloadManager.remove(existing.mediaItem)
        }
        preloadManager.add(mediaItem, index)
        managedItems[mediaItem.mediaId] = ManagedItem(mediaItem, index)
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
