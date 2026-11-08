package com.dongpiandi.douyintv.ui

import com.dongpiandi.douyintv.model.DanmakuMessage
import com.dongpiandi.douyintv.model.ShortComment

private const val FILL_STEP_MS = 3_700L
private const val MAX_FILL_DANMAKU = 480
private const val END_GUARD_MS = 800L

internal fun buildDanmakuTimeline(
    official: List<DanmakuMessage>,
    comments: List<ShortComment>,
    durationMs: Long,
): List<DanmakuMessage> {
    val officialMessages = official
        .asSequence()
        .filter { it.text.isNotBlank() }
        .distinctBy(DanmakuMessage::id)
        .sortedBy(DanmakuMessage::offsetMs)
        .toList()
    val texts = comments.asSequence()
        .map { comment -> comment.text.replace('\n', ' ').trim() }
        .filter(String::isNotBlank)
        .distinct()
        .take(12)
        .toList()
    if (texts.isEmpty()) return officialMessages

    val lastOfficialAt = officialMessages.maxOfOrNull(DanmakuMessage::offsetMs)
    val fillStartMs = lastOfficialAt
        ?.let { it + FILL_STEP_MS }
        ?: 600L
    val effectiveDuration = durationMs.takeIf { it > 0L }
        ?: (fillStartMs + FILL_STEP_MS * texts.size * 4)
    val fillEndMs = (effectiveDuration - END_GUARD_MS).coerceAtLeast(fillStartMs)
    if (fillEndMs <= fillStartMs) return officialMessages

    val fillCount = (
        (fillEndMs - fillStartMs) / FILL_STEP_MS + 1L
        )
        .coerceAtMost(MAX_FILL_DANMAKU.toLong())
        .toInt()
    val filler = List(fillCount) { index ->
        val text = texts[index % texts.size]
        DanmakuMessage(
            id = "comment-danmaku-$index-${text.hashCode()}",
            text = text,
            offsetMs = fillStartMs + index * FILL_STEP_MS,
        )
    }
    return officialMessages + filler
}
