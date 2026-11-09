@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dongpiandi.douyintv.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
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
import com.dongpiandi.douyintv.model.VideoCollection
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
    onRetryMyVideos: () -> Unit,
    onLoadMoreMyVideos: () -> Unit,
    onRetryFavorites: () -> Unit,
    onLoadMoreFavorites: () -> Unit,
    onPlayHistory: (ShortVideo) -> Unit,
    onPlayFavorite: (ShortVideo) -> Unit,
    onPlayMyVideo: (ShortVideo) -> Unit,
    onLogout: () -> Unit,
) {
    val tabs = MyHomeTab.entries
    val tabRequesters = remember {
        tabs.associateWith { FocusRequester() }
    }
    val historyFocusRequester = remember { FocusRequester() }
    val favoritesFocusRequester = remember { FocusRequester() }
    val profileFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = when (selectedTab) {
        MyHomeTab.HISTORY -> historyFocusRequester.takeIf {
            history.isNotEmpty()
        }

        MyHomeTab.FAVORITES -> favoritesFocusRequester.takeIf {
            favorites.isNotEmpty() || (!favoritesLoading && favoritesError != null)
        }

        MyHomeTab.PROFILE -> profileFocusRequester.takeIf {
            profile.videos.isNotEmpty() ||
                (
                    !profile.loading &&
                        !profile.videosLoading &&
                        (profile.videosError ?: profile.error) != null
                    )
        }
    }

    LaunchedEffect(selectedTab) {
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
            onLogout = currentProfile?.let { onLogout },
        )
        Spacer(Modifier.height(8.dp))
        SidebarTabs(
            labels = listOf("观看历史", "我的收藏", "我的主页"),
            selectedIndex = tabs.indexOf(selectedTab),
            requesters = tabs.map { tabRequesters.getValue(it) },
            downFocusRequester = contentFocusRequester,
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
                upFocusRequester = tabRequesters.getValue(MyHomeTab.HISTORY),
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
                firstFocusRequester = favoritesFocusRequester,
                upFocusRequester = tabRequesters.getValue(MyHomeTab.FAVORITES),
                onRetry = onRetryFavorites,
                onLoadMore = onLoadMoreFavorites,
                onPlay = onPlayFavorite,
            )

            MyHomeTab.PROFILE -> MyProfileContent(
                profile = profile,
                firstFocusRequester = profileFocusRequester,
                upFocusRequester = tabRequesters.getValue(MyHomeTab.PROFILE),
                onRetry = onRetryMyVideos,
                onLoadMore = onLoadMoreMyVideos,
                onPlay = onPlayMyVideo,
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
    collections: List<VideoCollection>,
    collectionsLoaded: Boolean,
    collectionsLoading: Boolean,
    collectionsLoadingMore: Boolean,
    collectionsError: String?,
    collectionsHasMore: Boolean,
    openingCollectionId: String?,
    onClose: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onRetryProfile: () -> Unit,
    onRetryVideos: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (ShortVideo) -> Unit,
    onRetryCollections: () -> Unit,
    onLoadMoreCollections: () -> Unit,
    onPlayCollection: (VideoCollection) -> Unit,
) {
    var selectedTab by remember(profile?.nickname ?: fallbackName) {
        mutableIntStateOf(0)
    }
    val showCollectionsTab = shouldShowAuthorCollectionsTab(
        collectionsLoaded = collectionsLoaded,
        collectionCount = collections.size,
    )
    val activeTab = if (showCollectionsTab && selectedTab == 1) 1 else 0
    val tabRequesters = remember { listOf(FocusRequester(), FocusRequester()) }
    val firstVideoFocusRequester = remember { FocusRequester() }
    val firstCollectionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        tabRequesters.first().requestFocus()
    }
    LaunchedEffect(showCollectionsTab) {
        if (!showCollectionsTab && selectedTab != 0) {
            selectedTab = 0
            delay(40)
            tabRequesters.first().requestFocus()
        }
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
            labels = buildList {
                add("视频 ${videos.size}")
                if (showCollectionsTab) {
                    add(if (collectionsLoaded) "合集 ${collections.size}" else "合集")
                }
            },
            selectedIndex = activeTab,
            requesters = tabRequesters,
            downFocusRequester = when (activeTab) {
                0 -> firstVideoFocusRequester.takeIf {
                    videos.isNotEmpty() || (!loading && error != null)
                }

                else -> firstCollectionFocusRequester.takeIf {
                    collections.isNotEmpty() ||
                        (!collectionsLoading && collectionsError != null)
                }
            },
            onSelected = {
                selectedTab = it
                onTabSelected(it)
            },
        )
        Spacer(Modifier.height(7.dp))
        when (activeTab) {
            0 -> SidebarVideoList(
                videos = videos,
                progressByVideoId = progressByVideoId,
                loading = loading,
                loadingMore = loadingMore,
                error = error,
                hasMore = hasMore,
                emptyText = "博主还没有公开视频",
                firstFocusRequester = firstVideoFocusRequester,
                upFocusRequester = tabRequesters.first(),
                onRetry = onRetryVideos,
                onLoadMore = onLoadMore,
                onPlay = onPlay,
            )

            1 -> SidebarCollectionList(
                collections = collections,
                loading = collectionsLoading,
                loadingMore = collectionsLoadingMore,
                error = collectionsError,
                hasMore = collectionsHasMore,
                openingCollectionId = openingCollectionId,
                firstFocusRequester = firstCollectionFocusRequester,
                upFocusRequester = tabRequesters.getOrNull(1),
                onRetry = onRetryCollections,
                onLoadMore = onLoadMoreCollections,
                onPlay = onPlayCollection,
            )
        }
    }
}

@Composable
private fun SidebarFrame(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onClose)
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
                        modifier = Modifier.fillMaxWidth(),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
    onLogout: (() -> Unit)? = null,
) {
    val logoutFocusRequester = remember { FocusRequester() }
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
                    modifier = Modifier.weight(1f, fill = false),
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
                if (onLogout != null) {
                    Spacer(Modifier.width(6.dp))
                    SidebarPillButton(
                        label = "退出登录",
                        focusRequester = logoutFocusRequester,
                        compact = true,
                        onClick = onLogout,
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
    downFocusRequester: FocusRequester? = null,
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
                downFocusRequester = downFocusRequester,
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
    downFocusRequester: FocusRequester?,
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
                when (event.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.repeatCount == 0
                        ) {
                            onClick()
                        }
                        true
                    }

                    Key.DirectionDown -> {
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.repeatCount == 0
                        ) {
                            downFocusRequester?.requestFocus()
                        }
                        true
                    }

                    else -> false
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
private fun SidebarCollectionList(
    collections: List<VideoCollection>,
    loading: Boolean,
    loadingMore: Boolean,
    error: String?,
    hasMore: Boolean,
    openingCollectionId: String?,
    firstFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (VideoCollection) -> Unit,
) {
    when {
        loading && collections.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Accent,
                    strokeWidth = 2.dp,
                )
            }
        }

        error != null && collections.isEmpty() -> {
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

        collections.isEmpty() -> SidebarEmptyState("暂无公开合集")

        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(
                    items = collections,
                    key = VideoCollection::id,
                ) { collection ->
                    val requester = remember(collection.id) { FocusRequester() }
                    SidebarCollectionCard(
                        collection = collection,
                        opening = openingCollectionId == collection.id,
                        focusRequester = if (collection.id == collections.first().id) {
                            firstFocusRequester ?: requester
                        } else {
                            requester
                        },
                        upFocusRequester = upFocusRequester,
                        onPlay = { onPlay(collection) },
                    )
                }
                if (error != null) {
                    item(
                        key = "sidebar-collection-error",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
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
                    item(
                        key = "sidebar-collection-load-more",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        LaunchedEffect(collections.size, hasMore) {
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
private fun SidebarCollectionCard(
    collection: VideoCollection,
    opening: Boolean,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    onPlay: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(
                shape = cardShape,
                style = GlassStyle(
                    top = if (focused) Color(0xF03C2231) else Color(0xB81A2030),
                    bottom = if (focused) Color(0xE01D0A13) else Color(0xA60C111B),
                    border = if (focused) {
                        Color.White.copy(alpha = 0.94f)
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    },
                    sheen = Color.White.copy(alpha = if (focused) 0.15f else 0.055f),
                    shadowAlpha = if (focused) 0.28f else 0.12f,
                ),
                elevation = if (focused) 7.dp else 2.dp,
            )
            .focusRequester(focusRequester)
            .then(
                if (upFocusRequester != null) {
                    Modifier.focusProperties { up = upFocusRequester }
                } else {
                    Modifier
                },
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (
                    (event.key == Key.DirectionCenter || event.key == Key.Enter) &&
                    event.type == KeyEventType.KeyDown
                ) {
                    if (event.nativeKeyEvent.repeatCount == 0 && !opening) onPlay()
                    true
                } else {
                    false
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color(0xFF11151E)),
        ) {
            collection.coverUrl?.let { coverUrl ->
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
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.76f),
                            ),
                        ),
                    ),
            )
            Text(
                text = collection.videoCount.takeIf { it > 0 }
                    ?.let { "$it 集" }
                    ?: "合集",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                ),
            )
            if (opening) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Accent,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
        ) {
            Text(
                text = collection.name,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
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
                    modifier = Modifier.size(9.dp),
                    tint = TextSecondary,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = buildString {
                        append("播放 ${formatCount(collection.playCount)}")
                        if (collection.videoCount > 0) {
                            append("  ·  ")
                            append(collection.videoCount)
                            append(" 集")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
    upFocusRequester: FocusRequester? = null,
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(
                    items = videos,
                    key = ShortVideo::id,
                ) { video ->
                    val requester = remember(video.id) { FocusRequester() }
                    SidebarVideoCard(
                        video = video,
                        progress = progressByVideoId[video.id],
                        focusRequester = if (video.id == videos.first().id) {
                            firstFocusRequester ?: requester
                        } else {
                            requester
                        },
                        onPlay = { onPlay(video) },
                        upFocusRequester = upFocusRequester,
                    )
                }
                if (error != null) {
                    item(
                        key = "sidebar-error",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
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
                    item(
                        key = "sidebar-load-more",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
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
private fun SidebarVideoCard(
    video: ShortVideo,
    progress: PlaybackProgress?,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester? = null,
    onPlay: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val lowTier = LocalDevicePerformanceProfile.current.lowTier
    val cardShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(
                shape = cardShape,
                style = GlassStyle(
                    top = if (focused) Color(0xF03C2231) else Color(0xB81A2030),
                    bottom = if (focused) Color(0xE01D0A13) else Color(0xA60C111B),
                    border = if (focused) {
                        Color.White.copy(alpha = 0.94f)
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    },
                    sheen = Color.White.copy(alpha = if (focused) 0.15f else 0.055f),
                    shadowAlpha = if (focused) 0.28f else 0.12f,
                ),
                elevation = if (focused) 7.dp else 2.dp,
            )
            .focusRequester(focusRequester)
            .then(
                if (upFocusRequester != null) {
                    Modifier.focusProperties { up = upFocusRequester }
                } else {
                    Modifier
                },
            )
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
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
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f),
                            ),
                        ),
                    ),
            )
            Text(
                text = formatDuration(progress?.durationMs ?: video.durationMs),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                ),
            )
            VideoProgressBar(
                progress = progress?.fraction ?: 0f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                height = 2.dp,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
        ) {
            Text(
                text = video.description.ifBlank { "抖音短视频" },
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
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
                    modifier = Modifier.size(9.dp),
                    tint = TextSecondary,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = buildMetadata(video, progress),
                    modifier = Modifier.weight(1f),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MyProfileContent(
    profile: MyProfileUiState,
    firstFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (ShortVideo) -> Unit,
) {
    SidebarVideoList(
        videos = profile.videos,
        progressByVideoId = profile.progress,
        loading = profile.loading || profile.videosLoading,
        loadingMore = profile.videosLoadingMore,
        error = profile.videosError ?: profile.error,
        hasMore = profile.videosHasMore,
        emptyText = if (profile.profile == null) {
            "账号资料暂未加载"
        } else {
            "还没有发布视频"
        },
        firstFocusRequester = firstFocusRequester,
        upFocusRequester = upFocusRequester,
        onRetry = onRetry,
        onLoadMore = onLoadMore,
        onPlay = onPlay,
    )
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
    compact: Boolean = false,
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
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 14.dp,
                vertical = if (compact) 3.dp else 7.dp,
            ),
            color = if (focused) Color(0xFF17120B) else TextPrimary,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = if (compact) 9.5.sp else 12.sp,
                lineHeight = if (compact) 12.sp else 15.sp,
            ),
            fontWeight = FontWeight.SemiBold,
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
