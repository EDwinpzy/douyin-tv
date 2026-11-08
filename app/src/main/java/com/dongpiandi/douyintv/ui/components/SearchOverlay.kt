@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dongpiandi.douyintv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dongpiandi.douyintv.model.ShortVideo
import com.dongpiandi.douyintv.ui.SearchUiState
import com.dongpiandi.douyintv.ui.formatCount
import com.dongpiandi.douyintv.ui.formatDuration
import com.dongpiandi.douyintv.ui.theme.Accent
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.Panel
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SearchOverlay(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (ShortVideo) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val inputFocusRequester = remember { FocusRequester() }
    val submitFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }
    var focusResultsAfterSubmit by remember { mutableStateOf(false) }

    fun submit() {
        keyboard?.hide()
        focusResultsAfterSubmit = true
        onSubmit()
    }

    BackHandler(onBack = onClose)
    DisposableEffect(Unit) {
        onDispose { keyboard?.hide() }
    }
    LaunchedEffect(Unit) {
        delay(120)
        inputFocusRequester.requestFocus()
        keyboard?.show()
    }
    LaunchedEffect(
        state.loading,
        state.results.size,
        state.submittedQuery,
        focusResultsAfterSubmit,
    ) {
        if (focusResultsAfterSubmit && !state.loading) {
            delay(120)
            if (state.results.isNotEmpty()) {
                keyboard?.hide()
                firstResultFocusRequester.requestFocus()
            } else {
                inputFocusRequester.requestFocus()
            }
            focusResultsAfterSubmit = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF20A0D14),
                        Color(0xFA05070B),
                    ),
                ),
            )
            .padding(horizontal = 34.dp, vertical = 22.dp),
    ) {
        val columnCount = if (maxWidth >= 1100.dp) 4 else 3
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "搜索",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "输入关键词后选择“搜索”，按 Back 返回",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchInput(
                    value = state.query,
                    onValueChange = onQueryChange,
                    focusRequester = inputFocusRequester,
                    nextFocusRequester = submitFocusRequester,
                    onShowKeyboard = { keyboard?.show() },
                    onSubmit = ::submit,
                    modifier = Modifier.weight(1f),
                )
                SearchActionButton(
                    label = "搜索",
                    focusRequester = submitFocusRequester,
                    downFocusRequester = firstResultFocusRequester
                        .takeIf { state.results.isNotEmpty() },
                    onClick = ::submit,
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    state.loading && state.results.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Accent)
                        }
                    }

                    state.error != null && state.results.isEmpty() -> {
                        SearchMessagePanel(
                            message = state.error,
                            actionLabel = "重试",
                            onAction = onRetry,
                        )
                    }

                    state.verificationRequired && state.results.isEmpty() -> {
                        SearchMessagePanel(
                            message = "抖音要求安全验证，当前第三方搜索暂不可用。" +
                                "请稍后重试，或在抖音 App、网页中搜索“${state.submittedQuery}”",
                        )
                    }

                    state.loaded &&
                        state.results.isEmpty() &&
                        state.submittedQuery.isNotBlank() -> {
                        SearchMessagePanel(
                            message = "没有找到“${state.submittedQuery}”相关视频",
                        )
                    }

                    state.submittedQuery.isBlank() -> {
                        SearchMessagePanel(
                            message = "搜索抖音上的视频、作者或话题",
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnCount),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(
                                items = state.results,
                                key = ShortVideo::id,
                            ) { video ->
                                val requester = remember(video.id) { FocusRequester() }
                                SearchVideoCard(
                                    video = video,
                                    focusRequester = if (video.id == state.results.first().id) {
                                        firstResultFocusRequester
                                    } else {
                                        requester
                                    },
                                    onClick = { onPlay(video) },
                                )
                            }
                            if (state.error != null) {
                                item(
                                    key = "search-error",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    SearchInlineError(
                                        message = state.error,
                                        onRetry = onRetry,
                                    )
                                }
                            }
                            if (state.hasMore) {
                                item(
                                    key = "search-load-more",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    LaunchedEffect(state.results.size, state.hasMore) {
                                        if (!state.loadingMore) onLoadMore()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (state.loadingMore) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = Accent,
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
private fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    onShowKeyboard: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(13.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(52.dp)
            .focusRequester(focusRequester)
            .focusProperties { down = nextFocusRequester }
            .onPreviewKeyEvent { event ->
                when (event.key) {
                    Key.DirectionDown -> {
                        if (event.type == KeyEventType.KeyDown) {
                            nextFocusRequester.requestFocus()
                        }
                        true
                    }

                    Key.DirectionCenter, Key.Enter -> {
                        if (event.type == KeyEventType.KeyDown) onShowKeyboard()
                        true
                    }

                    else -> false
                }
            },
        singleLine = true,
        textStyle = TextStyle(
            color = TextPrimary,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSubmit() },
        ),
        cursorBrush = SolidColor(Accent),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Panel)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (value.isBlank()) TextSecondary else Accent,
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isBlank()) {
                        Text(
                            text = "输入搜索关键词",
                            color = TextSecondary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                            ),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun SearchActionButton(
    label: String,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .width(108.dp)
            .height(52.dp)
            .clip(shape)
            .background(if (focused) Accent else Panel)
            .border(
                2.dp,
                if (focused) FocusRing else Color.White.copy(alpha = 0.2f),
                shape,
            )
            .focusRequester(focusRequester)
            .focusProperties {
                if (downFocusRequester != null) down = downFocusRequester
            }
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
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (focused) Color.Black else TextPrimary,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                lineHeight = 18.sp,
            ),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SearchVideoCard(
    video: ShortVideo,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(
                shape = shape,
                style = GlassStyle(
                    top = if (focused) Color(0xF03C2231) else Color(0xB81A2030),
                    bottom = if (focused) Color(0xE01D0A13) else Color(0xA60C111B),
                    border = if (focused) {
                        Color.White.copy(alpha = 0.94f)
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    },
                    sheen = Color.White.copy(alpha = if (focused) 0.15f else 0.055f),
                    shadowAlpha = if (focused) 0.3f else 0.12f,
                ),
                elevation = if (focused) 8.dp else 2.dp,
            )
            .focusRequester(focusRequester)
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color(0xFF11151E)),
        ) {
            video.coverUrl?.let { coverUrl ->
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.74f),
                            ),
                        ),
                    ),
            )
            Text(
                text = formatDuration(video.durationMs),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                ),
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = video.description.ifBlank { "抖音短视频" },
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                ),
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${video.authorName.ifBlank { "抖音用户" }}",
                    modifier = Modifier.weight(1f),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = TextSecondary,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(video.likeCount),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SearchMessagePanel(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            SearchInlineAction(label = actionLabel, onClick = onAction)
        }
    }
}

@Composable
private fun SearchInlineError(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = Accent,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
            ),
        )
        SearchInlineAction(label = "重试", onClick = onRetry)
    }
}

@Composable
private fun SearchInlineAction(
    label: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(if (focused) Accent else Panel)
            .border(1.dp, if (focused) FocusRing else Color.White.copy(alpha = 0.2f), shape)
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
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (focused) Color.Black else TextPrimary,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
            ),
            fontWeight = FontWeight.Bold,
        )
    }
}
