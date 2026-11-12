package com.dongpiandi.douyintv.data

import android.content.Context
import com.dongpiandi.douyintv.model.PlaybackHistoryEntry
import com.dongpiandi.douyintv.model.PlaybackProgress
import com.dongpiandi.douyintv.model.ShortVideo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PlaybackProgressStore(context: Context) {
    private val preferences =
        context.getSharedPreferences("douyin_tv_playback_progress", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()
    private var records: MutableMap<String, StoredProgress>? = null

    fun save(videoId: String, positionMs: Long, durationMs: Long) {
        if (videoId.isBlank()) return
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val all = loadRecords()
            val previous = all[videoId]
            val resolvedDuration = durationMs.takeIf { it > 0L }
                ?: previous?.durationMs
                ?: return
            val resolvedPosition = positionMs.coerceIn(0L, resolvedDuration)
            all[videoId] = StoredProgress(
                positionMs = resolvedPosition,
                durationMs = resolvedDuration,
                updatedAtMs = now,
                video = previous?.video,
            )
            persist(all)
        }
    }

    fun remember(video: ShortVideo) {
        if (video.id.isBlank()) return
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val all = loadRecords()
            val previous = all[video.id]
            val durationMs = previous?.durationMs?.takeIf { it > 0L }
                ?: video.durationMs.coerceAtLeast(0L)
            all[video.id] = StoredProgress(
                positionMs = previous?.positionMs?.coerceIn(0L, durationMs) ?: 0L,
                durationMs = durationMs,
                updatedAtMs = now,
                video = StoredVideo.from(video),
            )
            persist(all)
        }
    }

    fun load(videoIds: Collection<String>): Map<String, PlaybackProgress> {
        if (videoIds.isEmpty()) return emptyMap()
        synchronized(lock) {
            val all = loadRecords()
            return videoIds.distinct().mapNotNull { videoId ->
                all[videoId]?.let { stored ->
                    videoId to PlaybackProgress(
                        positionMs = stored.positionMs,
                        durationMs = stored.durationMs,
                        updatedAtMs = stored.updatedAtMs,
                    )
                }
            }.toMap()
        }
    }

    fun history(limit: Int = HISTORY_RECORDS): List<PlaybackHistoryEntry> {
        synchronized(lock) {
            return loadRecords()
                .values
                .asSequence()
                .mapNotNull { stored ->
                    stored.video?.toShortVideo()?.let { video ->
                        PlaybackHistoryEntry(
                            video = video,
                            progress = PlaybackProgress(
                                positionMs = stored.positionMs,
                                durationMs = stored.durationMs,
                                updatedAtMs = stored.updatedAtMs,
                            ),
                        )
                    }
                }
                .sortedByDescending { it.progress.updatedAtMs }
                .take(limit.coerceIn(1, HISTORY_RECORDS))
                .toList()
        }
    }

    private fun persist(all: MutableMap<String, StoredProgress>) {
        if (all.size > MAX_RECORDS) {
            all.entries
                .sortedBy { it.value.updatedAtMs }
                .take(all.size - MAX_RECORDS)
                .map(Map.Entry<String, StoredProgress>::key)
                .forEach(all::remove)
        }
        preferences.edit()
            .putString(
                KEY_PROGRESS,
                json.encodeToString(
                    StoredProgressMap.serializer(),
                    StoredProgressMap(all),
                ),
            )
            .apply()
    }

    private fun loadRecords(): MutableMap<String, StoredProgress> {
        records?.let { return it }
        val stored = runCatching {
            preferences.getString(KEY_PROGRESS, null)
                ?.let {
                    json.decodeFromString(StoredProgressMap.serializer(), it).values
                }
        }.getOrNull().orEmpty()
        return stored.toMutableMap().also { records = it }
    }

    @Serializable
    private data class StoredProgress(
        val positionMs: Long,
        val durationMs: Long,
        val updatedAtMs: Long,
        val video: StoredVideo? = null,
    )

    @Serializable
    private data class StoredVideo(
        val id: String,
        val description: String = "",
        val authorName: String = "",
        val authorSecUid: String = "",
        val authorAvatarUrl: String? = null,
        val playUrls: List<String> = emptyList(),
        val coverUrl: String? = null,
        val videoWidth: Int = 0,
        val videoHeight: Int = 0,
        val durationMs: Long = 0,
        val likeCount: Long = 0,
        val commentCount: Long = 0,
        val authorUid: String = "",
        val authorFollowStatus: Int = 0,
    ) {
        fun toShortVideo(): ShortVideo? {
            if (id.isBlank() || playUrls.isEmpty()) return null
            return ShortVideo(
                id = id,
                description = description,
                authorName = authorName,
                authorSecUid = authorSecUid,
                authorAvatarUrl = authorAvatarUrl,
                playUrls = playUrls,
                coverUrl = coverUrl,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                durationMs = durationMs,
                likeCount = likeCount,
                commentCount = commentCount,
                authorUid = authorUid,
                authorFollowStatus = authorFollowStatus,
            )
        }

        companion object {
            fun from(video: ShortVideo): StoredVideo = StoredVideo(
                id = video.id,
                description = video.description,
                authorName = video.authorName,
                authorSecUid = video.authorSecUid,
                authorAvatarUrl = video.authorAvatarUrl,
                playUrls = video.playUrls,
                coverUrl = video.coverUrl,
                videoWidth = video.videoWidth,
                videoHeight = video.videoHeight,
                durationMs = video.durationMs,
                likeCount = video.likeCount,
                commentCount = video.commentCount,
                authorUid = video.authorUid,
                authorFollowStatus = video.authorFollowStatus,
            )
        }
    }

    @Serializable
    private data class StoredProgressMap(
        val values: Map<String, StoredProgress> = emptyMap(),
    )

    private companion object {
        const val KEY_PROGRESS = "progress"
        const val MAX_RECORDS = 400
        const val HISTORY_RECORDS = 100
    }
}
