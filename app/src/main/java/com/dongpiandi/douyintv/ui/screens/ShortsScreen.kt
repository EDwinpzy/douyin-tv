@file:androidx.media3.common.util.UnstableApi
@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dongpiandi.douyintv.ui.screens

import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dongpiandi.douyintv.model.ShortVideo
import com.dongpiandi.douyintv.performance.DevicePerformanceProfile
import com.dongpiandi.douyintv.ui.MainTab
import com.dongpiandi.douyintv.ui.EngagementUiState
import com.dongpiandi.douyintv.ui.ShortsUiState
import com.dongpiandi.douyintv.ui.components.CommentsPanel
import com.dongpiandi.douyintv.ui.components.commentsPanelWidth
import com.dongpiandi.douyintv.ui.components.DanmakuOverlay
import com.dongpiandi.douyintv.ui.components.GlassStyle
import com.dongpiandi.douyintv.ui.components.PlaybackProgressIndicator
import com.dongpiandi.douyintv.ui.components.TopChrome
import com.dongpiandi.douyintv.ui.components.glassSurface
import com.dongpiandi.douyintv.ui.formatCount
import com.dongpiandi.douyintv.ui.isSidebarPlayback
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.Panel
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val OVERLAY_AUTO_HIDE_DELAY_MS = 4_000L
private const val DANMAKU_HEIGHT_FRACTION = 0.15f
private const val RAPID_SWITCH_WINDOW_MS = 280L
private const val RAPID_SWITCH_SETTLE_MS = 220L
private const val SEEK_STEP_MS = 5_000L
private const val SEEK_FAST_STEP_MS = 10_000L
private const val SEEK_REPEAT_INTERVAL_MS = 120L
private const val SEEK_ACCELERATION_AFTER_MS = 1_200L

@Composable
fun ShortsScreen(
    state: ShortsUiState,
    engagement: EngagementUiState,
    overlayVisible: Boolean,
    sidePanelVisible: Boolean,
    searchVisible: Boolean,
    isLoggedIn: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onAccountClick: () -> Unit,
    onSearchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onShowOverlay: () -> Unit,
    onHideOverlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePause: () -> Unit,
    onEnded: () -> Unit,
    onPlaybackError: (String) -> Unit,
    onRefresh: () -> Unit,
    onPrepareEngagement: (ShortVideo) -> Unit,
    onOpenComments: (ShortVideo) -> Unit,
    onCloseComments: () -> Unit,
    onLoadMoreComments: (ShortVideo) -> Unit,
    onRetryComments: (ShortVideo) -> Unit,
    favoriteIds: Set<String>,
    favoritePendingIds: Set<String>,
    onToggleFavorite: (ShortVideo) -> Unit,
    onOpenAuthor: (ShortVideo) -> Unit,
    onPlaybackProgress: (videoId: String, positionMs: Long, durationMs: Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val focusManager = LocalFocusManager.current
    val videoFocusRequester = remember { FocusRequester() }
    val chromeFocusRequester = remember { FocusRequester() }
    val bottomActionsFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    var focusChromeRequested by remember { mutableStateOf(false) }
    var focusBottomActionsRequested by remember { mutableStateOf(false) }
    var selectedBottomAction by remember { mutableIntStateOf(0) }
    val overlayInteractionSignals = remember { Channel<Unit>(Channel.CONFLATED) }
    var videoFocused by remember { mutableStateOf(false) }
    val performanceProfile = remember(context) {
        DevicePerformanceProfile.detect(context)
    }
    val playbackEngine = remember(context, performanceProfile, isLoggedIn) {
        ShortsPlaybackEngine(
            context = context,
            preloadForwardCount = performanceProfile.preloadForwardCount,
        )
    }
    val player = playbackEngine.player
    val current = state.current
    var urlIndex by remember(current?.id) { mutableIntStateOf(0) }
    var readyVideoId by remember { mutableStateOf<String?>(null) }
    val latestVideo by rememberUpdatedState(current)
    val latestUrlIndex by rememberUpdatedState(urlIndex)
    val latestPaused by rememberUpdatedState(state.paused)
    val latestOnEnded by rememberUpdatedState(onEnded)
    val latestOnError by rememberUpdatedState(onPlaybackError)
    val latestOnProgress by rememberUpdatedState(onPlaybackProgress)
    val activeUrl = current?.playUrls?.getOrNull(urlIndex)
    val commentsVisible = engagement.visible
    val lastPrepareRequestAt = remember { LongArray(1) }
    val seekStartedAt = remember { LongArray(1) }
    val lastSeekAt = remember { LongArray(1) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    readyVideoId = latestVideo?.id
                }
                if (playbackState == Player.STATE_ENDED) {
                    val video = latestVideo
                    if (video != null && player.duration > 0L) {
                        latestOnProgress(video.id, player.duration, player.duration)
                    }
                    latestOnEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    latestOnEnded()
                } else if (player.playbackState == Player.STATE_READY) {
                    readyVideoId = mediaItem?.mediaId?.substringBefore(':')
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val video = latestVideo
                val nextIndex = latestUrlIndex + 1
                if (video != null && nextIndex < video.playUrls.size) {
                    urlIndex = nextIndex
                } else {
                    latestOnError("视频播放失败，请刷新后重试")
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            playbackEngine.release()
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                player.pause()
            } else if (event == Lifecycle.Event.ON_RESUME && !latestPaused) {
                player.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(current?.id, activeUrl, state.videos) {
        if (activeUrl.isNullOrBlank()) {
            player.stop()
            player.clearMediaItems()
            return@LaunchedEffect
        }
        val video = current ?: return@LaunchedEffect
        readyVideoId = null
        val now = SystemClock.elapsedRealtime()
        val rapidSwitch =
            lastPrepareRequestAt[0] != 0L &&
                now - lastPrepareRequestAt[0] < RAPID_SWITCH_WINDOW_MS
        lastPrepareRequestAt[0] = now
        if (rapidSwitch) {
            delay(RAPID_SWITCH_SETTLE_MS)
        }
        playbackEngine.syncQueue(state.videos, state.index, urlIndex)
        player.playWhenReady = !state.paused
    }

    LaunchedEffect(state.videos, state.index, readyVideoId) {
        if (readyVideoId == current?.id) {
            playbackEngine.updatePreloadWindow(state.videos, state.index)
        }
    }

    LaunchedEffect(state.paused) {
        player.playWhenReady = !state.paused
        if (state.paused) player.pause() else player.play()
    }

    PlaybackProgressReporter(
        player = player,
        videoId = current?.id,
        fallbackDurationMs = current?.durationMs ?: 0L,
        intervalMs = performanceProfile.playbackProgressIntervalMs,
        onProgress = latestOnProgress,
    )

    LaunchedEffect(current?.id) {
        if (current != null) onPrepareEngagement(current)
    }

    LaunchedEffect(
        current?.id,
        overlayVisible,
        commentsVisible,
        sidePanelVisible,
        searchVisible,
        lifecycleState,
    ) {
        if (
            current != null &&
            !overlayVisible &&
            !commentsVisible &&
            !sidePanelVisible &&
            !searchVisible &&
            lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            withFrameNanos { }
            focusManager.clearFocus(force = true)
            videoFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(
        overlayVisible,
        current?.id,
        state.paused,
        commentsVisible,
        sidePanelVisible,
        searchVisible,
    ) {
        if (
            !overlayVisible ||
            current == null ||
            state.paused ||
            commentsVisible ||
            sidePanelVisible ||
            searchVisible
        ) {
            return@LaunchedEffect
        }
        while (overlayInteractionSignals.tryReceive().isSuccess) {
            Unit
        }
        while (true) {
            val interacted = withTimeoutOrNull(OVERLAY_AUTO_HIDE_DELAY_MS) {
                overlayInteractionSignals.receive()
            }
            if (interacted == null) break
        }
        focusManager.clearFocus(force = true)
        onHideOverlay()
        withFrameNanos { }
        videoFocusRequester.requestFocus()
    }

    LaunchedEffect(
        overlayVisible,
        focusChromeRequested,
        commentsVisible,
        sidePanelVisible,
        searchVisible,
    ) {
        if (
            overlayVisible &&
            focusChromeRequested &&
            !commentsVisible &&
            !sidePanelVisible &&
            !searchVisible
        ) {
            withFrameNanos { }
            delay(16)
            chromeFocusRequester.requestFocus()
            focusChromeRequested = false
        }
    }

    LaunchedEffect(
        overlayVisible,
        focusBottomActionsRequested,
        commentsVisible,
        sidePanelVisible,
        searchVisible,
    ) {
        if (
            overlayVisible &&
            focusBottomActionsRequested &&
            current != null &&
            !commentsVisible &&
            !sidePanelVisible &&
            !searchVisible
        ) {
            withFrameNanos { }
            delay(16)
            bottomActionsFocusRequester.requestFocus()
            focusBottomActionsRequested = false
        } else if (
            !overlayVisible &&
            !commentsVisible &&
            !sidePanelVisible &&
            !searchVisible
        ) {
            focusBottomActionsRequested = false
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) retryFocusRequester.requestFocus()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (overlayVisible && event.type == KeyEventType.KeyDown) {
                    overlayInteractionSignals.trySend(Unit)
                }
                val seekKey = event.key == Key.DirectionLeft ||
                    event.key == Key.DirectionRight
                if (
                    !seekKey ||
                    !videoFocused ||
                    overlayVisible ||
                    commentsVisible ||
                    searchVisible ||
                    current == null ||
                    state.error != null
                ) {
                    return@onPreviewKeyEvent false
                }
                if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                val now = SystemClock.elapsedRealtime()
                if (event.nativeKeyEvent.repeatCount == 0) {
                    seekStartedAt[0] = now
                } else if (now - lastSeekAt[0] < SEEK_REPEAT_INTERVAL_MS) {
                    return@onPreviewKeyEvent true
                }
                lastSeekAt[0] = now
                val step = if (now - seekStartedAt[0] >= SEEK_ACCELERATION_AFTER_MS) {
                    SEEK_FAST_STEP_MS
                } else {
                    SEEK_STEP_MS
                }
                val direction = if (event.key == Key.DirectionLeft) -1L else 1L
                val maxPosition = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                player.seekTo(
                    (player.currentPosition + direction * step).coerceIn(0L, maxPosition),
                )
                true
            },
    ) {
        val infoWidth = (maxWidth * 0.28f).coerceAtMost(280.dp)
        val danmakuHeight = (maxHeight * DANMAKU_HEIGHT_FRACTION)
            .coerceAtLeast(72.dp)
        val rightPanelWidth = when {
            commentsVisible -> commentsPanelWidth(maxWidth)
            sidePanelVisible -> maxWidth * (1f / 3f)
            else -> 0.dp
        }
        val hasRightPanel = commentsVisible || sidePanelVisible
        val videoRegionWidth = if (hasRightPanel) {
            (maxWidth - rightPanelWidth).coerceAtLeast(1.dp)
        } else {
            maxWidth
        }
        val videoRegionModifier = if (hasRightPanel) {
            Modifier
                .fillMaxHeight()
                .width(videoRegionWidth)
                .align(Alignment.CenterStart)
        } else {
            Modifier.fillMaxSize()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(videoFocusRequester)
                .onFocusChanged { videoFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (!videoFocused) return@onPreviewKeyEvent false
                    if (state.error != null) return@onPreviewKeyEvent false
                    if (overlayVisible) {
                        if (event.type == KeyEventType.KeyDown) {
                            focusChromeRequested = true
                        }
                        return@onPreviewKeyEvent true
                    }
                    val handled = event.key == Key.DirectionUp ||
                        event.key == Key.DirectionDown ||
                        event.key == Key.DirectionCenter ||
                        event.key == Key.Enter ||
                        event.key == Key.Menu ||
                        event.key == Key.MediaPlayPause ||
                        event.key == Key.MediaPlay ||
                        event.key == Key.MediaPause
                    if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent handled
                    if (!handled) return@onPreviewKeyEvent false
                    if (event.nativeKeyEvent.repeatCount > 0) return@onPreviewKeyEvent true

                    when (event.key) {
                        Key.DirectionUp -> {
                            if (state.index > 0) {
                                onHideOverlay()
                                onPrevious()
                            } else {
                                onShowOverlay()
                                focusChromeRequested = true
                            }
                        }

                        Key.DirectionDown -> {
                            onHideOverlay()
                            onNext()
                        }
                        Key.DirectionCenter, Key.Enter, Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                            onTogglePause()
                        }

                        Key.Menu -> {
                            onShowOverlay()
                            focusChromeRequested = true
                        }
                        else -> Unit
                    }
                    true
                }
                .focusable(
                    enabled = !commentsVisible && !sidePanelVisible && !searchVisible,
                ),
        )
        Box(
            modifier = videoRegionModifier,
        ) {
            if (current != null) {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            setKeepContentOnPlayerReset(true)
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            isFocusable = false
                            isClickable = false
                            importantForAccessibility =
                                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                        }
                    },
                    update = { view -> view.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (
            current != null &&
            !commentsVisible
        ) {
            DanmakuOverlay(
                messages = engagement.danmaku,
                player = player,
                refreshIntervalMs = performanceProfile.danmakuFrameIntervalMs,
                animating = true,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(videoRegionWidth)
                    .height(danmakuHeight),
            )
        }

        if (overlayVisible && !commentsVisible && !sidePanelVisible && !searchVisible) {
            Column(Modifier.fillMaxSize()) {
                TopChrome(
                    selectedTab = MainTab.SHORTS,
                    isLoggedIn = isLoggedIn,
                    onSelectTab = onSelectTab,
                    onAccountClick = onAccountClick,
                    initialFocusRequester = chromeFocusRequester,
                    onUserInteraction = { overlayInteractionSignals.trySend(Unit) },
                    showHeaderActions = !sidePanelVisible,
                    onSearchClick = onSearchClick,
                    downFocusRequester = bottomActionsFocusRequester,
                    showMainNavigation = !state.source.isSidebarPlayback,
                )
                Spacer(Modifier.weight(1f))
            }
        }

        if (
            current != null &&
            overlayVisible &&
            !commentsVisible &&
            !sidePanelVisible &&
            !searchVisible
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 30.dp, end = 30.dp, bottom = 28.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                ShortVideoInfo(
                    video = current,
                    favoriteSelected = current.id in favoriteIds,
                    favoritePending = current.id in favoritePendingIds,
                    topFocusRequester = chromeFocusRequester,
                    focusRequester = bottomActionsFocusRequester,
                    selectedAction = selectedBottomAction,
                    onSelectedActionChange = { selectedBottomAction = it },
                    onCommentsClick = {
                        focusBottomActionsRequested = true
                        onOpenComments(current)
                    },
                    onFavoriteClick = { onToggleFavorite(current) },
                    onAuthorClick = {
                        focusBottomActionsRequested = true
                        onOpenAuthor(current)
                    },
                    modifier = Modifier.width(infoWidth),
                )
            }
        }

        if (current == null && state.loading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = FocusRing)
            }
        }

        state.error?.let { message ->
            ErrorPanel(
                message = message,
                focusRequester = retryFocusRequester,
                actionLabel = if (current == null) "重试" else "刷新视频",
                onAction = onRefresh,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (current != null) {
            PlaybackProgressIndicator(
                player = player,
                videoId = current.id,
                fallbackDurationMs = current.durationMs,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(videoRegionWidth),
            )
        }

        if (current != null && commentsVisible) {
            CommentsPanel(
                videoId = current.id,
                comments = engagement.comments,
                total = engagement.total.coerceAtLeast(current.commentCount),
                loading = engagement.loading,
                loadingMore = engagement.loadingMore,
                error = engagement.error,
                hasMore = engagement.hasMore,
                onClose = onCloseComments,
                onRetry = { onRetryComments(current) },
                onLoadMore = { onLoadMoreComments(current) },
            )
        }

    }
}

@Composable
private fun PlaybackProgressReporter(
    player: Player,
    videoId: String?,
    fallbackDurationMs: Long,
    intervalMs: Long,
    onProgress: (videoId: String, positionMs: Long, durationMs: Long) -> Unit,
) {
    val latestOnProgress = rememberUpdatedState(onProgress)
    LaunchedEffect(player, videoId, fallbackDurationMs, intervalMs) {
        if (videoId == null) return@LaunchedEffect
        var lastPositionMs = -1L
        var lastDurationMs = fallbackDurationMs
        try {
            while (true) {
                delay(intervalMs.coerceAtLeast(1_000L))
                val duration = player.duration.takeIf { it > 0L } ?: fallbackDurationMs
                if (duration > 0L) {
                    val position = player.currentPosition.coerceIn(0L, duration)
                    lastPositionMs = position
                    lastDurationMs = duration
                    latestOnProgress.value(videoId, position, duration)
                }
            }
        } finally {
            if (lastPositionMs >= 0L && lastDurationMs > 0L) {
                latestOnProgress.value(videoId, lastPositionMs, lastDurationMs)
            }
        }
    }
}

@Composable
private fun ShortVideoInfo(
    video: ShortVideo,
    favoriteSelected: Boolean,
    favoritePending: Boolean,
    topFocusRequester: FocusRequester,
    focusRequester: FocusRequester,
    selectedAction: Int,
    onSelectedActionChange: (Int) -> Unit,
    onCommentsClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var actionsFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .glassSurface(
                shape = shape,
                style = GlassStyle(
                    top = Color(0xE01C2230),
                    bottom = Color(0xD2080B12),
                    border = Color.White.copy(alpha = 0.22f),
                    sheen = Color.White.copy(alpha = 0.12f),
                    shadowAlpha = 0.4f,
                ),
                elevation = 14.dp,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = video.description.ifBlank { "抖音短视频" },
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            video.authorAvatarUrl?.let { avatar ->
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = "@${video.authorName.ifBlank { "抖音创作者" }}",
                modifier = Modifier.weight(1f),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "赞 ${formatCount(video.likeCount)} · 评论 ${formatCount(video.commentCount)}",
                color = TextSecondary.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(13.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Spacer(Modifier.height(11.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusProperties {
                    up = topFocusRequester
                    down = FocusRequester.Cancel
                }
                .onFocusChanged { actionsFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionLeft, Key.DirectionRight -> {
                            if (event.type == KeyEventType.KeyDown) {
                                val step = if (event.key == Key.DirectionLeft) -1 else 1
                                onSelectedActionChange(
                                    Math.floorMod(selectedAction + step, 3),
                                )
                            }
                            true
                        }

                        Key.DirectionCenter, Key.Enter -> {
                            if (
                                event.type == KeyEventType.KeyDown &&
                                event.nativeKeyEvent.repeatCount == 0
                            ) {
                                when (selectedAction) {
                                    0 -> onCommentsClick()
                                    1 -> onFavoriteClick()
                                    else -> onAuthorClick()
                                }
                            }
                            true
                        }

                        else -> false
                    }
                }
                .focusable()
                .semantics { contentDescription = "视频操作菜单" },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VideoActionItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = "评论",
                focused = actionsFocused && selectedAction == 0,
            )
            VideoActionItem(
                icon = if (favoriteSelected) Icons.Filled.Star else Icons.Outlined.Star,
                label = when {
                    favoritePending -> "处理中"
                    favoriteSelected -> "已收藏"
                    else -> "收藏"
                },
                selected = favoriteSelected,
                focused = actionsFocused && selectedAction == 1,
            )
            VideoActionItem(
                icon = Icons.Outlined.Home,
                label = "主页",
                focused = actionsFocused && selectedAction == 2,
            )
        }
    }
}

@Composable
private fun VideoActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false,
    focused: Boolean,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    focused -> Brush.linearGradient(
                        listOf(Color(0xFFC51B4C), Color(0xFF72102F)),
                    )
                    selected -> Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.04f),
                        ),
                    )
                    else -> Brush.linearGradient(
                        listOf(Color.Transparent, Color.Transparent),
                    )
                },
            )
            .border(
                1.dp,
                when {
                    focused -> Color.White.copy(alpha = 0.36f)
                    selected -> Color.White.copy(alpha = 0.12f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(50),
            )
            .semantics(mergeDescendants = true) {
                this.contentDescription = label
            }
            .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = when {
                focused -> FocusRing
                selected -> Color(0xFFFFD45C)
                else -> TextPrimary
            },
        )
        Text(
            text = label,
            modifier = Modifier.clearAndSetSemantics { },
            color = if (focused) FocusRing else TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ErrorPanel(
    message: String,
    focusRequester: FocusRequester,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .width(500.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Panel.copy(alpha = 0.97f))
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(22.dp))
        Surface(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusState { focused = it }
                .focusable()
                .then(
                    Modifier.keyClick { onAction() },
                ),
            color = if (focused) FocusRing else Panel,
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (focused) FocusRing else TextSecondary.copy(alpha = 0.35f),
            ),
        ) {
            Text(
                text = actionLabel,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp),
                color = if (focused) Color(0xFF17120B) else TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

private fun Modifier.onFocusState(onChanged: (Boolean) -> Unit): Modifier =
    this.onFocusChanged { onChanged(it.isFocused) }

private fun Modifier.keyClick(onClick: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        if (
            (event.key == Key.DirectionCenter || event.key == Key.Enter) &&
            event.type == KeyEventType.KeyDown
        ) {
            if (event.nativeKeyEvent.repeatCount == 0) onClick()
            true
        } else {
            false
        }
    }
