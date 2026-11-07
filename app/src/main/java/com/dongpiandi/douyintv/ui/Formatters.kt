package com.dongpiandi.douyintv.ui

import java.util.Locale

fun formatCount(value: Long): String = when {
    value >= 100_000_000 -> String.format(Locale.CHINA, "%.1f亿", value / 100_000_000.0)
    value >= 10_000 -> String.format(Locale.CHINA, "%.1f万", value / 10_000.0)
    else -> value.toString()
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
