package com.dongpiandi.douyintv.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dongpiandi.douyintv.model.ShortComment
import com.dongpiandi.douyintv.ui.formatCount
import com.dongpiandi.douyintv.ui.theme.Accent
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.Panel
import com.dongpiandi.douyintv.ui.theme.PanelRaised
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

internal fun commentsPanelWidth(screenWidth: Dp): Dp = if (screenWidth < 1_100.dp) {
    screenWidth * 0.55f
} else {
    (screenWidth * 0.4f).coerceAtMost(820.dp)
}

@Composable
fun CommentsPanel(
    videoId: String,
    comments: List<ShortComment>,
    total: Long,
    loading: Boolean,
    loadingMore: Boolean,
    error: String?,
    hasMore: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val requesters = remember(videoId) { mutableMapOf<String, FocusRequester>() }
    val listState = rememberLazyListState()
    var initialFocusHandled by remember(videoId) { mutableStateOf(false) }

    LaunchedEffect(videoId) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(videoId) {
        delay(60)
        if (!initialFocusHandled) {
            closeFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(videoId, loading, comments.firstOrNull()?.id, error) {
        if (initialFocusHandled) return@LaunchedEffect
        when {
            !loading && comments.isNotEmpty() -> {
                delay(80)
                requesters[comments.first().id]?.requestFocus()
                initialFocusHandled = true
            }
            !loading && comments.isEmpty() && error != null -> {
                delay(80)
                retryFocusRequester.requestFocus()
                initialFocusHandled = true
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f)),
    ) {
        val panelWidth = commentsPanelWidth(maxWidth)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.12f)),
            )
            Column(
                modifier = Modifier
                    .width(panelWidth)
                    .fillMaxHeight()
                    .background(Color(0xF211141C))
                    .padding(horizontal = 22.dp, vertical = 18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "评论 · ${formatCount(total)}",
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    CloseButton(
                        focusRequester = closeFocusRequester,
                        onClick = onClose,
                    )
                }
                Spacer(Modifier.height(14.dp))

                when {
                    loading && comments.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = FocusRing)
                        }
                    }

                    comments.isEmpty() && error != null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = error,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(Modifier.height(18.dp))
                            PanelAction(
                                label = "重试",
                                focusRequester = retryFocusRequester,
                                onClick = {
                                    initialFocusHandled = false
                                    onRetry()
                                },
                            )
                        }
                    }

                    comments.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "暂无评论",
                                color = TextSecondary,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = comments,
                                key = ShortComment::id,
                            ) { comment ->
                                val focusRequester = remember(comment.id) {
                                    FocusRequester().also {
                                        requesters[comment.id] = it
                                    }
                                }
                                CommentRow(
                                    comment = comment,
                                    focusRequester = focusRequester,
                                )
                            }

                            if (error != null) {
                                item(key = "comment-error") {
                                    CommentErrorRow(
                                        message = error,
                                        onRetry = onRetry,
                                    )
                                }
                            }

                            if (hasMore) {
                                item(key = "comment-load-more") {
                                    LaunchedEffect(comments.size, hasMore) {
                                        if (!loadingMore) onLoadMore()
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 18.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (loadingMore) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = FocusRing,
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
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CommentRow(
    comment: ShortComment,
    focusRequester: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(focused) {
        if (focused) {
            delay(40)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable(),
        color = if (focused) PanelRaised else Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(14.dp),
        border = if (focused) BorderStroke(2.dp, FocusRing) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (comment.avatarUrl != null) {
                AsyncImage(
                    model = comment.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Accent.copy(alpha = 0.28f)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.nickname,
                        modifier = Modifier.weight(1f, fill = false),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (comment.ipLabel.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = comment.ipLabel,
                            color = TextSecondary.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = comment.text,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                if (comment.likeCount > 0 || comment.replyCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (comment.likeCount > 0) {
                            Text(
                                text = "♥ ${formatCount(comment.likeCount)}",
                                color = TextSecondary.copy(alpha = 0.76f),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        if (comment.replyCount > 0) {
                            Text(
                                text = "${formatCount(comment.replyCount)} 回复",
                                color = TextSecondary.copy(alpha = 0.76f),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentErrorRow(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = Accent,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        PanelAction(
            label = "重试",
            compact = true,
            onClick = onRetry,
        )
    }
}

@Composable
private fun CloseButton(
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .tvClickable(onClick),
        color = if (focused) FocusRing else PanelRaised,
        shape = CircleShape,
        border = if (focused) BorderStroke(2.dp, FocusRing) else null,
    ) {
        Text(
            text = "×",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = if (focused) Color(0xFF17120B) else TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PanelAction(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
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
            .tvClickable(onClick),
        color = if (focused) FocusRing else PanelRaised,
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            2.dp,
            if (focused) FocusRing else Color.White.copy(alpha = 0.12f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = if (compact) 14.dp else 24.dp,
                vertical = if (compact) 7.dp else 11.dp,
            ),
            color = if (focused) Color(0xFF17120B) else TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Modifier.tvClickable(onClick: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        if (
            (event.key == Key.DirectionCenter || event.key == Key.Enter) &&
            event.type == KeyEventType.KeyDown
        ) {
            if (event.nativeKeyEvent.repeatCount == 0) onClick()
            true
        } else {
            false
        }
    }
