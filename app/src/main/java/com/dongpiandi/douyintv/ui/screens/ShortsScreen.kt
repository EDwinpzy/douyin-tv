@file:androidx.media3.common.util.UnstableApi

package com.dongpiandi.douyintv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dongpiandi.douyintv.model.ShortVideo
import com.dongpiandi.douyintv.network.DouyinMediaSourceFactory
import com.dongpiandi.douyintv.ui.MainTab
import com.dongpiandi.douyintv.ui.ShortsUiState
import com.dongpiandi.douyintv.ui.components.HudHint
import com.dongpiandi.douyintv.ui.components.TopChrome
import com.dongpiandi.douyintv.ui.formatCount
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.Panel
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private const val OVERLAY_AUTO_HIDE_DELAY_MS = 4_000L

@Composable
fun ShortsScreen(
    state: ShortsUiState,
    overlayVisible: Boolean,
    isLoggedIn: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onAccountClick: () -> Unit,
    onShowOverlay: () -> Unit,
    onHideOverlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePause: () -> Unit,
    onEnded: () -> Unit,
    onPlaybackError: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoFocusRequester = remember { FocusRequester() }
    val chromeFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    var focusChromeRequested by remember { mutableStateOf(false) }
    var videoFocused by remember { mutableStateOf(false) }
    var autoHideEpoch by remember { mutableIntStateOf(0) }
    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DouyinMediaSourceFactory.create(
                    context = context,
                    referer = "https://www.douyin.com/",
                ),
            )
            .build()
            .apply {
                setHandleAudioBecomingNoisy(true)
            }
    }
    val current = state.current
    var urlIndex by remember(current?.id) { mutableIntStateOf(0) }
    var buffering by remember { mutableStateOf(true) }
    val latestVideo by rememberUpdatedState(current)
    val latestUrlIndex by rememberUpdatedState(urlIndex)
    val latestPaused by rememberUpdatedState(state.paused)
    val latestOnEnded by rememberUpdatedState(onEnded)
    val latestOnError by rememberUpdatedState(onPlaybackError)
    val activeUrl = current?.playUrls?.getOrNull(urlIndex)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) {
                    latestOnEnded()
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
            player.release()
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

    LaunchedEffect(activeUrl) {
        if (activeUrl.isNullOrBlank()) {
            player.stop()
            player.clearMediaItems()
            return@LaunchedEffect
        }
        buffering = true
        player.setMediaItem(MediaItem.fromUri(activeUrl))
        player.prepare()
        player.playWhenReady = !state.paused
    }

    LaunchedEffect(state.paused) {
        player.playWhenReady = !state.paused
        if (state.paused) player.pause() else player.play()
    }

    LaunchedEffect(current?.id) {
        if (current != null) {
            videoFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(
        overlayVisible,
        current?.id,
        videoFocused,
        state.paused,
        autoHideEpoch,
    ) {
        if (!overlayVisible || current == null || !videoFocused || state.paused) {
            return@LaunchedEffect
        }
        delay(OVERLAY_AUTO_HIDE_DELAY_MS)
        onHideOverlay()
    }

    LaunchedEffect(overlayVisible, focusChromeRequested) {
        if (overlayVisible && focusChromeRequested) {
            chromeFocusRequester.requestFocus()
            focusChromeRequested = false
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) retryFocusRequester.requestFocus()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val infoWidth = if (maxWidth < 700.dp) {
            maxWidth * 0.50f
        } else {
            (maxWidth * 0.55f).coerceAtMost(440.dp)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(videoFocusRequester)
                .onFocusChanged { videoFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (state.error != null) return@onPreviewKeyEvent false
                    val handled = event.key == Key.DirectionUp ||
                        event.key == Key.DirectionDown ||
                        event.key == Key.DirectionLeft ||
                        event.key == Key.DirectionRight ||
                        event.key == Key.DirectionCenter ||
                        event.key == Key.Enter ||
                        event.key == Key.Menu ||
                        event.key == Key.MediaPlayPause ||
                        event.key == Key.MediaPlay ||
                        event.key == Key.MediaPause
                    if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent handled
                    if (!handled) return@onPreviewKeyEvent false
                    if (event.nativeKeyEvent.repeatCount > 0) return@onPreviewKeyEvent true

                    onShowOverlay()
                    autoHideEpoch++
                    when (event.key) {
                        Key.DirectionUp -> {
                            if (state.index > 0) {
                                onPrevious()
                            } else {
                                focusChromeRequested = true
                            }
                        }

                        Key.DirectionDown -> onNext()
                        Key.DirectionLeft -> {
                            player.seekTo((player.currentPosition - 5_000).coerceAtLeast(0))
                        }

                        Key.DirectionRight -> {
                            player.seekTo(player.currentPosition + 5_000)
                        }

                        Key.DirectionCenter, Key.Enter, Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause ->
                            onTogglePause()

                        Key.Menu -> Unit
                        else -> Unit
                    }
                    true
                }
                .focusable(),
        )
        current?.coverUrl?.let { cover ->
            AsyncImage(
                model = cover,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.24f,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    ),
                ),
        )

        if (current != null) {
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
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (overlayVisible) {
            Column(Modifier.fillMaxSize()) {
                TopChrome(
                    selectedTab = MainTab.SHORTS,
                    isLoggedIn = isLoggedIn,
                    onSelectTab = onSelectTab,
                    onAccountClick = onAccountClick,
                    initialFocusRequester = chromeFocusRequester,
                )
                Spacer(Modifier.weight(1f))
            }
        }

        if (current != null && overlayVisible) {
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
                    modifier = Modifier.width(infoWidth),
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudHint("↑↓ 切换")
                    HudHint("OK 暂停")
                    HudHint("←→ 快进")
                }
            }
        }

        if (buffering && current != null && state.error == null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp),
                color = FocusRing,
                strokeWidth = 4.dp,
            )
        }

        if (current == null && state.loading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = FocusRing)
                Spacer(Modifier.height(18.dp))
                Text("正在加载推荐视频", color = TextSecondary)
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
    }
}

@Composable
private fun ShortVideoInfo(
    video: ShortVideo,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF080A0F).copy(alpha = 0.96f), Color.Black.copy(alpha = 0.86f)),
                ),
            )
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
