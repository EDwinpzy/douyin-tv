package com.dongpiandi.douyintv.ui

import com.dongpiandi.douyintv.model.DanmakuMessage

internal fun buildDanmakuTimeline(
    official: List<DanmakuMessage>,
    durationMs: Long,
): List<DanmakuMessage> {
    val maxOffsetMs = durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
    val seenIds = mutableSetOf<String>()
    val seenSlots = mutableSetOf<String>()
    return official.asSequence()
        .filter { message ->
            message.text.isNotBlank() &&
                message.offsetMs in 0L..maxOffsetMs
        }
        .sortedWith(compareBy(DanmakuMessage::offsetMs, DanmakuMessage::id))
        .filter { message ->
            val text = message.text.normalizedDanmakuText()
            seenIds.add(message.id) &&
                seenSlots.add("${message.offsetMs}:$text")
        }
        .toList()
}

private fun String.normalizedDanmakuText(): String =
    replace('\n', ' ')
        .trim()
        .replace(Regex("\\s+"), " ")
