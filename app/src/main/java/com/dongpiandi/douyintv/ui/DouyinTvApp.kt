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

    BackHandler {
        when {
            state.loginVisible -> viewModel.closeLogin()
            state.accountMenuVisible -> viewModel.closeAccountMenu()
            state.livePlayer != null -> viewModel.closeLivePlayer()
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
                    isLoggedIn = state.isLoggedIn,
                    onSelectTab = viewModel::selectTab,
                    onAccountClick = {
                        if (state.isLoggedIn) viewModel.showAccountMenu() else viewModel.openLogin()
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
                    onOpenRoom = viewModel::openLiveRoom,
                    onFocusRoom = viewModel::rememberLiveFocus,
                    onFocusRestored = viewModel::consumeLiveFocusRestore,
                    onRefresh = { viewModel.loadLiveRooms(reset = false) },
                )
            }
        }

        if (state.accountMenuVisible) {
            AccountOverlay(
                isLoggedIn = state.isLoggedIn,
                onLogin = viewModel::openLogin,
                onLogout = viewModel::logout,
                onClose = viewModel::closeAccountMenu,
            )
        }
    }
}

@Composable
private fun AccountOverlay(
    isLoggedIn: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit,
) {
    val closeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { closeFocusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(Panel, RoundedCornerShape(26.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isLoggedIn) "账号已登录" else "尚未登录",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isLoggedIn) {
                    "登录凭证已使用 Android Keystore 加密保存在本机"
                } else {
                    "使用抖音 App 扫码登录后，推荐内容会使用你的账号会话"
                },
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isLoggedIn) {
                    AccountButton(
                        label = "退出登录",
                        primary = true,
                        onClick = onLogout,
                    )
                } else {
                    AccountButton(
                        label = "去登录",
                        primary = true,
                        onClick = onLogin,
                    )
                }
                AccountButton(
                    label = "关闭",
                    focusRequester = closeFocusRequester,
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun AccountButton(
    label: String,
    primary: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
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
            else -> Color(0xFF252B39)
        },
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            2.dp,
            if (focused) FocusRing else Color.Transparent,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 13.dp),
            color = if (focused) Color(0xFF17120B) else TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
