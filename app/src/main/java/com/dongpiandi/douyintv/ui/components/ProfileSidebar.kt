@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dongpiandi.douyintv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dongpiandi.douyintv.model.PlaybackHistoryEntry
import com.dongpiandi.douyintv.model.PlaybackProgress
import com.dongpiandi.douyintv.model.ShortVideo
import com.dongpiandi.douyintv.model.UserProfile
import com.dongpiandi.douyintv.performance.LocalDevicePerformanceProfile
import com.dongpiandi.douyintv.ui.MyHomeTab
import com.dongpiandi.douyintv.ui.MyProfileUiState
import com.dongpiandi.douyintv.ui.formatCount
import com.dongpiandi.douyintv.ui.formatDuration
import com.dongpiandi.douyintv.ui.theme.Accent
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun MyHomeSidebar(
    selectedTab: MyHomeTab,
    profile: MyProfileUiState,
    history: List<PlaybackHistoryEntry>,
    favorites: List<ShortVideo>,
    favoriteProgress: Map<String, PlaybackProgress>,
    favoritesLoading: Boolean,
    favoritesLoadingMore: Boolean,
    favoritesError: String?,
    favoritesHasMore: Boolean,
    onTabSelected: (MyHomeTab) -> Unit,
    onClose: () -> Unit,
    onRetryProfile: () -> Unit,
    onRetryFavorites: () -> Unit,
    onLoadMoreFavorites: () -> Unit,
    onPlayHistory: (ShortVideo) -> Unit,
    onPlayFavorite: (ShortVideo) -> Unit,
    onLogout: () -> Unit,
) {
    val tabs = MyHomeTab.entries
    val tabRequesters = remember {
        tabs.associateWith { FocusRequester() }
    }
    val historyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        tabRequesters[selectedTab]?.requestFocus()
    }

    SidebarFrame(title = "我的主页", onClose = onClose) {
        val currentProfile = profile.profile
        ProfileIdentity(
            name = currentProfile?.nickname ?: "我的抖音",
            uniqueId = currentProfile?.uniqueId.orEmpty(),
            avatarUrl = currentProfile?.avatarUrl,
            signature = currentProfile?.signature
                .orEmpty()
                .ifBlank { "抖音 TV 登录账号" },
            followerCount = currentProfile?.followerCount,
            followingCount = currentProfile?.followingCount,
            totalFavorited = currentProfile?.totalFavorited,
            verified = true,
        )
        Spacer(Modifier.height(8.dp))
        SidebarTabs(
            labels = listOf("观看历史", "我的收藏", "我的主页"),
            selectedIndex = tabs.indexOf(selectedTab),
            requesters = tabs.map { tabRequesters.getValue(it) },
            onSelected = { index -> onTabSelected(tabs[index]) },
        )
        Spacer(Modifier.height(7.dp))
        when (selectedTab) {
            MyHomeTab.HISTORY -> SidebarVideoList(
                videos = history.map(PlaybackHistoryEntry::video),
                progressByVideoId = history.associate {
                    it.video.id to it.progress
                },
                emptyText = "还没有观看记录",
                firstFocusRequester = historyFocusRequester,
                onPlay = onPlayHistory,
            )

            MyHomeTab.FAVORITES -> SidebarVideoList(
                videos = favorites,
                progressByVideoId = favoriteProgress,
                loading = favoritesLoading,
                loadingMore = favoritesLoadingMore,
                error = favoritesError,
                hasMore = favoritesHasMore,
                emptyText = "收藏夹还是空的",
                onRetry = onRetryFavorites,
                onLoadMore = onLoadMoreFavorites,
                onPlay = onPlayFavorite,
            )

            MyHomeTab.PROFILE -> MyProfileContent(
                profile = profile,
                historyCount = history.size,
                favoritesCount = favorites.size,
                onRetry = onRetryProfile,
                onLogout = onLogout,
            )
        }
    }
}

@Composable
fun AuthorProfileSidebar(
    profile: UserProfile?,
    fallbackName: String,
    fallbackAvatarUrl: String?,
    profileLoading: Boolean,
    profileError: String?,
    videos: List<ShortVideo>,
    progressByVideoId: Map<String, PlaybackProgress>,
    loading: Boolean,
    loadingMore: Boolean,
    error: String?,
    hasMore: Boolean,
    onClose: () -> Unit,
    onRetryProfile: () -> Unit,
    onRetryVideos: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (ShortVideo) -> Unit,
) {
    var selectedTab by remember(profile?.nickname ?: fallbackName) {
        mutableIntStateOf(0)
    }
    val tabRequesters = remember { listOf(FocusRequester(), FocusRequester(), FocusRequester()) }
    val firstVideoFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        tabRequesters.first().requestFocus()
    }

    SidebarFrame(
        title = "@${profile?.nickname ?: fallbackName.ifBlank { "博主" }}的主页",
        onClose = onClose,
    ) {
        ProfileIdentity(
            name = profile?.nickname ?: fallbackName.ifBlank { "抖音创作者" },
            uniqueId = profile?.uniqueId.orEmpty(),
            avatarUrl = profile?.avatarUrl ?: fallbackAvatarUrl,
            signature = profile?.signature
                .orEmpty()
                .ifBlank { if (profileLoading) "正在加载主页资料" else "抖音视频创作者" },
            followerCount = profile?.followerCount,
            followingCount = profile?.followingCount,
            totalFavorited = profile?.totalFavorited,
            verified = true,
        )
        if (profileError != null && profile == null) {
            Text(
                text = profileError,
                modifier = Modifier.padding(top = 10.dp),
                color = Accent,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
        SidebarTabs(
            labels = listOf("视频 ${videos.size}", "合集 0", "喜欢 0"),
            selectedIndex = selectedTab,
            requesters = tabRequesters,
            onSelected = { selectedTab = it },
        )
        Spacer(Modifier.height(7.dp))
        when (selectedTab) {
            0 -> SidebarVideoList(
                videos = videos,
                progressByVideoId = progressByVideoId,
                loading = loading,
                loadingMore = loadingMore,
                error = error,
                hasMore = hasMore,
                emptyText = "博主还没有公开视频",
                firstFocusRequester = firstVideoFocusRequester,
                onRetry = onRetryVideos,
                onLoadMore = onLoadMore,
                onPlay = onPlay,
            )

            1 -> SidebarEmptyState("暂无公开合集")
            else -> SidebarEmptyState("作者喜欢的视频暂未公开")
        }
    }
}

@Composable
private fun SidebarFrame(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val closeFocusRequester = remember { FocusRequester() }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val panelWidth = maxWidth * (1f / 3f)
        val panelShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(panelWidth)
                .glassSurface(
                    shape = panelShape,
                    style = GlassStyle(
                        top = Color(0xE61B2233),
                        bottom = Color(0xCC0A0D14),
                        border = Color.White.copy(alpha = 0.22f),
                        sheen = Color.White.copy(alpha = 0.14f),
                        shadowAlpha = 0.52f,
                    ),
                    elevation = 18.dp,
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    RoundIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        focusRequester = closeFocusRequester,
                        onClick = onClose,
                    )
                }
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun ProfileIdentity(
    name: String,
    uniqueId: String,
    avatarUrl: String?,
    signature: String,
    followerCount: Long?,
    followingCount: Long?,
    totalFavorited: Long?,
    verified: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(avatarUrl = avatarUrl, size = 42)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (verified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFFFFC94A),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append("粉丝 ${followerCount?.let(::formatCount) ?: "—"}")
                    append("  ·  获赞 ${totalFavorited?.let(::formatCount) ?: "—"}")
                    if (!uniqueId.isNullOrBlank()) append("  ·  @$uniqueId")
                },
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(signature)
                    if (followingCount != null) {
                        if (isNotBlank()) append("  ·  ")
                        append("关注 ${formatCount(followingCount)}")
                    }
                },
                color = TextSecondary.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 12.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Avatar(avatarUrl: String?, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF7D8CFF), Color(0xFFFF5D9A)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size((size * 0.58f).dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun SidebarTabs(
    labels: List<String>,
    selectedIndex: Int,
    requesters: List<FocusRequester>,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            SidebarTab(
                label = label,
                selected = selectedIndex == index,
                focusRequester = requesters.getOrNull(index),
                previousFocusRequester = requesters.getOrNull(index - 1),
                nextFocusRequester = requesters.getOrNull(index + 1),
                onClick = { onSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SidebarTab(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    previousFocusRequester: FocusRequester?,
    nextFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val lowTier = LocalDevicePerformanceProfile.current.lowTier
    val foreground by animateColorAsState(
        when {
            selected -> Color.White
            focused -> FocusRing
            else -> TextSecondary
        },
        animationSpec = if (lowTier) snap() else tween(durationMillis = 140),
        label = "sidebarTabForeground",
    )
    Column(
        modifier = modifier
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            )
            .focusProperties {
                left = previousFocusRequester ?: FocusRequester.Cancel
                right = nextFocusRequester ?: FocusRequester.Cancel
            }
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (focused && !selected) {
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.095f),
                        1f to Color.White.copy(alpha = 0.035f),
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent),
                    )
                },
            )
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
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp,
            ),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(if (selected || focused) 0.64f else 0f)
                .height(2.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) Accent else FocusRing),
        )
    }
}

@Composable
private fun SidebarVideoList(
    videos: List<ShortVideo>,
    progressByVideoId: Map<String, PlaybackProgress>,
    loading: Boolean = false,
    loadingMore: Boolean = false,
    error: String? = null,
    hasMore: Boolean = false,
    emptyText: String,
    firstFocusRequester: FocusRequester? = null,
    onRetry: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onPlay: (ShortVideo) -> Unit,
) {
    when {
        loading && videos.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Accent,
                    strokeWidth = 2.dp,
                )
            }
        }

        error != null && videos.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = error,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                SidebarPillButton(
                    label = "重试",
                    focusRequester = firstFocusRequester,
                    onClick = onRetry,
                )
            }
        }

        videos.isEmpty() -> SidebarEmptyState(emptyText)

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                items(
                    items = videos,
                    key = ShortVideo::id,
                ) { video ->
                    val requester = remember(video.id) { FocusRequester() }
                    SidebarVideoRow(
                        video = video,
                        progress = progressByVideoId[video.id],
                        focusRequester = if (video.id == videos.first().id) {
                            firstFocusRequester ?: requester
                        } else {
                            requester
                        },
                        onPlay = { onPlay(video) },
                    )
                }
                if (error != null) {
                    item(key = "sidebar-error") {
                        Text(
                            text = error,
                            modifier = Modifier.padding(vertical = 5.dp),
                            color = Accent,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 13.sp,
                            ),
                        )
                    }
                }
                if (hasMore) {
                    item(key = "sidebar-load-more") {
                        LaunchedEffect(videos.size, hasMore) {
                            if (!loadingMore) onLoadMore()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (loadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Accent,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarVideoRow(
    video: ShortVideo,
    progress: PlaybackProgress?,
    focusRequester: FocusRequester,
    onPlay: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val lowTier = LocalDevicePerformanceProfile.current.lowTier
    val scale by animateFloatAsState(
        targetValue = if (focused && !lowTier) 1.012f else 1f,
        animationSpec = if (lowTier) snap() else spring(),
        label = "sidebarVideoScale",
    )
    val rowShape = RoundedCornerShape(10.dp)
    val motionLayer = if (lowTier) {
        Modifier
    } else {
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(motionLayer)
            .glassSurface(
                shape = rowShape,
                style = GlassStyle(
                    top = if (focused) Color(0xE6343D52) else Color(0x3D1B2130),
                    bottom = if (focused) Color(0xD1121825) else Color(0x2E10151F),
                    border = if (focused) {
                        FocusRing.copy(alpha = 0.92f)
                    } else {
                        Color.White.copy(alpha = 0.09f)
                    },
                    sheen = Color.White.copy(alpha = if (focused) 0.13f else 0.06f),
                    shadowAlpha = if (focused) 0.3f else 0.14f,
                ),
                elevation = if (focused) 8.dp else 2.dp,
            )
            .focusRequester(focusRequester)
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (
                    (event.key == Key.DirectionCenter || event.key == Key.Enter) &&
                    event.type == KeyEventType.KeyDown
                ) {
                    if (event.nativeKeyEvent.repeatCount == 0) onPlay()
                    true
                } else {
                    false
                }
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(41.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF11151E)),
            ) {
                video.coverUrl?.let { coverUrl ->
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)),
                            ),
                        ),
                )
                Text(
                    text = formatDuration(progress?.durationMs ?: video.durationMs),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp),
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                    ),
                )
            }
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = video.description.ifBlank { "抖音短视频" },
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp,
                    ),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = TextSecondary,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = buildMetadata(video, progress),
                        modifier = Modifier.weight(1f),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 9.5.sp,
                            lineHeight = 11.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = TextSecondary.copy(alpha = 0.72f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                VideoProgressBar(
                    progress = progress?.fraction ?: 0f,
                    modifier = Modifier.fillMaxWidth(),
                    height = 2.dp,
                )
            }
        }
    }
}

@Composable
private fun MyProfileContent(
    profile: MyProfileUiState,
    historyCount: Int,
    favoritesCount: Int,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    val retryFocusRequester = remember { FocusRequester() }
    val logoutFocusRequester = remember { FocusRequester() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "profile-stats") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(
                        shape = RoundedCornerShape(14.dp),
                        style = GlassStyle(
                            top = Color(0x402B3447),
                            bottom = Color(0x2E10151F),
                            border = Color.White.copy(alpha = 0.11f),
                            sheen = Color.White.copy(alpha = 0.07f),
                            shadowAlpha = 0.22f,
                        ),
                        elevation = 4.dp,
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ProfileMetric("$historyCount", "观看")
                    ProfileMetric("$favoritesCount", "收藏")
                    ProfileMetric(
                        if (profile.profile != null) "已登录" else "—",
                        "账号",
                    )
                }
            }
        }
        if (profile.loading) {
            item(key = "profile-loading") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Accent,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        if (profile.error != null) {
            item(key = "profile-error") {
                Column {
                    Text(
                        text = profile.error,
                        color = Accent,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        ),
                    )
                    Spacer(Modifier.height(6.dp))
                    SidebarPillButton(
                        label = "重新加载",
                        focusRequester = retryFocusRequester,
                        onClick = onRetry,
                    )
                }
            }
        }
        item(key = "profile-note") {
            Text(
                text = "观看历史与播放进度仅保存在本机。登录凭证继续使用 Android Keystore 加密保存。",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                ),
            )
        }
        item(key = "profile-logout") {
            Spacer(Modifier.height(2.dp))
            SidebarPillButton(
                label = "退出登录",
                focusRequester = logoutFocusRequester,
                primary = true,
                onClick = onLogout,
            )
        }
    }
}

@Composable
private fun ProfileMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 14.sp,
                lineHeight = 16.sp,
            ),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 10.sp,
                lineHeight = 12.sp,
            ),
        )
    }
}

@Composable
private fun SidebarEmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
            ),
        )
    }
}

@Composable
private fun SidebarPillButton(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    primary: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            )
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
            },
        color = when {
            focused -> FocusRing
            primary -> Accent
            else -> Color.White.copy(alpha = 0.09f)
        },
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.dp,
            if (focused) FocusRing else Color.White.copy(alpha = 0.1f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = if (focused) Color(0xFF17120B) else TextPrimary,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
            ),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (focused) Color.White.copy(alpha = 0.18f)
                else Color.Transparent,
            )
            .focusRequester(focusRequester)
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
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = if (focused) FocusRing else TextPrimary,
        )
    }
}

private fun buildMetadata(video: ShortVideo, progress: PlaybackProgress?): String {
    val like = "赞 ${formatCount(video.likeCount)}"
    val relative = relativeTime(progress?.updatedAtMs ?: 0L)
    return if (relative.isNotBlank()) "$like · $relative" else like
}

private fun relativeTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return ""
    val delta = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    return when {
        delta < 60_000L -> "刚刚"
        delta < 3_600_000L -> "${delta / 60_000L}分钟前"
        delta < 86_400_000L -> "${delta / 3_600_000L}小时前"
        delta < 30L * 86_400_000L -> "${delta / 86_400_000L}天前"
        else -> "${delta / (30L * 86_400_000L)}个月前"
    }
}
