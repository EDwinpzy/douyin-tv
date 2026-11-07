@file:androidx.media3.common.util.UnstableApi

package com.dongpiandi.douyintv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dongpiandi.douyintv.model.LiveRoom
import com.dongpiandi.douyintv.network.DouyinMediaSourceFactory
import com.dongpiandi.douyintv.ui.LiveListUiState
import com.dongpiandi.douyintv.ui.LivePlayerUiState
import com.dongpiandi.douyintv.ui.MainTab
import com.dongpiandi.douyintv.ui.components.HudHint
import com.dongpiandi.douyintv.ui.components.TopChrome
import com.dongpiandi.douyintv.ui.theme.Accent
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.Ink
import com.dongpiandi.douyintv.ui.theme.Panel
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun LiveGridScreen(
    state: LiveListUiState,
    isLoggedIn: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onAccountClick: () -> Unit,
    onOpenRoom: (LiveRoom) -> Unit,
    onFocusRoom: (String) -> Unit,
    onFocusRestored: () -> Unit,
    onRefresh: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val requesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val retryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(
        state.rooms.size,
        state.loaded,
        state.restoreFocusRoomId,
    ) {
        if (state.rooms.isEmpty()) return@LaunchedEffect
        val targetId = state.restoreFocusRoomId
            ?: state.lastFocusedRoomId
            ?: state.rooms.first().id
        val targetIndex = state.rooms.indexOfFirst { it.id == targetId }.coerceAtLeast(0)
        gridState.scrollToItem(targetIndex)
        delay(180)
        requesters[targetId]?.requestFocus()
        if (state.restoreFocusRoomId != null) onFocusRestored()
    }

    LaunchedEffect(state.error) {
        if (state.error != null && state.rooms.isEmpty()) retryFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF10131B), Ink),
                ),
            ),
    ) {
        TopChrome(
            selectedTab = MainTab.LIVE,
            isLoggedIn = isLoggedIn,
            onSelectTab = onSelectTab,
            onAccountClick = onAccountClick,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "直播广场",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "正在发生的现场，按 OK 进入直播间",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.rooms.isNotEmpty()) {
                HudHint("${state.rooms.size} 个直播间")
            }
        }
        Spacer(Modifier.height(4.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
        ) {
            val columnCount = if (maxWidth >= 800.dp) 4 else 3
            if (state.rooms.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 30.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = state.rooms,
                        key = { _, room -> room.id },
                    ) { _, room ->
                        val requester = remember(room.id) {
                            FocusRequester().also { requesters[room.id] = it }
                        }
                        LiveRoomCard(
                            room = room,
                            focusRequester = requester,
                            onFocus = {
                                onFocusRoom(room.id)
                            },
                            onClick = { onOpenRoom(room) },
                        )
                    }
                }
            }

            if (state.loading && state.rooms.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = FocusRing)
                    Spacer(Modifier.height(16.dp))
                    Text("正在连接直播广场", color = TextSecondary)
                }
            }

            state.error?.let { error ->
                if (state.rooms.isEmpty()) {
                    ErrorPanel(
                        message = error,
                        focusRequester = retryFocusRequester,
                        actionLabel = "重试",
                        onAction = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveRoomCard(
    room: LiveRoom,
    focusRequester: FocusRequester,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(
                BorderStroke(if (focused) 3.dp else 1.dp, if (focused) FocusRing else Color.Transparent),
                RoundedCornerShape(18.dp),
            )
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clickable(onClick = onClick)
            .padding(5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF202635)),
        ) {
            room.coverUrl?.let { cover ->
                AsyncImage(
                    model = cover,
                    contentDescription = room.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.60f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.86f),
                        ),
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(9.dp),
                color = Accent,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "LIVE",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (room.viewerText.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp),
                    color = Color.Black.copy(alpha = 0.56f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "${room.viewerText} 人",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(
                text = room.title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            room.ownerAvatarUrl?.let { avatar ->
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(50)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = room.ownerName.ifBlank { "抖音主播" },
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun LivePlayerScreen(
    state: LivePlayerUiState,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DouyinMediaSourceFactory.create(
                    context = context,
                    referer = "https://live.douyin.com/",
                ),
            )
            .build()
            .apply {
                setHandleAudioBecomingNoisy(true)
            }
    }
    var paused by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    val latestPaused by rememberUpdatedState(paused)
    val latestRetry by rememberUpdatedState(onRetry)
    val streamUrl = state.playback?.streamUrl

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
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

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNullOrBlank()) {
            player.stop()
            player.clearMediaItems()
            return@LaunchedEffect
        }
        paused = false
        buffering = true
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(streamUrl) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.error) {
        if (state.error != null) retryFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                val handled = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.MediaPlayPause ||
                    event.key == Key.MediaPlay ||
                    event.key == Key.MediaPause
                if (!handled) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                if (event.nativeKeyEvent.repeatCount == 0 && state.error == null) {
                    val nextPaused = !paused
                    paused = nextPaused
                    player.playWhenReady = !nextPaused
                    if (nextPaused) player.pause() else player.play()
                }
                true
            }
            .focusable(),
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isFocusable = false
                    isClickable = false
                }
            },
            update = { view -> view.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.56f),
                        0.18f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = Accent, shape = RoundedCornerShape(9.dp)) {
                Text(
                    text = "直播中",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.widthIn(max = 280.dp)) {
                Text(
                    text = state.playback?.title ?: state.room.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@${state.playback?.ownerName ?: state.room.ownerName}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(30.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HudHint(if (paused) "已暂停 · OK 继续" else "OK 暂停")
            HudHint("返回键离开")
        }

        if (buffering && state.error == null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                color = FocusRing,
            )
        }

        state.error?.let { error ->
            ErrorPanel(
                message = error,
                focusRequester = retryFocusRequester,
                actionLabel = "重试",
                onAction = latestRetry,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
