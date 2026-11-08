package com.dongpiandi.douyintv.ui

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dongpiandi.douyintv.DouyinTvApplication
import com.dongpiandi.douyintv.model.DanmakuMessage
import com.dongpiandi.douyintv.model.LivePlayback
import com.dongpiandi.douyintv.model.LiveRoom
import com.dongpiandi.douyintv.model.PlaybackHistoryEntry
import com.dongpiandi.douyintv.model.PlaybackProgress
import com.dongpiandi.douyintv.model.ShortComment
import com.dongpiandi.douyintv.model.ShortVideo
import com.dongpiandi.douyintv.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class MainTab {
    SHORTS,
    LIVE,
}

enum class ShortsSource {
    RECOMMEND,
    FAVORITES,
    AUTHOR,
}

enum class MyHomeTab {
    HISTORY,
    FAVORITES,
    PROFILE,
}

data class ShortsUiState(
    val videos: List<ShortVideo> = emptyList(),
    val index: Int = 0,
    val page: Int = 0,
    val source: ShortsSource = ShortsSource.RECOMMEND,
    val loading: Boolean = false,
    val paused: Boolean = false,
    val error: String? = null,
) {
    val current: ShortVideo?
        get() = videos.getOrNull(index)
}

data class LiveListUiState(
    val rooms: List<LiveRoom> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val lastFocusedRoomId: String? = null,
    val restoreFocusRoomId: String? = null,
)

data class LivePlayerUiState(
    val room: LiveRoom,
    val playback: LivePlayback? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

data class EngagementUiState(
    val videoId: String = "",
    val danmaku: List<DanmakuMessage> = emptyList(),
    val comments: List<ShortComment> = emptyList(),
    val visible: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val cursor: Long = 0,
    val hasMore: Boolean = false,
    val total: Long = 0,
)

data class FavoritesUiState(
    val visible: Boolean = false,
    val videos: List<ShortVideo> = emptyList(),
    val ids: Set<String> = emptySet(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val cursor: Long = 0,
    val hasMore: Boolean = false,
    val pendingIds: Set<String> = emptySet(),
    val progress: Map<String, PlaybackProgress> = emptyMap(),
    val resumeOnClose: Boolean = false,
)

data class AuthorVideosUiState(
    val visible: Boolean = false,
    val authorSecUid: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val profile: UserProfile? = null,
    val profileLoading: Boolean = false,
    val profileError: String? = null,
    val videos: List<ShortVideo> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val cursor: Long = 0,
    val hasMore: Boolean = false,
    val progress: Map<String, PlaybackProgress> = emptyMap(),
)

data class MyProfileUiState(
    val profile: UserProfile? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val videos: List<ShortVideo> = emptyList(),
    val videosLoading: Boolean = false,
    val videosLoadingMore: Boolean = false,
    val videosLoaded: Boolean = false,
    val videosError: String? = null,
    val videosCursor: Long = 0,
    val videosHasMore: Boolean = false,
    val progress: Map<String, PlaybackProgress> = emptyMap(),
)

private sealed interface PlaybackProgressCommand {
    data class Remember(val video: ShortVideo) : PlaybackProgressCommand

    data class Save(
        val videoId: String,
        val positionMs: Long,
        val durationMs: Long,
    ) : PlaybackProgressCommand
}

data class AppUiState(
    val tab: MainTab = MainTab.SHORTS,
    val overlayVisible: Boolean = true,
    val accountMenuVisible: Boolean = false,
    val myHomeTab: MyHomeTab = MyHomeTab.PROFILE,
    val loginVisible: Boolean = false,
    val isLoggedIn: Boolean = false,
    val shorts: ShortsUiState = ShortsUiState(),
    val engagement: EngagementUiState = EngagementUiState(),
    val favorites: FavoritesUiState = FavoritesUiState(),
    val history: List<PlaybackHistoryEntry> = emptyList(),
    val myProfile: MyProfileUiState = MyProfileUiState(),
    val authorVideos: AuthorVideosUiState = AuthorVideosUiState(),
    val live: LiveListUiState = LiveListUiState(),
    val livePlayer: LivePlayerUiState? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as DouyinTvApplication).container
    private val sessionStore = container.sessionStore
    private val progressStore = container.progressStore
    private val api = container.api
    private val shortsLoadMutex = Mutex()
    private var engagementJob: Job? = null
    private val progressCommands = Channel<PlaybackProgressCommand>(capacity = 32)

    private val _state = MutableStateFlow(
        AppUiState(
            isLoggedIn = sessionStore.hasSession(),
            history = progressStore.history(),
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                when (val command = progressCommands.receive()) {
                    is PlaybackProgressCommand.Remember ->
                        progressStore.remember(command.video)

                    is PlaybackProgressCommand.Save ->
                        progressStore.save(
                            videoId = command.videoId,
                            positionMs = command.positionMs,
                            durationMs = command.durationMs,
                        )
                }
            }
        }
        loadShorts(reset = true)
        if (sessionStore.hasSession()) {
            loadFavorites(reset = true)
        }
    }

    fun showOverlay() {
        _state.update { it.copy(overlayVisible = true) }
    }

    fun hideOverlay() {
        _state.update {
            it.copy(
                overlayVisible = false,
                accountMenuVisible = false,
            )
        }
    }

    fun prepareEngagement(video: ShortVideo, force: Boolean = false) {
        val previous = _state.value.engagement
        if (
            !force &&
            previous.videoId == video.id &&
            (previous.loading || previous.comments.isNotEmpty() || previous.danmaku.isNotEmpty())
        ) {
            return
        }
        progressCommands.trySend(PlaybackProgressCommand.Remember(video))
        engagementJob?.cancel()
        val sameVideo = previous.videoId == video.id
        _state.update {
            it.copy(
                engagement = EngagementUiState(
                    videoId = video.id,
                    visible = sameVideo && previous.visible,
                    loading = true,
                ),
            )
        }
        engagementJob = viewModelScope.launch {
            val results = coroutineScope {
                val comments = async { runCatching { api.loadComments(video.id) } }
                val danmaku = async {
                    runCatching { api.loadDanmaku(video.id, video.durationMs) }
                }
                comments.await() to danmaku.await()
            }
            if (_state.value.engagement.videoId != video.id) return@launch
            val commentsPage = results.first.getOrNull()
            val comments = commentsPage?.comments.orEmpty()
            val officialDanmaku = results.second.getOrDefault(emptyList())
            val visibleDanmaku = buildDanmakuTimeline(
                official = officialDanmaku,
                durationMs = video.durationMs,
            )
            _state.update { current ->
                if (current.engagement.videoId != video.id) {
                    current
                } else {
                    current.copy(
                        engagement = current.engagement.copy(
                            danmaku = visibleDanmaku,
                            comments = comments,
                            loading = false,
                            loadingMore = false,
                            error = results.first.exceptionOrNull()
                                ?.toDisplayMessage("评论加载失败"),
                            cursor = commentsPage?.cursor ?: 0,
                            hasMore = commentsPage?.hasMore ?: false,
                            total = commentsPage?.total ?: 0,
                        ),
                    )
                }
            }
        }
    }

    fun openComments(video: ShortVideo) {
        val current = _state.value
        _state.update {
            it.copy(
                engagement = it.engagement.copy(
                    visible = true,
                ),
            )
        }
        if (current.engagement.videoId != video.id && !current.engagement.loading) {
            prepareEngagement(video)
        }
    }

    fun closeComments() {
        _state.update { current ->
            current.copy(
                engagement = current.engagement.copy(
                    visible = false,
                ),
            )
        }
    }

    fun loadMoreComments(video: ShortVideo) {
        val engagement = _state.value.engagement
        if (
            engagement.videoId != video.id ||
            engagement.loading ||
            engagement.loadingMore ||
            !engagement.hasMore
        ) {
            return
        }
        val cursor = engagement.cursor
        _state.update {
            it.copy(
                engagement = it.engagement.copy(loadingMore = true, error = null),
            )
        }
        viewModelScope.launch {
            val result = runCatching { api.loadComments(video.id, cursor) }
            _state.update { current ->
                if (current.engagement.videoId != video.id) {
                    current
                } else {
                    val page = result.getOrNull()
                    if (page == null) {
                        current.copy(
                            engagement = current.engagement.copy(
                                loadingMore = false,
                                error = result.exceptionOrNull()
                                    ?.toDisplayMessage("评论加载失败"),
                            ),
                        )
                    } else {
                        current.copy(
                            engagement = current.engagement.copy(
                                comments = (current.engagement.comments + page.comments)
                                    .distinctBy(ShortComment::id),
                                cursor = page.cursor,
                                hasMore = page.hasMore,
                                total = page.total,
                                loadingMore = false,
                                error = null,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun retryComments(video: ShortVideo) {
        prepareEngagement(video, force = true)
    }

    fun toggleFavorite(video: ShortVideo) {
        val current = _state.value
        if (!current.isLoggedIn) {
            openLogin()
            return
        }
        if (video.id in current.favorites.pendingIds) return
        val shouldFavorite = video.id !in current.favorites.ids
        _state.update {
            it.copy(
                favorites = it.favorites.copy(
                    pendingIds = it.favorites.pendingIds + video.id,
                    error = null,
                ),
            )
        }
        viewModelScope.launch {
            runCatching { api.setFavorite(video.id, shouldFavorite) }
                .onSuccess {
                    _state.update { state ->
                        val ids = if (shouldFavorite) {
                            state.favorites.ids + video.id
                        } else {
                            state.favorites.ids - video.id
                        }
                        val videos = if (shouldFavorite) {
                            listOf(video) + state.favorites.videos.filterNot { it.id == video.id }
                        } else {
                            state.favorites.videos.filterNot { it.id == video.id }
                        }
                        state.copy(
                            favorites = state.favorites.copy(
                                ids = ids,
                                videos = videos,
                                pendingIds = state.favorites.pendingIds - video.id,
                                progress = progressStore.load(videos.map(ShortVideo::id)),
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            favorites = it.favorites.copy(
                                pendingIds = it.favorites.pendingIds - video.id,
                                error = error.toDisplayMessage("收藏操作失败"),
                            ),
                        )
                    }
                }
        }
    }

    fun openFavorites() {
        showAccountMenu(MyHomeTab.FAVORITES)
    }

    fun closeFavorites() {
        closeAccountMenu()
    }

    fun retryFavorites() {
        loadFavorites(reset = true)
    }

    fun loadMoreFavorites() {
        loadFavorites(reset = false)
    }

    fun playFavorite(video: ShortVideo) {
        playCollectionVideo(
            video = video,
            sourceVideos = _state.value.favorites.videos,
            source = ShortsSource.FAVORITES,
        )
    }

    fun playHistoryVideo(video: ShortVideo) {
        playCollectionVideo(
            video = video,
            sourceVideos = _state.value.history.map(PlaybackHistoryEntry::video),
            source = ShortsSource.FAVORITES,
        )
    }

    fun playMyVideo(video: ShortVideo) {
        playCollectionVideo(
            video = video,
            sourceVideos = _state.value.myProfile.videos,
            source = ShortsSource.AUTHOR,
        )
    }

    fun openAuthorVideos(video: ShortVideo) {
        if (video.authorSecUid.isBlank()) return
        val current = _state.value
        val sameAuthor = current.authorVideos.authorSecUid == video.authorSecUid
        _state.update {
            if (sameAuthor) {
                it.copy(
                    accountMenuVisible = false,
                    authorVideos = it.authorVideos.copy(
                        visible = true,
                        authorName = video.authorName,
                        authorAvatarUrl = video.authorAvatarUrl,
                        progress = progressStore.load(
                            it.authorVideos.videos.map(ShortVideo::id),
                        ),
                    ),
                )
            } else {
                it.copy(
                    accountMenuVisible = false,
                    authorVideos = AuthorVideosUiState(
                        visible = true,
                        authorSecUid = video.authorSecUid,
                        authorName = video.authorName,
                        authorAvatarUrl = video.authorAvatarUrl,
                        profileLoading = true,
                    ),
                )
            }
        }
        if (!sameAuthor || !_state.value.authorVideos.loaded) {
            loadAuthorVideos(reset = true)
        }
        if (!sameAuthor || _state.value.authorVideos.profile == null) {
            loadAuthorProfile(video.authorSecUid)
        }
    }

    fun closeAuthorVideos() {
        _state.update {
            it.copy(
                overlayVisible = false,
                authorVideos = it.authorVideos.copy(
                    visible = false,
                ),
            )
        }
    }

    fun retryAuthorVideos() {
        loadAuthorVideos(reset = true)
    }

    fun loadMoreAuthorVideos() {
        loadAuthorVideos(reset = false)
    }

    fun playAuthorVideo(video: ShortVideo) {
        playCollectionVideo(
            video = video,
            sourceVideos = _state.value.authorVideos.videos,
            source = ShortsSource.AUTHOR,
        )
    }

    fun retryAuthorProfile() {
        val authorSecUid = _state.value.authorVideos.authorSecUid
        if (authorSecUid.isNotBlank()) loadAuthorProfile(authorSecUid)
    }

    fun recordPlaybackProgress(
        videoId: String,
        positionMs: Long,
        durationMs: Long,
    ) {
        if (videoId.isBlank() || durationMs <= 0L) return
        val resolvedPosition = positionMs.coerceIn(0L, durationMs)
        progressCommands.trySend(
            PlaybackProgressCommand.Save(
                videoId = videoId,
                positionMs = resolvedPosition,
                durationMs = durationMs,
            ),
        )
        if (
            !_state.value.accountMenuVisible &&
            !_state.value.authorVideos.visible
        ) {
            return
        }
        val progress = PlaybackProgress(
            positionMs = resolvedPosition,
            durationMs = durationMs,
            updatedAtMs = System.currentTimeMillis(),
        )
        _state.update {
            it.copy(
                favorites = if (it.favorites.videos.any { video -> video.id == videoId }) {
                    it.favorites.copy(progress = it.favorites.progress + (videoId to progress))
                } else {
                    it.favorites
                },
                history = it.history.map { entry ->
                    if (entry.video.id == videoId) {
                        entry.copy(progress = progress)
                    } else {
                        entry
                    }
                },
                authorVideos = if (it.authorVideos.videos.any { video -> video.id == videoId }) {
                    it.authorVideos.copy(
                        progress = it.authorVideos.progress + (videoId to progress),
                    )
                } else {
                    it.authorVideos
                },
            )
        }
    }

    fun selectTab(tab: MainTab) {
        _state.update { it.copy(tab = tab, overlayVisible = true) }
        if (tab == MainTab.LIVE && !_state.value.live.loaded) {
            loadLiveRooms(reset = true)
        }
    }

    fun previousShort() {
        _state.update { current ->
            val shorts = current.shorts
            if (shorts.index <= 0) {
                current
            } else {
                current.copy(shorts = shorts.copy(index = shorts.index - 1, paused = false))
            }
        }
        prefetchShortsIfNeeded()
    }

    fun nextShort() {
        val current = _state.value.shorts
        if (current.index + 1 < current.videos.size) {
            _state.update {
                it.copy(
                    shorts = it.shorts.copy(
                        index = it.shorts.index + 1,
                        paused = false,
                    ),
                )
            }
            prefetchShortsIfNeeded()
        } else {
            if (current.source == ShortsSource.RECOMMEND) {
                loadShorts(reset = false, advanceAfterLoad = true)
            }
        }
    }

    fun replayCurrentShort() {
        _state.update {
            it.copy(
                shorts = it.shorts.copy(paused = false, error = null),
            )
        }
    }

    fun toggleShortPause() {
        _state.update {
            it.copy(shorts = it.shorts.copy(paused = !it.shorts.paused))
        }
    }

    fun setShortPaused(paused: Boolean) {
        _state.update {
            it.copy(shorts = it.shorts.copy(paused = paused))
        }
    }

    fun onShortEnded() {
        nextShort()
    }

    fun onShortPlaybackError(message: String) {
        _state.update {
            it.copy(shorts = it.shorts.copy(error = message, paused = true))
        }
    }

    fun refreshShorts() {
        loadShorts(reset = true)
    }

    fun loadLiveRooms(reset: Boolean = false) {
        if (_state.value.live.loading) return
        _state.update {
            it.copy(
                live = it.live.copy(
                    loading = true,
                    error = null,
                    rooms = if (reset) emptyList() else it.live.rooms,
                ),
            )
        }
        viewModelScope.launch {
            runCatching { api.loadLiveRooms() }
                .onSuccess { rooms ->
                    _state.update { current ->
                        val merged = if (reset) {
                            rooms
                        } else {
                            (current.live.rooms + rooms).distinctBy(LiveRoom::id)
                        }
                        current.copy(
                            live = current.live.copy(
                                rooms = merged,
                                loading = false,
                                loaded = true,
                                error = if (merged.isEmpty()) "暂时没有可用的直播" else null,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            live = it.live.copy(
                                loading = false,
                                loaded = true,
                                error = error.toDisplayMessage("直播加载失败"),
                            ),
                        )
                    }
                }
        }
    }

    fun rememberLiveFocus(roomId: String) {
        _state.update {
            it.copy(
                live = it.live.copy(
                    lastFocusedRoomId = roomId,
                    restoreFocusRoomId = null,
                ),
            )
        }
    }

    fun consumeLiveFocusRestore() {
        _state.update {
            it.copy(live = it.live.copy(restoreFocusRoomId = null))
        }
    }

    fun openLiveRoom(room: LiveRoom) {
        _state.update {
            it.copy(
                live = it.live.copy(lastFocusedRoomId = room.id),
                livePlayer = LivePlayerUiState(room = room),
            )
        }
        resolveLiveRoom(room)
    }

    fun retryLiveRoom() {
        val room = _state.value.livePlayer?.room ?: return
        _state.update {
            it.copy(livePlayer = LivePlayerUiState(room = room))
        }
        resolveLiveRoom(room)
    }

    fun closeLivePlayer() {
        val focusedId = _state.value.livePlayer?.room?.id
            ?: _state.value.live.lastFocusedRoomId
        _state.update {
            it.copy(
                livePlayer = null,
                live = it.live.copy(restoreFocusRoomId = focusedId),
            )
        }
    }

    fun openLogin() {
        _state.update { it.copy(loginVisible = true, accountMenuVisible = false) }
    }

    fun hasSessionCookie(cookieHeader: String): Boolean =
        sessionStore.hasSessionCookie(cookieHeader)

    fun closeLogin() {
        _state.update { it.copy(loginVisible = false) }
    }

    fun completeLogin(cookieHeader: String) {
        if (!sessionStore.saveCookieHeader(cookieHeader)) return
        CookieManager.getInstance().flush()
        _state.update {
            it.copy(
                loginVisible = false,
                isLoggedIn = true,
                shorts = it.shorts.copy(error = null),
            )
        }
        loadShorts(reset = true)
        loadFavorites(reset = true)
    }

    fun showAccountMenu(tab: MyHomeTab = MyHomeTab.PROFILE) {
        val current = _state.value
        if (!current.isLoggedIn) {
            openLogin()
            return
        }
        _state.update {
            it.copy(
                overlayVisible = true,
                accountMenuVisible = true,
                myHomeTab = tab,
                history = progressStore.history(),
                favorites = it.favorites.copy(
                    progress = progressStore.load(it.favorites.videos.map(ShortVideo::id)),
                ),
                authorVideos = it.authorVideos.copy(visible = false),
            )
        }
        if (tab == MyHomeTab.FAVORITES && !_state.value.favorites.loaded) {
            loadFavorites(reset = true)
        }
        if (tab == MyHomeTab.PROFILE) {
            loadSelfProfile()
        }
    }

    fun closeAccountMenu() {
        _state.update {
            it.copy(
                overlayVisible = false,
                accountMenuVisible = false,
            )
        }
    }

    fun selectMyHomeTab(tab: MyHomeTab) {
        if (!_state.value.accountMenuVisible) {
            showAccountMenu(tab)
            return
        }
        _state.update {
            it.copy(
                myHomeTab = tab,
                history = if (tab == MyHomeTab.HISTORY) {
                    progressStore.history()
                } else {
                    it.history
                },
            )
        }
        when (tab) {
            MyHomeTab.FAVORITES -> {
                if (!_state.value.favorites.loaded) loadFavorites(reset = true)
            }

            MyHomeTab.PROFILE -> loadSelfProfile()
            MyHomeTab.HISTORY -> Unit
        }
    }

    fun retryMyProfile() {
        val profile = _state.value.myProfile
        val secUid = profile.profile?.secUid.orEmpty()
        if (secUid.isBlank()) {
            loadSelfProfile(force = true)
        } else {
            loadSelfVideos(secUid = secUid, reset = true)
        }
    }

    fun loadMoreMyVideos() {
        val secUid = _state.value.myProfile.profile?.secUid.orEmpty()
        if (secUid.isNotBlank()) {
            loadSelfVideos(secUid = secUid, reset = false)
        }
    }

    fun logout() {
        sessionStore.clear()
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
        }
        _state.update {
            it.copy(
                isLoggedIn = false,
                accountMenuVisible = false,
                loginVisible = false,
                favorites = FavoritesUiState(),
                myProfile = MyProfileUiState(),
                authorVideos = AuthorVideosUiState(),
            )
        }
    }

    private fun loadFavorites(reset: Boolean) {
        val snapshot = _state.value.favorites
        if (snapshot.loading || snapshot.loadingMore) return
        if (!reset && !snapshot.hasMore) return
        val cursor = if (reset) 0L else snapshot.cursor
        _state.update {
            it.copy(
                favorites = it.favorites.copy(
                    loading = reset,
                    loadingMore = !reset,
                    error = null,
                    videos = if (reset) emptyList() else it.favorites.videos,
                ),
            )
        }
        viewModelScope.launch {
            runCatching { api.loadFavoriteVideos(cursor = cursor) }
                .onSuccess { page ->
                    _state.update { state ->
                        val existing = if (reset) emptyList() else state.favorites.videos
                        val merged = (existing + page.videos)
                            .distinctBy(ShortVideo::id)
                        state.copy(
                            favorites = state.favorites.copy(
                                videos = merged,
                                ids = state.favorites.ids + page.videos.map(ShortVideo::id),
                                loading = false,
                                loaded = true,
                                loadingMore = false,
                                error = null,
                                cursor = page.cursor,
                                hasMore = page.hasMore,
                                progress = progressStore.load(merged.map(ShortVideo::id)),
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            favorites = it.favorites.copy(
                                loading = false,
                                loadingMore = false,
                                error = error.toDisplayMessage("收藏列表加载失败"),
                            ),
                        )
                    }
                }
        }
    }

    private fun loadSelfProfile(force: Boolean = false) {
        val current = _state.value
        if (!current.isLoggedIn) return
        val profile = current.myProfile
        if (profile.loading) return
        if (!force && profile.profile != null) {
            val secUid = profile.profile.secUid
            if (
                secUid.isNotBlank() &&
                !profile.videosLoaded &&
                !profile.videosLoading &&
                !profile.videosLoadingMore
            ) {
                loadSelfVideos(secUid = secUid, reset = true)
            }
            return
        }
        _state.update {
            it.copy(
                myProfile = it.myProfile.copy(
                    loading = true,
                    error = null,
                ),
            )
        }
        viewModelScope.launch {
            runCatching { api.loadSelfProfile() }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            myProfile = it.myProfile.copy(
                                profile = result,
                                loading = false,
                                error = null,
                            ),
                        )
                    }
                    if (result.secUid.isNotBlank()) {
                        loadSelfVideos(secUid = result.secUid, reset = true)
                    } else {
                        _state.update {
                            it.copy(
                                myProfile = it.myProfile.copy(
                                    videosError = "无法获取账号作品",
                                ),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            myProfile = it.myProfile.copy(
                                loading = false,
                                error = error.toDisplayMessage("账号资料加载失败"),
                            ),
                        )
                    }
                }
        }
    }

    private fun loadSelfVideos(
        secUid: String,
        reset: Boolean,
    ) {
        val snapshot = _state.value.myProfile
        if (snapshot.profile?.secUid != secUid) return
        if (snapshot.videosLoading || snapshot.videosLoadingMore) return
        if (!reset && !snapshot.videosHasMore) return
        val cursor = if (reset) 0L else snapshot.videosCursor
        _state.update {
            it.copy(
                myProfile = it.myProfile.copy(
                    videosLoading = reset,
                    videosLoadingMore = !reset,
                    videosError = null,
                    videos = if (reset) emptyList() else it.myProfile.videos,
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                api.loadAuthorVideos(authorSecUid = secUid, cursor = cursor)
            }
                .onSuccess { page ->
                    _state.update { state ->
                        if (state.myProfile.profile?.secUid != secUid) {
                            state
                        } else {
                            val existing = if (reset) emptyList() else state.myProfile.videos
                            val merged = (existing + page.videos)
                                .distinctBy(ShortVideo::id)
                            state.copy(
                                myProfile = state.myProfile.copy(
                                    videos = merged,
                                    videosLoading = false,
                                    videosLoadingMore = false,
                                    videosLoaded = true,
                                    videosError = null,
                                    videosCursor = page.cursor,
                                    videosHasMore = page.hasMore,
                                    progress = progressStore.load(merged.map(ShortVideo::id)),
                                ),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update { state ->
                        if (state.myProfile.profile?.secUid != secUid) {
                            state
                        } else {
                            state.copy(
                                myProfile = state.myProfile.copy(
                                    videosLoading = false,
                                    videosLoadingMore = false,
                                    videosError = error.toDisplayMessage("我的作品加载失败"),
                                ),
                            )
                        }
                    }
                }
        }
    }

    private fun loadAuthorProfile(authorSecUid: String) {
        if (authorSecUid.isBlank()) return
        _state.update {
            if (it.authorVideos.authorSecUid != authorSecUid) {
                it
            } else {
                it.copy(
                    authorVideos = it.authorVideos.copy(
                        profileLoading = true,
                        profileError = null,
                    ),
                )
            }
        }
        viewModelScope.launch {
            runCatching { api.loadAuthorProfile(authorSecUid) }
                .onSuccess { profile ->
                    _state.update {
                        if (it.authorVideos.authorSecUid != authorSecUid) {
                            it
                        } else {
                            it.copy(
                                authorVideos = it.authorVideos.copy(
                                    profile = profile,
                                    profileLoading = false,
                                    profileError = null,
                                    authorName = profile.nickname,
                                    authorAvatarUrl = profile.avatarUrl
                                        ?: it.authorVideos.authorAvatarUrl,
                                ),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        if (it.authorVideos.authorSecUid != authorSecUid) {
                            it
                        } else {
                            it.copy(
                                authorVideos = it.authorVideos.copy(
                                    profileLoading = false,
                                    profileError = error.toDisplayMessage("博主资料加载失败"),
                                ),
                            )
                        }
                    }
                }
        }
    }

    private fun loadAuthorVideos(reset: Boolean) {
        val snapshot = _state.value.authorVideos
        val authorSecUid = snapshot.authorSecUid
        if (authorSecUid.isBlank()) return
        if (snapshot.loading || snapshot.loadingMore) return
        if (!reset && !snapshot.hasMore) return
        val cursor = if (reset) 0L else snapshot.cursor
        _state.update {
            it.copy(
                authorVideos = it.authorVideos.copy(
                    loading = reset,
                    loadingMore = !reset,
                    error = null,
                    videos = if (reset) emptyList() else it.authorVideos.videos,
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                api.loadAuthorVideos(authorSecUid = authorSecUid, cursor = cursor)
            }
                .onSuccess { page ->
                    _state.update { state ->
                        if (state.authorVideos.authorSecUid != authorSecUid) {
                            state
                        } else {
                            val existing = if (reset) emptyList() else state.authorVideos.videos
                            val merged = (existing + page.videos)
                                .distinctBy(ShortVideo::id)
                            state.copy(
                                authorVideos = state.authorVideos.copy(
                                    videos = merged,
                                    loading = false,
                                    loaded = true,
                                    loadingMore = false,
                                    error = null,
                                    cursor = page.cursor,
                                    hasMore = page.hasMore,
                                    progress = progressStore.load(merged.map(ShortVideo::id)),
                                ),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update { state ->
                        if (state.authorVideos.authorSecUid != authorSecUid) {
                            state
                        } else {
                            state.copy(
                                authorVideos = state.authorVideos.copy(
                                    loading = false,
                                    loadingMore = false,
                                    error = error.toDisplayMessage("博主视频加载失败"),
                                ),
                            )
                        }
                    }
                }
        }
    }

    private fun playCollectionVideo(
        video: ShortVideo,
        sourceVideos: List<ShortVideo>,
        source: ShortsSource,
    ) {
        val ordered = sourceVideos.ifEmpty { listOf(video) }
        val index = ordered.indexOfFirst { it.id == video.id }.takeIf { it >= 0 } ?: 0
        _state.update {
            it.copy(
                tab = MainTab.SHORTS,
                overlayVisible = false,
                engagement = EngagementUiState(),
                accountMenuVisible = false,
                authorVideos = it.authorVideos.copy(visible = false),
                shorts = it.shorts.copy(
                    videos = ordered,
                    index = index,
                    page = 0,
                    source = source,
                    loading = false,
                    paused = false,
                    error = null,
                ),
            )
        }
    }

    private fun resolveLiveRoom(room: LiveRoom) {
        viewModelScope.launch {
            runCatching { api.resolveLiveRoom(room) }
                .onSuccess { playback ->
                    _state.update { current ->
                        val player = current.livePlayer
                        if (player?.room?.id != room.id) {
                            current
                        } else {
                            current.copy(
                                livePlayer = player.copy(
                                    playback = playback,
                                    loading = false,
                                    error = null,
                                ),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update { current ->
                        val player = current.livePlayer
                        if (player?.room?.id != room.id) {
                            current
                        } else {
                            current.copy(
                                livePlayer = player.copy(
                                    loading = false,
                                    error = error.toDisplayMessage("直播打开失败"),
                                ),
                            )
                        }
                    }
                }
        }
    }

    private fun prefetchShortsIfNeeded() {
        val shorts = _state.value.shorts
        if (
            shorts.source == ShortsSource.RECOMMEND &&
            shorts.videos.isNotEmpty() &&
            shorts.index >= shorts.videos.lastIndex - 4
        ) {
            loadShorts(reset = false)
        }
    }

    private fun loadShorts(reset: Boolean, advanceAfterLoad: Boolean = false) {
        viewModelScope.launch {
            shortsLoadMutex.withLock {
                val snapshot = _state.value.shorts
                if (snapshot.loading) return@withLock
                val nextPage = if (reset) 1 else snapshot.page + 1
                _state.update {
                    it.copy(
                        shorts = it.shorts.copy(
                            loading = true,
                            error = null,
                            videos = if (reset) emptyList() else it.shorts.videos,
                            index = if (reset) 0 else it.shorts.index,
                            page = if (reset) 0 else it.shorts.page,
                            source = ShortsSource.RECOMMEND,
                        ),
                    )
                }
                runCatching { api.loadShortVideos(nextPage) }
                    .onSuccess { videos ->
                        _state.update { current ->
                            val existing = if (reset) emptyList() else current.shorts.videos
                            val seen = existing.mapTo(mutableSetOf(), ShortVideo::id)
                            val merged = existing + videos.filter { seen.add(it.id) }
                            val targetIndex = when {
                                merged.isEmpty() -> 0
                                advanceAfterLoad && current.shorts.index + 1 < merged.size ->
                                    current.shorts.index + 1
                                else -> current.shorts.index.coerceIn(0, merged.lastIndex)
                            }
                            current.copy(
                                shorts = current.shorts.copy(
                                    videos = merged,
                                    index = targetIndex,
                                    page = nextPage,
                                    loading = false,
                                    paused = false,
                                    error = if (merged.isEmpty()) "暂时没有视频" else null,
                                ),
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                shorts = it.shorts.copy(
                                    loading = false,
                                    error = error.toDisplayMessage("视频加载失败"),
                                ),
                            )
                        }
                    }
            }
        }
    }

    private fun Throwable.toDisplayMessage(fallback: String): String =
        message?.takeIf { it.isNotBlank() } ?: fallback

}
