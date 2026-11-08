package com.dongpiandi.douyintv.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dongpiandi.douyintv.ui.screens.LiveGridScreen
import com.dongpiandi.douyintv.ui.screens.LivePlayerScreen
import com.dongpiandi.douyintv.ui.screens.LoginScreen
import com.dongpiandi.douyintv.ui.screens.ShortsScreen
import com.dongpiandi.douyintv.ui.components.AuthorProfileSidebar
import com.dongpiandi.douyintv.ui.components.MyHomeSidebar
import com.dongpiandi.douyintv.performance.DevicePerformanceProfile
import com.dongpiandi.douyintv.performance.LocalDevicePerformanceProfile
import com.dongpiandi.douyintv.ui.theme.Accent
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.Ink
import com.dongpiandi.douyintv.ui.theme.Panel
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary

@Composable
fun DouyinTvApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()
    val livePlayer = state.livePlayer
    val performanceProfile = remember(context) {
        DevicePerformanceProfile.detect(context)
    }

    CompositionLocalProvider(
        LocalDevicePerformanceProfile provides performanceProfile,
    ) {
        BackHandler {
            when {
                state.loginVisible -> viewModel.closeLogin()
                state.accountMenuVisible -> viewModel.closeAccountMenu()
                state.livePlayer != null -> viewModel.closeLivePlayer()
                state.authorVideos.visible -> viewModel.closeAuthorVideos()
                state.tab == MainTab.SHORTS && state.engagement.visible ->
                    viewModel.closeComments()
                state.tab == MainTab.SHORTS && state.overlayVisible -> viewModel.hideOverlay()
                else -> activity?.finish()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink),
        ) {
        when {
            state.loginVisible -> {
                LoginScreen(
                    isSessionCookie = viewModel::hasSessionCookie,
                    onLoggedIn = viewModel::completeLogin,
                    onClose = viewModel::closeLogin,
                )
            }

            livePlayer != null -> {
                LivePlayerScreen(
                    state = livePlayer,
                    onRetry = viewModel::retryLiveRoom,
                )
            }

            state.tab == MainTab.SHORTS -> {
                ShortsScreen(
                    state = state.shorts,
                    engagement = state.engagement,
                    overlayVisible = state.overlayVisible,
                    sidePanelVisible =
                        state.accountMenuVisible || state.authorVideos.visible,
                    isLoggedIn = state.isLoggedIn,
                    onSelectTab = viewModel::selectTab,
                    onAccountClick = {
                        if (state.isLoggedIn) viewModel.showAccountMenu() else viewModel.openLogin()
                    },
                    onHistoryClick = {
                        if (state.isLoggedIn) {
                            viewModel.showAccountMenu(MyHomeTab.HISTORY)
                        } else {
                            viewModel.openLogin()
                        }
                    },
                    onShowOverlay = viewModel::showOverlay,
                    onHideOverlay = viewModel::hideOverlay,
                    onPrevious = viewModel::previousShort,
                    onNext = viewModel::nextShort,
                    onTogglePause = viewModel::toggleShortPause,
                    onEnded = viewModel::onShortEnded,
                    onPlaybackError = viewModel::onShortPlaybackError,
                    onRefresh = viewModel::refreshShorts,
                    onPrepareEngagement = viewModel::prepareEngagement,
                    onOpenComments = viewModel::openComments,
                    onCloseComments = viewModel::closeComments,
                    onLoadMoreComments = viewModel::loadMoreComments,
                    onRetryComments = viewModel::retryComments,
                    favoriteIds = state.favorites.ids,
                    favoritePendingIds = state.favorites.pendingIds,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenAuthor = viewModel::openAuthorVideos,
                    onPlaybackProgress = viewModel::recordPlaybackProgress,
                )
            }

            else -> {
                LiveGridScreen(
                    state = state.live,
                    isLoggedIn = state.isLoggedIn,
                    onSelectTab = viewModel::selectTab,
                    onAccountClick = {
                        if (state.isLoggedIn) viewModel.showAccountMenu() else viewModel.openLogin()
                    },
                    onHistoryClick = {
                        if (state.isLoggedIn) {
                            viewModel.showAccountMenu(MyHomeTab.HISTORY)
                        } else {
                            viewModel.openLogin()
                        }
                    },
                    onOpenRoom = viewModel::openLiveRoom,
                    onFocusRoom = viewModel::rememberLiveFocus,
                    onFocusRestored = viewModel::consumeLiveFocusRestore,
                    onRefresh = { viewModel.loadLiveRooms(reset = false) },
                )
            }
        }

            if (state.accountMenuVisible) {
            MyHomeSidebar(
                selectedTab = state.myHomeTab,
                profile = state.myProfile,
                history = state.history,
                favorites = state.favorites.videos,
                favoriteProgress = state.favorites.progress,
                favoritesLoading = state.favorites.loading,
                favoritesLoadingMore = state.favorites.loadingMore,
                favoritesError = state.favorites.error,
                favoritesHasMore = state.favorites.hasMore,
                onTabSelected = viewModel::selectMyHomeTab,
                onClose = viewModel::closeAccountMenu,
                onRetryProfile = viewModel::retryMyProfile,
                onRetryFavorites = viewModel::retryFavorites,
                onLoadMoreFavorites = viewModel::loadMoreFavorites,
                onPlayHistory = viewModel::playHistoryVideo,
                onPlayFavorite = viewModel::playFavorite,
                onLogout = viewModel::logout,
            )
        }

            if (state.authorVideos.visible) {
            AuthorProfileSidebar(
                profile = state.authorVideos.profile,
                fallbackName = state.authorVideos.authorName,
                fallbackAvatarUrl = state.authorVideos.authorAvatarUrl,
                profileLoading = state.authorVideos.profileLoading,
                profileError = state.authorVideos.profileError,
                videos = state.authorVideos.videos,
                progressByVideoId = state.authorVideos.progress,
                loading = state.authorVideos.loading,
                loadingMore = state.authorVideos.loadingMore,
                error = state.authorVideos.error,
                hasMore = state.authorVideos.hasMore,
                onClose = viewModel::closeAuthorVideos,
                onRetryProfile = viewModel::retryAuthorProfile,
                onRetryVideos = viewModel::retryAuthorVideos,
                onLoadMore = viewModel::loadMoreAuthorVideos,
                onPlay = viewModel::playAuthorVideo,
            )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
