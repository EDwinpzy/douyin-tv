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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dongpiandi.douyintv.R
import com.dongpiandi.douyintv.performance.LocalDevicePerformanceProfile
import com.dongpiandi.douyintv.ui.MainTab
import com.dongpiandi.douyintv.ui.theme.Accent
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary

@Composable
fun TopChrome(
    selectedTab: MainTab,
    isLoggedIn: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onAccountClick: () -> Unit,
    initialFocusRequester: FocusRequester? = null,
    onHistoryClick: (() -> Unit)? = null,
    onUserInteraction: (() -> Unit)? = null,
    onMenuFocusChanged: ((Boolean) -> Unit)? = null,
    showHeaderActions: Boolean = true,
) {
    val fallbackRecommendFocusRequester = remember { FocusRequester() }
    val recommendFocusRequester =
        initialFocusRequester ?: fallbackRecommendFocusRequester
    val liveFocusRequester = remember { FocusRequester() }
    val historyFocusRequester = remember { FocusRequester() }
    val accountFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .onFocusChanged { onMenuFocusChanged?.invoke(it.hasFocus) },
    ) {
        BrandLogo(
            modifier = Modifier.align(Alignment.CenterStart),
        )

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .glassSurface(
                    shape = RoundedCornerShape(50),
                    style = GlassStyle(
                        top = Color(0xC7171C27),
                        bottom = Color(0xA6080A11),
                        border = Color.White.copy(alpha = 0.18f),
                        sheen = Color.White.copy(alpha = 0.12f),
                        shadowAlpha = 0.3f,
                    ),
                    elevation = 8.dp,
                )
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainNavTab(
                label = "推荐",
                selected = selectedTab == MainTab.SHORTS,
                focusRequester = recommendFocusRequester,
                previousFocusRequester = null,
                nextFocusRequester = liveFocusRequester,
                onUserInteraction = onUserInteraction,
                onClick = { onSelectTab(MainTab.SHORTS) },
            )
            MainNavTab(
                label = "直播",
                selected = selectedTab == MainTab.LIVE,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                focusRequester = liveFocusRequester,
                previousFocusRequester = recommendFocusRequester,
                nextFocusRequester = if (onHistoryClick != null) {
                    historyFocusRequester
                } else {
                    accountFocusRequester
                },
                onUserInteraction = onUserInteraction,
                onClick = { onSelectTab(MainTab.LIVE) },
            )
        }

        if (showHeaderActions) {
            HeaderActionStrip(
                modifier = Modifier.align(Alignment.CenterEnd),
                historyFocusRequester = if (onHistoryClick != null) {
                    historyFocusRequester
                } else {
                    null
                },
                accountFocusRequester = accountFocusRequester,
                previousBeforeHistory = liveFocusRequester,
                onHistoryClick = onHistoryClick,
                onAccountClick = onAccountClick,
                onUserInteraction = onUserInteraction,
            )
        }
    }
}

@Composable
private fun BrandLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tiktok_mark),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (-1.2).dp, y = 1.4.dp),
                tint = Color(0xFF25F4EE),
            )
            Icon(
                painter = painterResource(R.drawable.ic_tiktok_mark),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 1.2.dp, y = (-0.7).dp),
                tint = Color(0xFFFE2C55),
            )
            Icon(
                painter = painterResource(R.drawable.ic_tiktok_mark),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = Color.White,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "抖音TV",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun MainNavTab(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    previousFocusRequester: FocusRequester?,
    nextFocusRequester: FocusRequester,
    onUserInteraction: (() -> Unit)?,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val lowTier = LocalDevicePerformanceProfile.current.lowTier
    val scale by animateFloatAsState(
        targetValue = if (focused && !lowTier) 1.025f else 1f,
        animationSpec = if (lowTier) snap() else spring(),
        label = "mainNavScale",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected || focused) Color.White else TextSecondary,
        animationSpec = if (lowTier) snap() else tween(durationMillis = 140),
        label = "mainNavForeground",
    )
    val shape = RoundedCornerShape(50)
    val motionLayer = if (lowTier) {
        Modifier
    } else {
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

    Row(
        modifier = Modifier
            .width(106.dp)
            .height(36.dp)
            .then(motionLayer)
            .then(
                if (!lowTier && selected) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = shape,
                        clip = false,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(
                when {
                    selected -> Brush.horizontalGradient(
                        listOf(Color(0xFFFF4D81), Color(0xFFF12761)),
                    )
                    focused -> Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.08f),
                        ),
                    )
                    else -> Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.Transparent),
                    )
                },
            )
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(
                        alpha = if (selected || focused) 0.16f else 0.055f,
                    ),
                    0.42f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.08f),
                ),
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = if (focused) {
                        Color.White.copy(alpha = 0.76f)
                    } else if (selected) {
                        Color.White.copy(alpha = 0.18f)
                    } else {
                        Color.Transparent
                    },
                ),
                shape,
            )
            .focusRequester(focusRequester)
            .focusProperties {
                left = previousFocusRequester ?: FocusRequester.Cancel
                right = nextFocusRequester
            }
            .onFocusChanged {
                if (it.isFocused && !focused) onUserInteraction?.invoke()
                focused = it.isFocused
            }
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
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides foreground,
            ) {
                icon()
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeaderActionStrip(
    modifier: Modifier = Modifier,
    historyFocusRequester: FocusRequester?,
    accountFocusRequester: FocusRequester,
    previousBeforeHistory: FocusRequester,
    onHistoryClick: (() -> Unit)?,
    onAccountClick: () -> Unit,
    onUserInteraction: (() -> Unit)?,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .glassSurface(
                shape = RoundedCornerShape(50),
                style = GlassStyle(
                    top = Color(0xB81A1E29),
                    bottom = Color(0x94090B11),
                    border = Color.White.copy(alpha = 0.15f),
                    sheen = Color.White.copy(alpha = 0.1f),
                    shadowAlpha = 0.26f,
                ),
                elevation = 7.dp,
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "搜索",
                modifier = Modifier.size(19.dp),
                tint = TextPrimary.copy(alpha = 0.88f),
            )
        }
        ActionDivider()
        HeaderIconButton(
            icon = Icons.Outlined.History,
            contentDescription = "观看历史",
            focusRequester = historyFocusRequester,
            previousFocusRequester = previousBeforeHistory,
            nextFocusRequester = accountFocusRequester,
            onUserInteraction = onUserInteraction,
            onClick = onHistoryClick,
        )
        ActionDivider()
        HeaderIconButton(
            icon = Icons.Outlined.Person,
            contentDescription = "我的主页",
            focusRequester = if (historyFocusRequester != null) {
                accountFocusRequester
            } else {
                accountFocusRequester
            },
            previousFocusRequester = historyFocusRequester ?: previousBeforeHistory,
            nextFocusRequester = null,
            onUserInteraction = onUserInteraction,
            onClick = onAccountClick,
        )
    }
}

@Composable
private fun ActionDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(16.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.13f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    focusRequester: FocusRequester?,
    previousFocusRequester: FocusRequester?,
    nextFocusRequester: FocusRequester?,
    onUserInteraction: (() -> Unit)?,
    onClick: (() -> Unit)?,
) {
    var focused by remember { mutableStateOf(false) }
    val enabled = onClick != null
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (focused) Color.White.copy(alpha = 0.17f) else Color.Transparent,
            )
            .then(
                if (enabled && focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            )
            .focusProperties {
                left = previousFocusRequester ?: FocusRequester.Cancel
                right = nextFocusRequester ?: FocusRequester.Cancel
            }
            .onFocusChanged {
                if (it.isFocused && !focused) onUserInteraction?.invoke()
                focused = it.isFocused
            }
            .then(if (enabled) Modifier.focusable() else Modifier)
            .then(
                if (enabled) {
                    Modifier.onPreviewKeyEvent { event ->
                        if (
                            (event.key == Key.DirectionCenter || event.key == Key.Enter) &&
                            event.type == KeyEventType.KeyDown
                        ) {
                            if (event.nativeKeyEvent.repeatCount == 0) onClick?.invoke()
                            true
                        } else {
                            false
                        }
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp),
            tint = when {
                focused -> FocusRing
                enabled -> TextPrimary
                else -> TextPrimary.copy(alpha = 0.72f)
            },
        )
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
