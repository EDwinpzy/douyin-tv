package com.dongpiandi.douyintv.ui

import android.app.Application
import android.util.Log
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
import com.dongpiandi.douyintv.model.VideoCollection
import com.dongpiandi.douyintv.voice.DouyinVoiceCommand
import com.dongpiandi.douyintv.voice.DouyinVoiceOperation
import kotlinx.coroutines.CancellationException
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
    WATCH_LATER,
    AUTHOR,
    SEARCH,
}

enum class MyHomeTab {
    HISTORY,
    WATCH_LATER,
    FAVORITES,
    PROFILE,
}

enum class SidebarKind {
    MY_HOME,
    AUTHOR,
}

data class SidebarPlaybackReturn(
    val kind: SidebarKind,
    val myHomeTab: MyHomeTab = MyHomeTab.PROFILE,
    val authorTab: Int = 0,
    val videoId: String? = null,
    val collectionId: String? = null,
)

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

data class WatchLaterUiState(
    val videos: List<ShortVideo> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val offset: Long = 0,
    val hasMore: Boolean = false,
    val progress: Map<String, PlaybackProgress> = emptyMap(),
)

data class AccountHistoryUiState(
    val videos: List<ShortVideo> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val cursor: Long = 0,
    val hasMore: Boolean = false,
)

data class FollowUiState(
    val statusByAuthor: Map<String, Boolean> = emptyMap(),
    val pendingAuthors: Set<String> = emptySet(),
    val error: String? = null,
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
    val likes: List<ShortVideo> = emptyList(),
    val likesLoading: Boolean = false,
    val likesLoaded: Boolean = false,
    val likesLoadingMore: Boolean = false,
    val likesError: String? = null,
    val likesCursor: Long = 0,
    val likesHasMore: Boolean = false,
    val likesProgress: Map<String, PlaybackProgress> = emptyMap(),
    val collections: List<VideoCollection> = emptyList(),
    val collectionsLoading: Boolean = false,
    val collectionsLoaded: Boolean = false,
    val collectionsLoadingMore: Boolean = false,
    val collectionsError: String? = null,
    val collectionsCursor: Long = 0,
    val collectionsHasMore: Boolean = false,
    val openingCollectionId: String? = null,
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

data class SearchUiState(
    val visible: Boolean = false,
    val query: String = "",
    val submittedQuery: String = "",
    val results: List<ShortVideo> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val cursor: Long = 0,
    val hasMore: Boolean = false,
    val loaded: Boolean = false,
    val verificationRequired: Boolean = false,
    val searchId: String = "",
    val backtrace: String = "",
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
    val watchLater: WatchLaterUiState = WatchLaterUiState(),
    val accountHistory: AccountHistoryUiState = AccountHistoryUiState(),
    val follows: FollowUiState = FollowUiState(),
    val history: List<PlaybackHistoryEntry> = emptyList(),
    val myProfile: MyProfileUiState = MyProfileUiState(),
    val authorVideos: AuthorVideosUiState = AuthorVideosUiState(),
    val search: SearchUiState = SearchUiState(),
    val live: LiveListUiState = LiveListUiState(),
    val livePlayer: LivePlayerUiState? = null,
    val recommendationReturn: ShortsUiState? = null,
    val sidebarPlaybackReturn: SidebarPlaybackReturn? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as DouyinTvApplication).container
    private val sessionStore = container.sessionStore
    private val progressStore = container.progressStore
    private val api = container.api
    private val shortsLoadMutex = Mutex()
    private var engagementJob: Job? = null
    private var searchJob: Job? = null
    private var pendingVoiceOpenQuery: String? = null
    private var lastSessionValidationAtMs = 0L
    private val progressCommands = Channel<PlaybackProgressCommand>(capacity = 32)
    private var accountHistoryJob: Job? = null
    private var accountHistoryGeneration = 0L
    private val accountHistoryWriteTimes = mutableMapOf<String, Long>()
    private var lastAccountHistoryWriteAtMs = 0L
    private var lastProgressVideoId: String? = null
    private var lastProgressPositionMs = 0L
    private var lastProgressDurationMs = 0L
    private var historyWriteChainVideoId = ""
    private var historyWriteChainVideoSeen = 0

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

    internal fun applyVoiceCommand(command: DouyinVoiceCommand): Boolean {
        if (command.operation == DouyinVoiceOperation.SEARCH) {
            startVoiceSearch(command.query, command.openFirstResult)
            return command.query.isNotBlank()
        }

        val current = _state.value
        val video = current.shorts.current ?: return false
        if (current.tab != MainTab.SHORTS) return false
        return when (command.operation) {
            DouyinVoiceOperation.PLAY -> {
                setShortPaused(false)
                true
            }
            DouyinVoiceOperation.PAUSE -> {
                setShortPaused(true)
                true
            }
            DouyinVoiceOperation.NEXT -> {
                nextShort()
                true
            }
            DouyinVoiceOperation.PREVIOUS -> {
                previousShort()
                true
            }
            DouyinVoiceOperation.COMMENT -> {
                openComments(video)
                true
            }
            DouyinVoiceOperation.FAVORITE -> {
                setFavorite(video, shouldFavorite = true)
                true
            }
            DouyinVoiceOperation.UNFAVORITE -> {
                setFavorite(video, shouldFavorite = false)
                true
            }
            DouyinVoiceOperation.AUTHOR -> {
                openAuthorVideos(video)
                true
            }
            DouyinVoiceOperation.SEARCH -> false
        }
    }

    private fun startVoiceSearch(
        query: String,
        openFirstResult: Boolean,
    ) {
        val normalized = query.trim().take(80)
        if (normalized.isEmpty()) return
        pendingVoiceOpenQuery = normalized.takeIf { openFirstResult }
        _state.update {
            it.copy(
                tab = MainTab.SHORTS,
                livePlayer = null,
                loginVisible = false,
            )
        }
        openSearch()
        updateSearchQuery(normalized)
        submitSearch()
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
        setFavorite(
            video = video,
            shouldFavorite = video.id !in _state.value.favorites.ids,
        )
    }

    fun toggleFollow(video: ShortVideo) {
        val current = _state.value
        if (!current.isLoggedIn) {
            openLogin()
            return
        }
        val authorKey = video.authorSecUid.ifBlank { video.authorUid }
        if (authorKey.isBlank() || authorKey in current.follows.pendingAuthors) return
        val isFollowing = current.follows.statusByAuthor[authorKey]
            ?: (video.authorFollowStatus > 0)
        val shouldFollow = !isFollowing
        _state.update {
            it.copy(
                follows = it.follows.copy(
                    pendingAuthors = it.follows.pendingAuthors + authorKey,
                    error = null,
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                api.setFollow(
                    authorUid = video.authorUid,
                    authorSecUid = video.authorSecUid,
                    follow = shouldFollow,
                )
            }
                .onSuccess { followed ->
                    _state.update { state ->
                        val profile = state.authorVideos.profile
                        val updateProfile = state.authorVideos.authorSecUid ==
                            video.authorSecUid && profile != null
                        state.copy(
                            follows = state.follows.copy(
                                statusByAuthor = state.follows.statusByAuthor +
                                    (authorKey to followed),
                                pendingAuthors = state.follows.pendingAuthors - authorKey,
                                error = null,
                            ),
                            authorVideos = if (updateProfile) {
                                state.authorVideos.copy(
                                    profile = profile.copy(
                                        followStatus = if (followed) 1 else 0,
                                    ),
                                )
                            } else {
                                state.authorVideos
                            },
                        )
                    }
                }
                .onFailure { error ->
                    if (clearInvalidSession(error)) return@onFailure
                    _state.update {
                        it.copy(
                            follows = it.follows.copy(
                                pendingAuthors = it.follows.pendingAuthors - authorKey,
                                error = error.toDisplayMessage("关注操作失败"),
                            ),
                        )
                    }
                }
        }
    }

    private fun setFavorite(
        video: ShortVideo,
        shouldFavorite: Boolean,
    ) {
        val current = _state.value
        if (!current.isLoggedIn) {
            openLogin()
            return
        }
        if (video.id in current.favorites.pendingIds) return
        if ((video.id in current.favorites.ids) == shouldFavorite) return
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
                    if (clearInvalidSession(error)) return@onFailure
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
            sidebarReturn = myHomeSidebarPlaybackReturn(
                tab = MyHomeTab.FAVORITES,
                videoId = video.id,
            ),
        )
    }

    fun retryWatchLater() {
        loadWatchLater(reset = true)
    }

    fun loadMoreWatchLater() {
        loadWatchLater(reset = false)
    }

    fun retryAccountHistory() {
        loadAccountHistory(reset = true)
    }

    fun loadMoreAccountHistory() {
        loadAccountHistory(reset = false)
    }

    fun playWatchLater(video: ShortVideo) {
        playCollectionVideo(
            video = video,
            sourceVideos = _state.value.watchLater.videos,
            source = ShortsSource.WATCH_LATER,
            sidebarReturn = myHomeSidebarPlaybackReturn(
                tab = MyHomeTab.WATCH_LATER,
                videoId = video.id,
            ),
        )
    }

    fun playHistoryVideo(video: ShortVideo) {
        playCollectionVideo(
            video = video,
            sourceVideos = _state.value.history.map(PlaybackHistoryEntry::video),
            source = ShortsSource.FAVORITES,
            sidebarReturn = myHomeSidebarPlaybackReturn(
                tab = MyHomeTab.HISTORY,
                videoId = video.id,
            ),
        )
    }

    fun playMyVideo(video: ShortVideo) {
        playCollectionVideo(
            video = video,
            sourceVideos = _state.value.myProfile.videos,
            source = ShortsSource.AUTHOR,
            sidebarReturn = myHomeSidebarPlaybackReturn(
                tab = MyHomeTab.PROFILE,
                videoId = video.id,
            ),
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
                    sidebarPlaybackReturn = null,
                    authorVideos = it.authorVideos.copy(
                        visible = true,
                        authorName = video.authorName,
                        authorAvatarUrl = video.authorAvatarUrl,
                        progress = progressStore.load(
                            it.authorVideos.videos.map(ShortVideo::id),
                        ),
                        likesProgress = progressStore.load(
                            it.authorVideos.likes.map(ShortVideo::id),
                        ),
                    ),
                )
            } else {
                it.copy(
                    accountMenuVisible = false,
                    sidebarPlaybackReturn = null,
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
        loadAuthorVideos(reset = true, keepContent = sameAuthor)
        loadAuthorProfile(video.authorSecUid)
        loadAuthorCollectionsPage(reset = true, keepContent = sameAuthor)
    }

    fun closeAuthorVideos() {
        _state.update {
            it.copy(
                overlayVisible = true,
                authorVideos = it.authorVideos.copy(
                    visible = false,
                ),
                sidebarPlaybackReturn = null,
            )
        }
    }

    fun restoreSidebarFromPlayback(): Boolean {
        val saved = _state.value.sidebarPlaybackReturn ?: return false
        _state.update {
            when (saved.kind) {
                SidebarKind.MY_HOME -> it.copy(
                    overlayVisible = false,
                    accountMenuVisible = true,
                    myHomeTab = saved.myHomeTab,
                    authorVideos = it.authorVideos.copy(visible = false),
                    search = it.search.copy(visible = false),
                )

                SidebarKind.AUTHOR -> it.copy(
                    overlayVisible = false,
                    accountMenuVisible = false,
                    authorVideos = it.authorVideos.copy(visible = true),
                    search = it.search.copy(visible = false),
                )
            }
        }
        return true
    }

    fun consumeSidebarPlaybackReturn() {
        _state.update {
            if (it.sidebarPlaybackReturn == null) {
                it
            } else {
                it.copy(sidebarPlaybackReturn = null)
            }
        }
    }

    fun returnToRecommendation() {
        val saved = _state.value.recommendationReturn
        if (saved == null) {
            _state.update { it.copy(overlayVisible = true) }
            loadShorts(reset = true)
            return
        }
        _state.update {
            it.copy(
                overlayVisible = true,
                accountMenuVisible = false,
                authorVideos = it.authorVideos.copy(visible = false),
                search = it.search.copy(visible = false),
                engagement = EngagementUiState(),
                shorts = saved.copy(paused = false, error = null),
                recommendationReturn = null,
                sidebarPlaybackReturn = null,
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
            sidebarReturn = authorSidebarVideoPlaybackReturn(video.id),
        )
    }

    fun selectAuthorTab(index: Int) {
        when (index) {
            1 -> if (!_state.value.authorVideos.collectionsLoading) {
                loadAuthorCollectionsPage(reset = true, keepContent = true)
            }
        }
    }

    fun retryAuthorLikes() {
        loadAuthorLikes(reset = true)
    }

    fun loadMoreAuthorLikes() {
        loadAuthorLikes(reset = false)
    }

    fun playAuthorLike(video: ShortVideo) {
        playCollectionVideo(
            video = video,
            sourceVideos = _state.value.authorVideos.likes,
            source = ShortsSource.AUTHOR,
        )
    }

    fun retryAuthorCollections() {
        loadAuthorCollectionsPage(reset = true)
    }

    fun loadMoreAuthorCollections() {
        loadAuthorCollectionsPage(reset = false)
    }

    fun playAuthorCollection(collection: VideoCollection) {
        val state = _state.value.authorVideos
        val authorSecUid = state.authorSecUid
        if (authorSecUid.isBlank() || state.openingCollectionId != null) return
        _state.update {
            it.copy(
                authorVideos = it.authorVideos.copy(
                    openingCollectionId = collection.id,
                    collectionsError = null,
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                api.loadCollectionVideos(mixId = collection.id, cursor = 0)
            }
                .onSuccess { page ->
                    val firstVideo = page.videos.firstOrNull()
                    _state.update { current ->
                        if (current.authorVideos.authorSecUid != authorSecUid) {
                            current
                        } else {
                            current.copy(
                                authorVideos = current.authorVideos.copy(
                                    openingCollectionId = null,
                                    collectionsError = if (firstVideo == null) {
                                        "该合集还没有可播放视频"
                                    } else {
                                        null
                                    },
                                ),
                            )
                        }
                    }
                    if (firstVideo != null && _state.value.authorVideos.authorSecUid == authorSecUid) {
                        playCollectionVideo(
                            video = firstVideo,
                            sourceVideos = page.videos,
                            source = ShortsSource.AUTHOR,
                            sidebarReturn = authorSidebarCollectionPlaybackReturn(
                                collection.id,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { current ->
                        if (current.authorVideos.authorSecUid != authorSecUid) {
                            current
                        } else {
                            current.copy(
                                authorVideos = current.authorVideos.copy(
                                    openingCollectionId = null,
                                    collectionsError = error.toDisplayMessage("合集打开失败"),
                                ),
                            )
                        }
                    }
                }
        }
    }

    fun retryAuthorProfile() {
        val authorSecUid = _state.value.authorVideos.authorSecUid
        if (authorSecUid.isNotBlank()) loadAuthorProfile(authorSecUid)
    }

    fun openSearch() {
        _state.update {
            it.copy(
                overlayVisible = false,
                accountMenuVisible = false,
                authorVideos = it.authorVideos.copy(visible = false),
                search = it.search.copy(visible = true, error = null),
                sidebarPlaybackReturn = null,
            )
        }
    }

    fun closeSearch() {
        pendingVoiceOpenQuery = null
        _state.update {
            it.copy(search = it.search.copy(visible = false))
        }
    }

    fun updateSearchQuery(query: String) {
        if (pendingVoiceOpenQuery != null && query.trim() != pendingVoiceOpenQuery) {
            pendingVoiceOpenQuery = null
        }
        _state.update {
            it.copy(search = it.search.copy(query = query.take(80), error = null))
        }
    }

    fun submitSearch() {
        val query = _state.value.search.query.trim()
        if (query.isBlank()) {
            _state.update {
                it.copy(search = it.search.copy(error = "请输入搜索关键词"))
            }
            return
        }
        searchJob?.cancel()
        _state.update {
            it.copy(
                search = it.search.copy(
                    query = query,
                    submittedQuery = query,
                    results = emptyList(),
                    loading = false,
                    loadingMore = false,
                    error = null,
                    cursor = 0,
                    hasMore = false,
                    loaded = false,
                    verificationRequired = false,
                    searchId = "",
                    backtrace = "",
                ),
            )
        }
        loadSearchPage(reset = true)
    }

    fun retrySearch() {
        val query = _state.value.search.submittedQuery
        if (query.isNotBlank()) {
            _state.update {
                it.copy(search = it.search.copy(loading = false, error = null))
            }
            loadSearchPage(reset = true)
        }
    }

    fun loadMoreSearch() {
        loadSearchPage(reset = false)
    }

    fun playSearchResult(video: ShortVideo) {
        playCollectionVideo(
            video = video,
            sourceVideos = _state.value.search.results,
            source = ShortsSource.SEARCH,
        )
    }

    fun recordPlaybackProgress(
        videoId: String,
        authorUid: String,
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
        updatePreviousPlayback(
            videoId = videoId,
            positionMs = resolvedPosition,
            durationMs = durationMs,
        )
        maybeWriteAccountHistory(
            videoId = videoId,
            authorUid = authorUid,
            positionMs = resolvedPosition,
            durationMs = durationMs,
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
                watchLater = if (
                    it.watchLater.videos.any { video -> video.id == videoId }
                ) {
                    it.watchLater.copy(
                        progress = it.watchLater.progress + (videoId to progress),
                    )
                } else {
                    it.watchLater
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
        _state.update {
            it.copy(
                tab = tab,
                overlayVisible = true,
                sidebarPlaybackReturn = null,
            )
        }
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
        lastSessionValidationAtMs = System.currentTimeMillis()
        invalidateAccountSession()
        accountHistoryWriteTimes.clear()
        lastAccountHistoryWriteAtMs = 0L
        resetPreviousPlayback()
        val myHomeVisible = _state.value.accountMenuVisible
        val myHomeTab = _state.value.myHomeTab
        _state.update {
            it.copy(
                loginVisible = false,
                isLoggedIn = true,
                shorts = it.shorts.copy(error = null),
                accountHistory = AccountHistoryUiState(),
                history = progressStore.history(),
            )
        }
        loadShorts(reset = true)
        loadFavorites(reset = true)
        if (myHomeVisible) {
            refreshMyHomeTab(myHomeTab)
        }
    }

    fun showAccountMenu(tab: MyHomeTab = MyHomeTab.PROFILE) {
        val current = _state.value
        _state.update {
            it.copy(
                overlayVisible = true,
                accountMenuVisible = true,
                myHomeTab = tab,
                history = currentHistory(),
                favorites = it.favorites.copy(
                    progress = progressStore.load(it.favorites.videos.map(ShortVideo::id)),
                ),
                watchLater = it.watchLater.copy(
                    progress = progressStore.load(
                        it.watchLater.videos.map(ShortVideo::id),
                    ),
                ),
                authorVideos = it.authorVideos.copy(visible = false),
                sidebarPlaybackReturn = null,
            )
        }
        if (!current.isLoggedIn) return
        refreshMyHomeTab(tab)
    }

    fun closeAccountMenu() {
        _state.update {
            it.copy(
                overlayVisible = false,
                accountMenuVisible = false,
                sidebarPlaybackReturn = null,
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
                    currentHistory()
                } else {
                    it.history
                },
            )
        }
        if (_state.value.isLoggedIn) refreshMyHomeTab(tab)
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

    private fun refreshMyHomeTab(tab: MyHomeTab) {
        when (tab) {
            MyHomeTab.FAVORITES -> loadFavorites(reset = true, keepContent = true)
            MyHomeTab.WATCH_LATER -> loadWatchLater(reset = true, keepContent = true)
            MyHomeTab.PROFILE -> loadSelfProfile(force = true)
            MyHomeTab.HISTORY -> {
                validateSessionIfNeeded()
                loadAccountHistory(reset = true, keepContent = true)
            }
        }
    }

    private fun validateSessionIfNeeded() {
        if (!_state.value.isLoggedIn) return
        val now = System.currentTimeMillis()
        if (now - lastSessionValidationAtMs < SESSION_VALIDATION_INTERVAL_MS) return
        lastSessionValidationAtMs = now
        viewModelScope.launch {
            runCatching { api.loadSelfProfile() }
                .onSuccess { profile ->
                    _state.update {
                        it.copy(
                            myProfile = it.myProfile.copy(
                                profile = profile,
                                error = null,
                            ),
                        )
                    }
                }
                .onFailure { error -> clearInvalidSession(error) }
        }
    }

    fun logout() {
        invalidateAccountSession()
        sessionStore.clear()
        lastSessionValidationAtMs = 0L
        accountHistoryWriteTimes.clear()
        lastAccountHistoryWriteAtMs = 0L
        resetPreviousPlayback()
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
        }
        _state.update {
            it.copy(
                isLoggedIn = false,
                accountMenuVisible = false,
                loginVisible = false,
                favorites = FavoritesUiState(),
                watchLater = WatchLaterUiState(),
                accountHistory = AccountHistoryUiState(),
                history = progressStore.history(),
                follows = FollowUiState(),
                myProfile = MyProfileUiState(),
                authorVideos = AuthorVideosUiState(),
                search = SearchUiState(),
            )
        }
    }

    private fun loadFavorites(reset: Boolean, keepContent: Boolean = false) {
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
                    videos = if (reset && !keepContent) {
                        emptyList()
                    } else {
                        it.favorites.videos
                    },
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
                    if (clearInvalidSession(error)) return@onFailure
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

    private fun loadWatchLater(reset: Boolean, keepContent: Boolean = false) {
        val snapshot = _state.value.watchLater
        if (snapshot.loading || snapshot.loadingMore) return
        if (!reset && !snapshot.hasMore) return
        val offset = if (reset) 0L else snapshot.offset
        _state.update {
            it.copy(
                watchLater = it.watchLater.copy(
                    loading = reset,
                    loadingMore = !reset,
                    error = null,
                    videos = if (reset && !keepContent) {
                        emptyList()
                    } else {
                        it.watchLater.videos
                    },
                ),
            )
        }
        viewModelScope.launch {
            runCatching { api.loadWatchLaterVideos(offset = offset) }
                .onSuccess { page ->
                    _state.update { state ->
                        val existing = if (reset) {
                            emptyList()
                        } else {
                            state.watchLater.videos
                        }
                        val merged = (existing + page.videos)
                            .distinctBy(ShortVideo::id)
                        state.copy(
                            watchLater = state.watchLater.copy(
                                videos = merged,
                                loading = false,
                                loaded = true,
                                loadingMore = false,
                                error = null,
                                offset = page.cursor,
                                hasMore = page.hasMore,
                                progress = progressStore.load(merged.map(ShortVideo::id)),
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    if (clearInvalidSession(error)) return@onFailure
                    _state.update {
                        it.copy(
                            watchLater = it.watchLater.copy(
                                loading = false,
                                loadingMore = false,
                                error = error.toDisplayMessage("稍后再看加载失败"),
                            ),
                        )
                    }
                }
        }
    }

    private fun loadAccountHistory(reset: Boolean, keepContent: Boolean = false) {
        if (!_state.value.isLoggedIn) return
        val snapshot = _state.value.accountHistory
        if (snapshot.loading || snapshot.loadingMore) return
        if (!reset && !snapshot.hasMore) return
        val cursor = if (reset) 0L else snapshot.cursor
        val generation = accountHistoryGeneration
        _state.update {
            it.copy(
                accountHistory = it.accountHistory.copy(
                    loading = reset,
                    loadingMore = !reset,
                    error = null,
                    videos = if (reset && !keepContent) {
                        emptyList()
                    } else {
                        it.accountHistory.videos
                    },
                ),
            )
        }
        accountHistoryJob = viewModelScope.launch {
            try {
                val page = api.loadAccountHistory(cursor = cursor)
                if (
                    generation != accountHistoryGeneration ||
                    !_state.value.isLoggedIn
                ) {
                    return@launch
                }
                _state.update { state ->
                    val existing = if (reset) {
                        emptyList()
                    } else {
                        state.accountHistory.videos
                    }
                    val merged = (existing + page.videos)
                        .distinctBy(ShortVideo::id)
                    state.copy(
                        accountHistory = state.accountHistory.copy(
                            videos = merged,
                            loading = false,
                            loaded = true,
                            loadingMore = false,
                            error = null,
                            cursor = page.cursor,
                            hasMore = page.hasMore &&
                                merged.isNotEmpty() &&
                                page.cursor > cursor,
                        ),
                        history = mergeAccountHistory(
                            progressStore.history(),
                            merged,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (
                    generation != accountHistoryGeneration ||
                    !_state.value.isLoggedIn
                ) {
                    return@launch
                }
                if (clearInvalidSession(error)) return@launch
                _state.update {
                    it.copy(
                        accountHistory = it.accountHistory.copy(
                            loading = false,
                            loadingMore = false,
                            error = error.toDisplayMessage("账号历史加载失败"),
                        ),
                        history = mergeAccountHistory(
                            progressStore.history(),
                            it.accountHistory.videos,
                        ),
                    )
                }
            } finally {
                if (generation == accountHistoryGeneration) {
                    accountHistoryJob = null
                }
            }
        }
    }

    private fun currentHistory(
        account: List<ShortVideo> = _state.value.accountHistory.videos,
    ): List<PlaybackHistoryEntry> =
        mergeAccountHistory(progressStore.history(), account)

    private fun updatePreviousPlayback(
        videoId: String,
        positionMs: Long,
        durationMs: Long,
    ) {
        val previousId = lastProgressVideoId
        if (previousId != null && previousId != videoId) {
            if (previousId == historyWriteChainVideoId) {
                historyWriteChainVideoSeen = if (
                    lastProgressDurationMs > 0L &&
                    lastProgressPositionMs * 100L >= lastProgressDurationMs * 95L
                ) {
                    1
                } else {
                    0
                }
            }
            lastProgressPositionMs = positionMs
        } else if (previousId == videoId) {
            lastProgressPositionMs = maxOf(lastProgressPositionMs, positionMs)
        } else {
            lastProgressPositionMs = positionMs
        }
        lastProgressVideoId = videoId
        lastProgressDurationMs = durationMs
    }

    private fun resetPreviousPlayback() {
        lastProgressVideoId = null
        lastProgressPositionMs = 0L
        lastProgressDurationMs = 0L
        historyWriteChainVideoId = ""
        historyWriteChainVideoSeen = 0
    }

    private fun maybeWriteAccountHistory(
        videoId: String,
        authorUid: String,
        positionMs: Long,
        durationMs: Long,
    ) {
        if (!_state.value.isLoggedIn) return
        if (authorUid.isBlank()) return
        val now = System.currentTimeMillis()
        if (
            !AccountHistoryWritePolicy.shouldWrite(
                positionMs = positionMs,
                durationMs = durationMs,
                nowMs = now,
                lastWriteAtMs = lastAccountHistoryWriteAtMs,
                lastWriteForVideoAtMs = accountHistoryWriteTimes[videoId],
            )
        ) {
            return
        }
        lastAccountHistoryWriteAtMs = now
        accountHistoryWriteTimes[videoId] = now
        val preItemId = historyWriteChainVideoId
        val preItemSeen = historyWriteChainVideoSeen
        if (accountHistoryWriteTimes.size > ACCOUNT_HISTORY_WRITE_CACHE_LIMIT) {
            val iterator = accountHistoryWriteTimes.entries.iterator()
            while (iterator.hasNext()) {
                if (now - iterator.next().value >=
                    AccountHistoryWritePolicy.VIDEO_COOLDOWN_MS
                ) {
                    iterator.remove()
                }
            }
        }
        viewModelScope.launch {
            runCatching {
                api.writeAccountHistory(
                    awemeId = videoId,
                    authorId = authorUid,
                    preItemId = preItemId,
                    preItemSeen = preItemSeen,
                )
            }
                .onSuccess {
                    historyWriteChainVideoId = videoId
                    historyWriteChainVideoSeen = 0
                    Log.d(TAG, "account history write ok videoId=$videoId")
                }
                .onFailure { error ->
                    if (clearInvalidSession(error)) return@onFailure
                    Log.w(
                        TAG,
                        "account history write failed videoId=$videoId: ${error.message}",
                    )
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
                loadSelfVideos(secUid = secUid, reset = true, keepContent = true)
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
                    val keepVideos = _state.value.myProfile.videos.isNotEmpty()
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
                        loadSelfVideos(
                            secUid = result.secUid,
                            reset = true,
                            keepContent = keepVideos,
                        )
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
                    if (clearInvalidSession(error)) return@onFailure
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
        keepContent: Boolean = false,
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
                    videos = if (reset && !keepContent) {
                        emptyList()
                    } else {
                        it.myProfile.videos
                    },
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
                            val authorKey = profile.secUid.ifBlank { profile.uid }
                            it.copy(
                                follows = if (
                                    authorKey.isBlank() ||
                                    authorKey in it.follows.pendingAuthors
                                ) {
                                    it.follows
                                } else {
                                    it.follows.copy(
                                        statusByAuthor = it.follows.statusByAuthor +
                                            (authorKey to (profile.followStatus > 0)),
                                    )
                                },
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

    private fun loadAuthorVideos(reset: Boolean, keepContent: Boolean = false) {
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
                    videos = if (reset && !keepContent) {
                        emptyList()
                    } else {
                        it.authorVideos.videos
                    },
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

    private fun loadAuthorLikes(reset: Boolean) {
        val snapshot = _state.value.authorVideos
        val authorSecUid = snapshot.authorSecUid
        if (authorSecUid.isBlank()) return
        if (snapshot.likesLoading || snapshot.likesLoadingMore) return
        if (!reset && !snapshot.likesHasMore) return
        val cursor = if (reset) 0L else snapshot.likesCursor
        _state.update {
            it.copy(
                authorVideos = it.authorVideos.copy(
                    likesLoading = reset,
                    likesLoadingMore = !reset,
                    likesError = null,
                    likes = if (reset) emptyList() else it.authorVideos.likes,
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                api.loadAuthorLikedVideos(authorSecUid = authorSecUid, cursor = cursor)
            }
                .onSuccess { page ->
                    _state.update { state ->
                        if (state.authorVideos.authorSecUid != authorSecUid) {
                            state
                        } else {
                            val existing = if (reset) emptyList() else state.authorVideos.likes
                            val merged = (existing + page.videos)
                                .distinctBy(ShortVideo::id)
                            state.copy(
                                authorVideos = state.authorVideos.copy(
                                    likes = merged,
                                    likesLoading = false,
                                    likesLoaded = true,
                                    likesLoadingMore = false,
                                    likesError = null,
                                    likesCursor = page.cursor,
                                    likesHasMore = page.hasMore,
                                    likesProgress = progressStore.load(
                                        merged.map(ShortVideo::id),
                                    ),
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
                                    likesLoading = false,
                                    likesLoadingMore = false,
                                    likesError = error.toDisplayMessage("博主喜欢列表加载失败"),
                                ),
                            )
                        }
                    }
                }
        }
    }

    private fun loadAuthorCollectionsPage(
        reset: Boolean,
        keepContent: Boolean = false,
    ) {
        val snapshot = _state.value.authorVideos
        val authorSecUid = snapshot.authorSecUid
        if (authorSecUid.isBlank()) return
        if (snapshot.collectionsLoading || snapshot.collectionsLoadingMore) return
        if (!reset && !snapshot.collectionsHasMore) return
        val cursor = if (reset) 0L else snapshot.collectionsCursor
        _state.update {
            it.copy(
                authorVideos = it.authorVideos.copy(
                    collectionsLoading = reset,
                    collectionsLoadingMore = !reset,
                    collectionsError = null,
                    collections = if (reset && !keepContent) {
                        emptyList()
                    } else {
                        it.authorVideos.collections
                    },
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                api.loadAuthorCollections(authorSecUid = authorSecUid, cursor = cursor)
            }
                .onSuccess { page ->
                    _state.update { state ->
                        if (state.authorVideos.authorSecUid != authorSecUid) {
                            state
                        } else {
                            val existing = if (reset) {
                                emptyList()
                            } else {
                                state.authorVideos.collections
                            }
                            val merged = (existing + page.collections)
                                .distinctBy(VideoCollection::id)
                            state.copy(
                                authorVideos = state.authorVideos.copy(
                                    collections = merged,
                                    collectionsLoading = false,
                                    collectionsLoaded = true,
                                    collectionsLoadingMore = false,
                                    collectionsError = null,
                                    collectionsCursor = page.cursor,
                                    collectionsHasMore = page.hasMore,
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
                                    collectionsLoading = false,
                                    collectionsLoadingMore = false,
                                    collectionsError = error.toDisplayMessage("博主合集加载失败"),
                                ),
                            )
                        }
                    }
                }
        }
    }

    private fun loadSearchPage(reset: Boolean) {
        val snapshot = _state.value.search
        val query = snapshot.submittedQuery.trim()
        if (query.isBlank()) return
        if (snapshot.loading || snapshot.loadingMore) return
        if (!reset && (!snapshot.loaded || !snapshot.hasMore)) return
        val cursor = if (reset) 0L else snapshot.cursor
        val searchId = if (reset) "" else snapshot.searchId
        val backtrace = if (reset) "" else snapshot.backtrace
        _state.update {
            it.copy(
                search = it.search.copy(
                    loading = reset,
                    loadingMore = !reset,
                    error = null,
                    results = if (reset) emptyList() else it.search.results,
                    verificationRequired =
                        if (reset) false else it.search.verificationRequired,
                ),
            )
        }
        searchJob = viewModelScope.launch {
            runCatching {
                api.searchVideos(
                    keyword = query,
                    cursor = cursor,
                    searchId = searchId,
                    backtrace = backtrace,
                )
            }
                .onSuccess { page ->
                    _state.update { state ->
                        if (state.search.submittedQuery != query) {
                            state
                        } else {
                            val existing = if (reset) emptyList() else state.search.results
                            val merged = (existing + page.videos)
                                .distinctBy(ShortVideo::id)
                            state.copy(
                                search = state.search.copy(
                                    results = merged,
                                    loading = false,
                                    loadingMore = false,
                                    loaded = true,
                                    error = null,
                                    verificationRequired = page.verificationRequired,
                                    cursor = page.cursor,
                                    hasMore = page.hasMore,
                                    searchId = page.searchId,
                                    backtrace = page.backtrace,
                                ),
                            )
                        }
                    }
                    val latest = _state.value
                    val firstResult = latest.search.results.firstOrNull()
                    if (
                        reset &&
                        !page.verificationRequired &&
                        latest.search.submittedQuery == query &&
                        pendingVoiceOpenQuery == query &&
                        firstResult != null
                    ) {
                        pendingVoiceOpenQuery = null
                        playSearchResult(firstResult)
                    }
                }
                .onFailure { error ->
                    _state.update { state ->
                        if (state.search.submittedQuery != query) {
                            state
                        } else {
                            state.copy(
                                search = state.search.copy(
                                    loading = false,
                                    loadingMore = false,
                                    verificationRequired = false,
                                    error = error.toDisplayMessage("搜索失败"),
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
        sidebarReturn: SidebarPlaybackReturn? = null,
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
                search = it.search.copy(visible = false),
                recommendationReturn = recommendationReturnAfterPlayback(
                    currentShorts = it.shorts,
                    existingReturn = it.recommendationReturn,
                    nextSource = source,
                ),
                sidebarPlaybackReturn = sidebarReturn,
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
                        recommendationReturn = if (reset) {
                            null
                        } else {
                            it.recommendationReturn
                        },
                        sidebarPlaybackReturn = if (reset) {
                            null
                        } else {
                            it.sidebarPlaybackReturn
                        },
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

    private fun clearInvalidSession(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        val authenticationRequired = message.contains("未登录") ||
            message.contains("请先登录") ||
            message.contains("登录已失效") ||
            message.contains("登录失效") ||
            message.contains("重新登录")
        if (!authenticationRequired) return false

        invalidateAccountSession()
        lastSessionValidationAtMs = 0L
        accountHistoryWriteTimes.clear()
        lastAccountHistoryWriteAtMs = 0L
        resetPreviousPlayback()
        sessionStore.clear()
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            _state.update {
                it.copy(
                    isLoggedIn = false,
                    loginVisible = false,
                    favorites = FavoritesUiState(),
                    watchLater = WatchLaterUiState(),
                    accountHistory = AccountHistoryUiState(),
                    history = progressStore.history(),
                    follows = FollowUiState(),
                    myProfile = MyProfileUiState(),
                    authorVideos = AuthorVideosUiState(),
                    search = SearchUiState(),
                )
            }
        }
        return true
    }

    private fun invalidateAccountSession() {
        accountHistoryGeneration += 1
        accountHistoryJob?.cancel()
        accountHistoryJob = null
    }

    private companion object {
        const val SESSION_VALIDATION_INTERVAL_MS = 60_000L
        const val ACCOUNT_HISTORY_WRITE_CACHE_LIMIT = 500
        const val TAG = "DouyinTvHistory"
    }

}
