package com.dongpiandi.douyintv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dongpiandi.douyintv.ui.MainTab
import com.dongpiandi.douyintv.ui.theme.Accent
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.Panel
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary

@Composable
fun TopChrome(
    selectedTab: MainTab,
    isLoggedIn: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onAccountClick: () -> Unit,
    initialFocusRequester: FocusRequester? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent),
                ),
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvPill(
                label = "推荐",
                selected = selectedTab == MainTab.SHORTS,
                focusRequester = initialFocusRequester,
                onClick = { onSelectTab(MainTab.SHORTS) },
            )
            TvPill(
                label = "直播",
                selected = selectedTab == MainTab.LIVE,
                onClick = { onSelectTab(MainTab.LIVE) },
            )
        }
        Spacer(Modifier.weight(1f))
        TvPill(
            label = if (isLoggedIn) "账号 · 已登录" else "扫码登录",
            selected = false,
            accent = true,
            onClick = onAccountClick,
        )
    }
}

@Composable
private fun TvPill(
    label: String,
    selected: Boolean,
    accent: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        when {
            focused -> FocusRing
            selected -> Accent
            else -> Panel.copy(alpha = 0.92f)
        },
        label = "pillBackground",
    )
    val foreground by animateColorAsState(
        when {
            focused -> Color(0xFF17120B)
            selected || accent -> Color.White
            else -> TextSecondary
        },
        label = "pillForeground",
    )

    Surface(
        modifier = Modifier
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = background,
        border = if (focused) BorderStroke(2.dp, FocusRing) else null,
        shadowElevation = if (focused) 8.dp else 0.dp,
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 9.dp)) {
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
fun HudHint(text: String) {
    Surface(
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
