@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dongpiandi.douyintv.ui.components

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
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dongpiandi.douyintv.R
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
    onUserInteraction: (() -> Unit)? = null,
    showHeaderActions: Boolean = true,
    onSearchClick: (() -> Unit)? = null,
    downFocusRequester: FocusRequester? = null,
    focusEnabled: Boolean = true,
) {
    val fallbackFocusRequester = remember { FocusRequester() }
    val menuFocusRequester = initialFocusRequester ?: fallbackFocusRequester
    val searchIndex = 2
    val accountIndex = 3
    val itemCount = if (showHeaderActions) accountIndex + 1 else 2
    var menuFocused by remember { mutableStateOf(false) }
    var focusedIndex by remember {
        mutableIntStateOf(if (selectedTab == MainTab.SHORTS) 0 else 1)
    }

    LaunchedEffect(selectedTab, itemCount) {
        if (!menuFocused) {
            focusedIndex = if (selectedTab == MainTab.SHORTS) 0 else 1
        } else if (focusedIndex !in 0 until itemCount) {
            focusedIndex = 0
        }
    }

    fun activate(index: Int) {
        when (index) {
            0 -> onSelectTab(MainTab.SHORTS)
            1 -> onSelectTab(MainTab.LIVE)
            searchIndex -> onSearchClick?.invoke()
            accountIndex -> onAccountClick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .focusRequester(menuFocusRequester)
            .focusProperties {
                if (downFocusRequester != null) down = downFocusRequester
            }
            .onFocusChanged {
                menuFocused = it.isFocused
                if (it.isFocused) {
                    focusedIndex = if (selectedTab == MainTab.SHORTS) 0 else 1
                    onUserInteraction?.invoke()
                }
            }
            .onPreviewKeyEvent { event ->
                when (event.key) {
                    Key.DirectionLeft, Key.DirectionRight -> {
                        if (event.type == KeyEventType.KeyDown) {
                            val step = if (event.key == Key.DirectionLeft) -1 else 1
                            focusedIndex =
                                Math.floorMod(focusedIndex + step, itemCount)
                            onUserInteraction?.invoke()
                        }
                        true
                    }

                    Key.DirectionCenter, Key.Enter -> {
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.repeatCount == 0
                        ) {
                            activate(focusedIndex)
                            onUserInteraction?.invoke()
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusable(enabled = focusEnabled)
            .semantics {
                contentDescription = "顶部菜单"
            },
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
                focused = menuFocused && focusedIndex == 0,
            )
            MainNavTab(
                label = "直播",
                selected = selectedTab == MainTab.LIVE,
                focused = menuFocused && focusedIndex == 1,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }

        if (showHeaderActions) {
            HeaderActionStrip(
                modifier = Modifier.align(Alignment.CenterEnd),
                searchFocused = menuFocused && focusedIndex == searchIndex,
                accountFocused = menuFocused && focusedIndex == accountIndex,
            )
        }
    }
}

@Composable
private fun BrandLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics { },
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
    focused: Boolean,
    icon: (@Composable () -> Unit)? = null,
) {
    val foreground = if (selected || focused) Color.White else TextSecondary
    val shape = RoundedCornerShape(50)

    Row(
        modifier = Modifier
            .width(106.dp)
            .height(36.dp)
            .clip(shape)
            .then(
                if (selected || focused) {
                    Modifier.background(
                        when {
                            selected -> Brush.horizontalGradient(
                                listOf(Color(0xFFE51D55), Color(0xFF9E123C)),
                            )
                            else -> Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF5E1731),
                                    Color(0xFF35101F),
                                ),
                            )
                        },
                    )
                } else {
                    Modifier
                },
            )
            .border(
                BorderStroke(
                    width = if (focused) 1.5.dp else 1.dp,
                    color = if (focused) {
                        Color.White.copy(alpha = 0.94f)
                    } else if (selected) {
                        Color.White.copy(alpha = 0.22f)
                    } else {
                        Color.Transparent
                    },
                ),
                shape,
            ),
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
    searchFocused: Boolean,
    accountFocused: Boolean,
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
        HeaderIconItem(
            icon = Icons.Rounded.Search,
            contentDescription = "搜索",
            focused = searchFocused,
        )
        ActionDivider()
        HeaderIconItem(
            icon = Icons.Outlined.Person,
            contentDescription = "我的主页",
            focused = accountFocused,
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
private fun HeaderIconItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    focused: Boolean,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(50))
            .then(
                if (focused) {
                    Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFC51B4C), Color(0xFF72102F)),
                            ),
                        )
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(50),
                        )
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
            tint = if (focused) Color.White else TextPrimary.copy(alpha = 0.9f),
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
