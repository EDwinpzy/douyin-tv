@file:androidx.media3.common.util.UnstableApi
@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dongpiandi.douyintv.ui.screens

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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.Panel
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

private const val OVERLAY_AUTO_HIDE_DELAY_MS = 4_000L
private const val DANMAKU_HEIGHT_FRACTION = 0.15f

@Composable
fun ShortsScreen(
    state: ShortsUiState,
    engagement: EngagementUiState,
    overlayVisible: Boolean,
    sidePanelVisible: Boolean,
    isLoggedIn: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onAccountClick: () -> Unit,
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
    val focusManager = LocalFocusManager.current
    val videoFocusRequester = remember { FocusRequester() }
    val chromeFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    var focusChromeRequested by remember { mutableStateOf(false) }
    val autoHideSignal = remember { MutableStateFlow(0L) }
    var videoFocused by remember { mutableStateOf(false) }
    var topChromeFocused by remember { mutableStateOf(false) }
    var bottomChromeFocused by remember { mutableStateOf(false) }
    val chromeFocused = topChromeFocused || bottomChromeFocused
    val performanceProfile = remember(context) {
        DevicePerformanceProfile.detect(context)
    }
    val playbackEngine = remember(context, performanceProfile) {
        ShortsPlaybackEngine(
            context = context,
            preloadForwardCount = performanceProfile.preloadForwardCount,
        )
    }
    val player = playbackEngine.player
    val current = state.current
    var urlIndex by remember(current?.id) { mutableIntStateOf(0) }
    var readyVideoId by remember { mutableStateOf<String?>(null) }
    var renderedVideoId by remember { mutableStateOf<String?>(null) }
    val latestVideo by rememberUpdatedState(current)
    val latestUrlIndex by rememberUpdatedState(urlIndex)
    val latestPaused by rememberUpdatedState(state.paused)
    val latestOnEnded by rememberUpdatedState(onEnded)
    val latestOnError by rememberUpdatedState(onPlaybackError)
    val latestOnProgress by rememberUpdatedState(onPlaybackProgress)
    val activeUrl = current?.playUrls?.getOrNull(urlIndex)
    val commentsVisible = engagement.visible

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

            override fun onRenderedFirstFrame() {
                renderedVideoId = latestVideo?.id
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

    LaunchedEffect(current?.id, activeUrl) {
        if (activeUrl.isNullOrBlank()) {
            player.stop()
            player.clearMediaItems()
            return@LaunchedEffect
        }
        val video = current ?: return@LaunchedEffect
        readyVideoId = null
        renderedVideoId = null
        playbackEngine.prepare(video, urlIndex)
        player.prepare()
        player.seekTo(0L)
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
        if (current != null) {
            onPrepareEngagement(current)
            if (!engagement.visible) {
                videoFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(commentsVisible, sidePanelVisible) {
        if (!commentsVisible && !sidePanelVisible) {
            withFrameNanos { }
            focusManager.clearFocus(force = true)
            videoFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(overlayVisible, current?.id) {
        if (!overlayVisible && current != null && !commentsVisible) {
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
        chromeFocused,
    ) {
        if (
            !overlayVisible ||
            current == null ||
            state.paused ||
            commentsVisible ||
            sidePanelVisible ||
            chromeFocused
        ) {
            return@LaunchedEffect
        }
        autoHideSignal.collectLatest {
            delay(OVERLAY_AUTO_HIDE_DELAY_MS)
            focusManager.clearFocus(force = true)
            onHideOverlay()
            withFrameNanos { }
            videoFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(
        overlayVisible,
        focusChromeRequested,
        commentsVisible,
    ) {
        if (
            overlayVisible &&
            focusChromeRequested &&
            !commentsVisible
        ) {
            withFrameNanos { }
            delay(16)
            chromeFocusRequester.requestFocus()
            focusChromeRequested = false
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) retryFocusRequester.requestFocus()
    }

    val videoFrameVisible = current != null && renderedVideoId == current.id

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                val seekKey = event.key == Key.DirectionLeft ||
                    event.key == Key.DirectionRight
                if (
                    !seekKey ||
                    !videoFocused ||
                    overlayVisible ||
                    commentsVisible ||
                    current == null ||
                    state.error != null
                ) {
                    return@onPreviewKeyEvent false
                }
                if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                if (event.nativeKeyEvent.repeatCount == 0) {
                    val delta = if (event.key == Key.DirectionLeft) -5_000L else 5_000L
                    val maxPosition = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                    player.seekTo(
                        (player.currentPosition + delta).coerceIn(0L, maxPosition),
                    )
                }
                true
            },
    ) {
        val infoWidth = (maxWidth * 0.28f).coerceAtMost(280.dp)
        val danmakuHeight = (maxHeight * DANMAKU_HEIGHT_FRACTION)
            .coerceAtLeast(72.dp)
        val commentsWidth = commentsPanelWidth(maxWidth)
        val videoRegionWidth = if (commentsVisible) {
            (maxWidth - commentsWidth).coerceAtLeast(1.dp)
        } else {
            maxWidth
        }
        val videoRegionModifier = if (commentsVisible) {
            Modifier
                .fillMaxHeight()
                .width(videoRegionWidth)
                .align(Alignment.CenterStart)
        } else {
            Modifier.fillMaxSize()
        }
        val aspectRatio = current?.aspectRatio
        val fittedByWidthHeight = aspectRatio
            ?.takeIf { it > 0f }
            ?.let { videoRegionWidth / it }
        val videoFrameModifier = if (
            aspectRatio != null &&
            fittedByWidthHeight != null
        ) {
            if (fittedByWidthHeight <= maxHeight) {
                Modifier
                    .width(videoRegionWidth)
                    .height(fittedByWidthHeight)
            } else {
                Modifier
                    .width(maxHeight * aspectRatio)
                    .height(maxHeight)
            }
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
                                autoHideSignal.value += 1
                                focusChromeRequested = true
                            }
                        }

                        Key.DirectionDown -> {
                            onHideOverlay()
                            onNext()
                        }
                        Key.DirectionCenter, Key.Enter, Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                            onShowOverlay()
                            autoHideSignal.value += 1
                            onTogglePause()
                            focusChromeRequested = true
                        }

                        Key.Menu -> {
                            onShowOverlay()
                            autoHideSignal.value += 1
                            focusChromeRequested = true
                        }
                        else -> Unit
                    }
                    true
                }
                .focusable(enabled = !commentsVisible && !sidePanelVisible),
        )
        Box(
            modifier = videoRegionModifier,
        ) {
            if (current != null) {
                Box(
                    modifier = videoFrameModifier.align(Alignment.Center),
                ) {
                    AndroidView(
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                useController = false
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                isFocusable = false
                                isClickable = false
                            }
                        },
                        update = { view -> view.player = player },
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (videoFrameVisible) 1f else 0f),
                    )
                }
            }
        }

        if (current != null && !commentsVisible && !sidePanelVisible) {
            DanmakuOverlay(
                messages = engagement.danmaku,
                player = player,
                refreshIntervalMs = performanceProfile.danmakuFrameIntervalMs,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .height(danmakuHeight),
            )
        }

        if (overlayVisible && !commentsVisible) {
            Column(Modifier.fillMaxSize()) {
                TopChrome(
                    selectedTab = MainTab.SHORTS,
                    isLoggedIn = isLoggedIn,
                    onSelectTab = onSelectTab,
                    onAccountClick = onAccountClick,
                    initialFocusRequester = chromeFocusRequester,
                    onHistoryClick = onHistoryClick,
                    onUserInteraction = { autoHideSignal.value += 1 },
                    onMenuFocusChanged = { topChromeFocused = it },
                    showHeaderActions = !sidePanelVisible,
                )
                Spacer(Modifier.weight(1f))
            }
        }

        if (
            current != null &&
            overlayVisible &&
            !commentsVisible
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f)),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 30.dp, end = 30.dp, bottom = 28.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                ShortVideoInfo(
                    video = current,
                    paused = state.paused,
                    favoriteSelected = current.id in favoriteIds,
                    favoritePending = current.id in favoritePendingIds,
                    topFocusRequester = chromeFocusRequester,
                    onCommentsClick = { onOpenComments(current) },
                    onFavoriteClick = { onToggleFavorite(current) },
                    onAuthorClick = { onOpenAuthor(current) },
                    onMenuFocusChanged = { bottomChromeFocused = it },
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
            val videoWidth = if (commentsVisible) {
                (maxWidth - commentsWidth).coerceAtLeast(1.dp)
            } else {
                maxWidth
            }
            PlaybackProgressIndicator(
                player = player,
                videoId = current.id,
                fallbackDurationMs = current.durationMs,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(videoWidth),
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
    paused: Boolean,
    favoriteSelected: Boolean,
    favoritePending: Boolean,
    topFocusRequester: FocusRequester,
    onCommentsClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onMenuFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val commentsFocusRequester = remember { FocusRequester() }
    val favoriteFocusRequester = remember { FocusRequester() }
    val authorFocusRequester = remember { FocusRequester() }
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
            .onFocusChanged { onMenuFocusChanged(it.hasFocus) }
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (paused) {
            Surface(
                color = FocusRing.copy(alpha = 0.95f),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = "已暂停",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    color = Color(0xFF17120B),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VideoActionButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = "评论",
                focusRequester = commentsFocusRequester,
                previousFocusRequester = null,
                nextFocusRequester = favoriteFocusRequester,
                upFocusRequester = topFocusRequester,
                onClick = onCommentsClick,
            )
            VideoActionButton(
                icon = if (favoriteSelected) Icons.Filled.Star else Icons.Outlined.Star,
                label = when {
                    favoritePending -> "处理中"
                    favoriteSelected -> "已收藏"
                    else -> "收藏"
                },
                selected = favoriteSelected,
                focusRequester = favoriteFocusRequester,
                previousFocusRequester = commentsFocusRequester,
                nextFocusRequester = authorFocusRequester,
                upFocusRequester = topFocusRequester,
                onClick = onFavoriteClick,
            )
            VideoActionButton(
                icon = Icons.Outlined.Home,
                label = "主页",
                focusRequester = authorFocusRequester,
                previousFocusRequester = favoriteFocusRequester,
                nextFocusRequester = null,
                upFocusRequester = topFocusRequester,
                onClick = onAuthorClick,
            )
        }
    }
}

@Composable
private fun VideoActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    focusRequester: FocusRequester,
    previousFocusRequester: FocusRequester?,
    nextFocusRequester: FocusRequester?,
    upFocusRequester: FocusRequester,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    focused -> Color.White.copy(alpha = 0.14f)
                    selected -> Color.White.copy(alpha = 0.07f)
                    else -> Color.Transparent
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
            .focusRequester(focusRequester)
            .focusProperties {
                left = previousFocusRequester ?: FocusRequester.Cancel
                right = nextFocusRequester ?: FocusRequester.Cancel
                up = upFocusRequester
                down = FocusRequester.Cancel
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
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
