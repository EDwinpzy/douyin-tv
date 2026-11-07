package com.dongpiandi.douyintv.ui

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dongpiandi.douyintv.DouyinTvApplication
import com.dongpiandi.douyintv.model.DanmakuMessage
import com.dongpiandi.douyintv.model.LivePlayback
import com.dongpiandi.douyintv.model.LiveRoom
import com.dongpiandi.douyintv.model.ShortComment
import com.dongpiandi.douyintv.model.ShortVideo
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

data class ShortsUiState(
    val videos: List<ShortVideo> = emptyList(),
    val index: Int = 0,
    val page: Int = 0,
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
    val resumeOnClose: Boolean = false,
)

data class AppUiState(
    val tab: MainTab = MainTab.SHORTS,
    val overlayVisible: Boolean = true,
    val accountMenuVisible: Boolean = false,
    val loginVisible: Boolean = false,
    val isLoggedIn: Boolean = false,
    val shorts: ShortsUiState = ShortsUiState(),
    val engagement: EngagementUiState = EngagementUiState(),
    val live: LiveListUiState = LiveListUiState(),
    val livePlayer: LivePlayerUiState? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as DouyinTvApplication).container
    private val sessionStore = container.sessionStore
    private val api = container.api
    private val shortsLoadMutex = Mutex()
    private var engagementJob: Job? = null

    private val _state = MutableStateFlow(
        AppUiState(isLoggedIn = sessionStore.hasSession()),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        loadShorts(reset = true)
    }

    fun showOverlay() {
        _state.update { it.copy(overlayVisible = true) }
    }

    fun hideOverlay() {
        _state.update { it.copy(overlayVisible = false, accountMenuVisible = false) }
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
        engagementJob?.cancel()
        val sameVideo = previous.videoId == video.id
        _state.update {
            it.copy(
                engagement = EngagementUiState(
                    videoId = video.id,
                    visible = sameVideo && previous.visible,
                    loading = true,
                    resumeOnClose = if (sameVideo) previous.resumeOnClose else false,
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
            val visibleDanmaku = officialDanmaku.ifEmpty { commentDanmaku(comments) }
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
        val resumeOnClose = if (current.engagement.visible) {
            current.engagement.resumeOnClose
        } else {
            !current.shorts.paused
        }
        _state.update {
            it.copy(
                shorts = it.shorts.copy(paused = true),
                engagement = it.engagement.copy(
                    visible = true,
                    resumeOnClose = resumeOnClose,
                ),
            )
        }
        if (current.engagement.videoId != video.id && !current.engagement.loading) {
            prepareEngagement(video)
        }
    }

    fun closeComments() {
        _state.update { current ->
            val shouldResume = current.engagement.resumeOnClose
            current.copy(
                shorts = current.shorts.copy(
                    paused = if (shouldResume) false else current.shorts.paused,
                ),
                engagement = current.engagement.copy(
                    visible = false,
                    resumeOnClose = false,
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
            loadShorts(reset = false, advanceAfterLoad = true)
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
    }

    fun showAccountMenu() {
        _state.update { it.copy(accountMenuVisible = true) }
    }

    fun closeAccountMenu() {
        _state.update { it.copy(accountMenuVisible = false) }
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
        if (shorts.videos.isNotEmpty() && shorts.index >= shorts.videos.lastIndex - 4) {
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

    private fun commentDanmaku(comments: List<ShortComment>): List<DanmakuMessage> =
        comments.asSequence()
            .map { comment -> comment.text.replace('\n', ' ').trim() }
            .filter(String::isNotBlank)
            .distinct()
            .take(12)
            .mapIndexed { index, text ->
                DanmakuMessage(
                    id = "comment-danmaku-$index",
                    text = text,
                    offsetMs = 600L + index * 1_200L,
                )
            }
            .toList()
}
