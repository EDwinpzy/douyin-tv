package com.dongpiandi.douyintv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dongpiandi.douyintv.ui.MainTab
import com.dongpiandi.douyintv.ui.theme.Accent
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary

@Composable
fun TopChrome(
    selectedTab: MainTab,
    isLoggedIn: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onAccountClick: () -> Unit,
    initialFocusRequester: FocusRequester? = null,
    commentLabel: String? = null,
    onCommentsClick: (() -> Unit)? = null,
    onUserInteraction: (() -> Unit)? = null,
) {
    val fallbackRecommendFocusRequester = remember { FocusRequester() }
    val recommendFocusRequester =
        initialFocusRequester ?: fallbackRecommendFocusRequester
    val liveFocusRequester = remember { FocusRequester() }
    val commentsFocusRequester = remember { FocusRequester() }
    val accountFocusRequester = remember { FocusRequester() }
    val hasComments = commentLabel != null && onCommentsClick != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassPill(
                label = "推荐",
                selected = selectedTab == MainTab.SHORTS,
                focusRequester = recommendFocusRequester,
                nextFocusRequester = liveFocusRequester,
                onUserInteraction = onUserInteraction,
                onClick = { onSelectTab(MainTab.SHORTS) },
            )
            GlassPill(
                label = "直播",
                selected = selectedTab == MainTab.LIVE,
                focusRequester = liveFocusRequester,
                previousFocusRequester = recommendFocusRequester,
                nextFocusRequester = if (hasComments) {
                    commentsFocusRequester
                } else {
                    accountFocusRequester
                },
                onUserInteraction = onUserInteraction,
                onClick = { onSelectTab(MainTab.LIVE) },
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasComments) {
                GlassPill(
                    label = commentLabel.orEmpty(),
                    selected = false,
                    focusRequester = commentsFocusRequester,
                    previousFocusRequester = liveFocusRequester,
                    nextFocusRequester = accountFocusRequester,
                    onUserInteraction = onUserInteraction,
                    onClick = { onCommentsClick?.invoke() },
                )
            }
            GlassPill(
                label = if (isLoggedIn) "账号 · 已登录" else "扫码登录",
                selected = false,
                focusRequester = accountFocusRequester,
                previousFocusRequester = if (hasComments) {
                    commentsFocusRequester
                } else {
                    liveFocusRequester
                },
                onUserInteraction = onUserInteraction,
                onClick = onAccountClick,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun GlassPill(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester? = null,
    previousFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    onUserInteraction: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val fillTop by animateColorAsState(
        when {
            focused -> Color.White.copy(alpha = 0.96f)
            selected -> Accent.copy(alpha = 0.34f)
            else -> Color.White.copy(alpha = 0.15f)
        },
        label = "pillFillTop",
    )
    val fillBottom by animateColorAsState(
        when {
            focused -> Color.White.copy(alpha = 0.76f)
            selected -> Accent.copy(alpha = 0.16f)
            else -> Color.White.copy(alpha = 0.055f)
        },
        label = "pillFillBottom",
    )
    val borderColor by animateColorAsState(
        when {
            focused -> Color.White.copy(alpha = 0.9f)
            selected -> Accent.copy(alpha = 0.55f)
            else -> Color.White.copy(alpha = 0.24f)
        },
        label = "pillBorder",
    )
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.045f else 1f,
        label = "pillFocusScale",
    )
    val foreground by animateColorAsState(
        when {
            focused -> Color(0xFF0B0D12)
            selected -> TextPrimary
            else -> TextSecondary
        },
        label = "pillForeground",
    )
    val shape = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (focused) 10.dp else 0.dp,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(fillTop, fillBottom),
                ),
            )
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            )
            .focusProperties {
                left = previousFocusRequester ?: FocusRequester.Cancel
                right = nextFocusRequester ?: FocusRequester.Cancel
            }
            .onFocusChanged {
                if (it.isFocused && !focused) onUserInteraction?.invoke()
                focused = it.isFocused
            }
            .onPreviewKeyEvent { event ->
                val isHorizontalKey =
                    event.key == Key.DirectionLeft || event.key == Key.DirectionRight
                when {
                    event.type == KeyEventType.KeyDown -> {
                        onUserInteraction?.invoke()
                        when (event.key) {
                            Key.DirectionLeft -> {
                                previousFocusRequester?.requestFocus()
                                true
                            }

                            Key.DirectionRight -> {
                                nextFocusRequester?.requestFocus()
                                true
                            }

                            Key.DirectionCenter, Key.Enter -> {
                                if (event.nativeKeyEvent.repeatCount == 0) onClick()
                                true
                            }

                            else -> false
                        }
                    }
                    isHorizontalKey || event.key == Key.DirectionCenter || event.key == Key.Enter -> {
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
    ) {
        Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(
                text = label,
                color = foreground,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun HudHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.48f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = TextPrimary.copy(alpha = 0.86f),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
