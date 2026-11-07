package com.dongpiandi.douyintv.network

import com.dongpiandi.douyintv.data.DeviceIdentityStore
import com.dongpiandi.douyintv.data.SessionStore
import com.dongpiandi.douyintv.model.CommentPage
import com.dongpiandi.douyintv.model.DanmakuMessage
import com.dongpiandi.douyintv.model.LivePlayback
import com.dongpiandi.douyintv.model.LiveRoom
import com.dongpiandi.douyintv.model.ShortComment
import com.dongpiandi.douyintv.model.ShortVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class DouyinApi(
    private val identityStore: DeviceIdentityStore,
    private val sessionStore: SessionStore,
    private val signer: AbogusSigner = AbogusSigner(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val ttwidMutex = Mutex()

    suspend fun loadShortVideos(page: Int): List<ShortVideo> = withContext(Dispatchers.IO) {
        val params = commonWebParams()
            .add("count", 20)
            .add("tag_id", "")
            .add("Seo-Flag", 0)
            .add("refresh_index", page.coerceAtLeast(1))
            .add(
                "awemePcRecRawData",
                """{"is_xigua_user":0,"danmaku_switch_status":0,"is_client":false}""",
            )
        val request = requestBuilder(
            url = "$WEB_HOST/aweme/v1/web/channel/feed/?${params.sign(signer)}",
            referer = "$WEB_HOST/discover",
        ).get().build()
        val body = executeText(request)
        val response = json.decodeFromString<ChannelFeedResponse>(body)
        if (response.status_code != 0) {
            throw ApiException(response.status_msg ?: "短视频接口返回 ${response.status_code}")
        }
        response.aweme_list.mapNotNull(::mapVideo)
    }

    suspend fun loadComments(
        awemeId: String,
        cursor: Long = 0,
        count: Int = 50,
    ): CommentPage = withContext(Dispatchers.IO) {
        val params = SignedParams()
            .add("aweme_id", awemeId)
            .add("cursor", cursor)
            .add("count", count.coerceIn(1, 50))
        val request = Request.Builder()
            .url("$IES_HOST/web/api/v2/comment/list/?${params.encoded()}")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "$WEB_HOST/video/$awemeId")
            .get()
            .build()
        val response = json.decodeFromString<MobileCommentListResponse>(executeText(request))
        val statusCode = response.status_code?.statusCode ?: 0
        if (statusCode != 0) {
            val message = response.status_code?.statusMsg.orEmpty()
            throw ApiException(message.ifBlank { "评论接口返回 $statusCode" })
        }
        val comments = response.comments.orEmpty()
            .mapNotNull(::mapComment)
            .distinctBy(ShortComment::id)
        CommentPage(
            comments = comments,
            cursor = 0,
            hasMore = false,
            total = comments.size.toLong(),
        )
    }

    suspend fun loadDanmaku(
        itemId: String,
        durationMs: Long,
    ): List<DanmakuMessage> = withContext(Dispatchers.IO) {
        if (itemId.isBlank() || durationMs <= 0) return@withContext emptyList()
        ensureTtwid()
        val params = commonWebParams()
            .add("item_id", itemId)
            .add("duration", durationMs)
            .add("start_time", 0)
            .add("end_time", (durationMs - 1).coerceAtLeast(0))
            .add("danmaku_switch_status", 1)
        val request = requestBuilder(
            url = "$WEB_HOST/aweme/v1/web/danmaku/get_v2/?${params.sign(signer)}",
            referer = "$WEB_HOST/video/$itemId",
        ).get().build()
        val response = json.decodeFromString<DanmakuListResponse>(executeText(request))
        if (response.status_code != 0) {
            throw ApiException(response.status_msg ?: "弹幕接口返回 ${response.status_code}")
        }
        response.danmaku_list.orEmpty()
            .mapNotNull(::mapDanmaku)
            .sortedBy(DanmakuMessage::offsetMs)
    }

    suspend fun loadLiveRooms(): List<LiveRoom> = withContext(Dispatchers.IO) {
        val params = commonLiveParams()
            .add("request_tag_from", "web")
            .add("need_map", 1)
            .add("liveid", 1)
            .add("is_draw", 1)
            .add("inner_from_drawer", 0)
            .add("custom_count", 50)
            .add("action", "load_more")
            .add("action_type", "loadmore")
            .add("enter_source", "web_homepage_hot_web_live_card")
            .add("source_key", "web_homepage_hot_web_live_card")
            .add("is_ssr", "true")
            .add("maxtime", 0)
        val request = requestBuilder(
            url = "$LIVE_HOST/webcast/feed/?${params.sign(signer)}",
            referer = "$LIVE_HOST/",
        ).get().build()
        val response = json.decodeFromString<LiveFeedResponse>(executeText(request))
        if (response.status_code != 0) {
            throw ApiException("直播列表接口返回 ${response.status_code}")
        }
        response.data.mapNotNull { it.data }.mapNotNull(::mapLiveRoom).distinctBy { it.id }
    }

    suspend fun resolveLiveRoom(room: LiveRoom): LivePlayback = withContext(Dispatchers.IO) {
        val ttwid = ensureTtwid() ?: throw ApiException("无法初始化直播会话")
        val params = commonWebParams()
            .add("app_name", "douyin_web")
            .add("live_id", "1")
            .add("device_platform", "web")
            .add("language", "zh-CN")
            .add("enter_from", "link_share")
            .add("web_rid", room.webRid)
            .add("room_id_str", room.id)
            .add("enter_source", "")
        val request = requestBuilder(
            url = "$LIVE_HOST/webcast/room/web/enter/?${params.sign(signer)}",
            referer = "$LIVE_HOST/${room.webRid}",
            cookieOverride = mergedCookie(ttwid),
        ).get().build()
        val response = json.decodeFromString<LiveEnterResponse>(executeText(request))
        if (response.status_code != 0) {
            throw ApiException("直播取流接口返回 ${response.status_code}")
        }
        val resolved = response.data?.data?.firstOrNull()
            ?: throw ApiException("直播间当前不可用")
        val streamUrl = resolveStreamUrl(resolved.stream_url)
            ?: throw ApiException("直播间没有可用的 HLS 地址")
        LivePlayback(
            roomId = room.id,
            streamUrl = streamUrl,
            title = resolved.title.ifBlank { room.title },
            ownerName = resolved.owner?.nickname.orEmpty().ifBlank { room.ownerName },
        )
    }

    suspend fun refreshTtwid(): String? {
        identityStore.clearTtwid()
        return ensureTtwid()
    }

    private suspend fun ensureTtwid(): String? {
        identityStore.ttwid()?.takeIf { it.isNotBlank() }?.let { return it }
        return ttwidMutex.withLock {
            identityStore.ttwid()?.takeIf { it.isNotBlank() }?.let { return@withLock it }
            val url = "https://ttwid.bytedance.com/ttwid/union/register/" +
                "?aid=6383&service=www.douyin.com"
            val body = """{"aid":6383,"service":"www.douyin.com"}"""
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", WEB_HOST)
                .post(body)
                .build()
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.headers.values("Set-Cookie")
                        .firstNotNullOfOrNull { parseCookieValue(it, "ttwid") }
                        ?.also(identityStore::saveTtwid)
                }
            }.getOrNull()
        }
    }

    private fun commonWebParams(): SignedParams = SignedParams().apply {
        add("device_platform", "webapp")
        add("aid", 6383)
        add("channel", "channel_pc_web")
        add("pc_client_type", 1)
        add("pc_libra_divert", "Windows")
        add("update_version_code", "170400")
        add("support_h265", 1)
        add("support_dash", 1)
        add("version_code", "170400")
        add("version_name", "17.4.0")
        add("cookie_enabled", "true")
        add("screen_width", 1920)
        add("screen_height", 1080)
        add("browser_language", "zh-CN")
        add("browser_platform", "Win32")
        add("browser_name", "Chrome")
        add("browser_version", "131.0.0.0")
        add("browser_online", "true")
        add("engine_name", "Blink")
        add("engine_version", "131.0.0.0")
        add("os_name", "Windows")
        add("os_version", "10")
        add("cpu_core_num", 8)
        add("device_memory", 8)
        add("platform", "PC")
        add("downlink", 10)
        add("effective_type", "4g")
        add("round_trip_time", 0)
        add("webid", identityStore.webId)
        add("uifid", identityStore.uifid)
        add("verifyFp", identityStore.verifyFp)
        add("fp", identityStore.verifyFp)
    }

    private fun commonLiveParams(): SignedParams = SignedParams().apply {
        add("aid", 6383)
        add("app_name", "douyin_web")
        add("live_id", "1")
        add("device_platform", "web")
        add("language", "zh-CN")
        add("enter_from", "link_share")
        add("cookie_enabled", "true")
        add("screen_width", 1920)
        add("screen_height", 1080)
        add("browser_language", "zh-CN")
        add("browser_platform", "Win32")
        add("browser_name", "Chrome")
        add("browser_version", "131.0.0.0")
        add("os_name", "Windows")
        add("os_version", "10")
        add("channel", "channel_pc_web")
        add("webid", identityStore.webId)
        add("uifid", identityStore.uifid)
        add("verifyFp", identityStore.verifyFp)
        add("fp", identityStore.verifyFp)
    }

    private fun requestBuilder(
        url: String,
        referer: String,
        cookieOverride: String? = null,
    ): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", referer)
            .header("x-secsdk-csrf-token", "DOWNGRADE")
        val cookie = cookieOverride ?: mergedCookie(identityStore.ttwid())
        if (cookie.isNotBlank()) builder.header("Cookie", cookie)
        return builder
    }

    private fun mergedCookie(ttwid: String?): String {
        val stored = sessionStore.cookieHeader().orEmpty()
        val entries = linkedMapOf<String, String>()
        parseCookieHeader(stored).forEach { (name, value) -> entries[name] = value }
        if (!ttwid.isNullOrBlank() && "ttwid" !in entries) entries["ttwid"] = ttwid
        if ("UIFID_TEMP" !in entries && "UIFID" !in entries) {
            entries["UIFID_TEMP"] = identityStore.uifid
        }
        if ("s_v_web_id" !in entries) {
            entries["s_v_web_id"] = identityStore.verifyFp
        }
        return entries.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun parseCookieHeader(header: String): List<Pair<String, String>> =
        header.split(';').mapNotNull { part ->
            val value = part.trim()
            val separator = value.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            value.substring(0, separator).trim() to value.substring(separator + 1).trim()
        }

    private fun parseCookieValue(setCookie: String, name: String): String? {
        val first = setCookie.substringBefore(';').trim()
        val separator = first.indexOf('=')
        if (separator <= 0 || first.substring(0, separator) != name) return null
        return first.substring(separator + 1).takeIf { it.isNotBlank() }
    }

    private fun executeText(request: Request): String {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw ApiException("网络请求失败：HTTP ${response.code}")
                }
                if (body.isBlank()) {
                    throw ApiException("接口返回为空，请稍后重试")
                }
                return body
            }
        } catch (error: ApiException) {
            throw error
        } catch (error: IOException) {
            throw ApiException("网络连接失败：${error.message ?: "请检查网络"}")
        }
    }

    private fun mapVideo(dto: AwemeDto): ShortVideo? {
        if (dto.aweme_id.isBlank()) return null
        val playUrls = dto.video?.play_addr?.url_list.orEmpty()
            .map(::forceHttps)
            .filter { it.startsWith("https://") }
            .distinct()
            .sortedBy { url ->
                if (url.startsWith("$WEB_HOST/aweme/v1/play/")) 0 else 1
            }
        if (playUrls.isEmpty()) return null
        return ShortVideo(
            id = dto.aweme_id,
            description = dto.desc.trim(),
            authorName = dto.author?.nickname.orEmpty().trim(),
            authorAvatarUrl = dto.author?.avatar_thumb?.url_list?.firstOrNull()?.let(::forceHttps),
            playUrls = playUrls,
            coverUrl = dto.video?.cover?.url_list?.firstOrNull()?.let(::forceHttps),
            durationMs = dto.video?.duration ?: 0,
            likeCount = dto.statistics?.digg_count ?: 0,
            commentCount = dto.statistics?.comment_count ?: 0,
        )
    }

    private fun mapComment(dto: CommentDto): ShortComment? {
        val text = dto.text.trim()
        if (dto.cid.isBlank() || text.isBlank()) return null
        return ShortComment(
            id = dto.cid,
            text = text,
            nickname = dto.user?.nickname.orEmpty().trim().ifBlank { "抖音用户" },
            avatarUrl = dto.user?.avatar_thumb?.url_list?.firstOrNull()?.let(::forceHttps),
            likeCount = dto.digg_count,
            replyCount = dto.reply_comment_total,
            ipLabel = dto.ip_label.trim(),
        )
    }

    private fun mapDanmaku(dto: DanmakuDto): DanmakuMessage? {
        val text = dto.text.trim()
        if (text.isBlank()) return null
        return DanmakuMessage(
            id = dto.danmaku_id.ifBlank { "${dto.offset_time}-$text" },
            text = text,
            offsetMs = dto.offset_time.coerceAtLeast(0),
        )
    }

    private fun mapLiveRoom(dto: LiveRoomDto): LiveRoom? {
        val webRid = dto.owner?.web_rid.orEmpty()
        if (dto.id_str.isBlank() || webRid.isBlank()) return null
        val viewer = when {
            dto.room_view_stats?.is_hidden == false &&
                dto.room_view_stats.display_short.isNotBlank() ->
                dto.room_view_stats.display_short
            dto.stats?.user_count_str?.isNotBlank() == true -> dto.stats.user_count_str
            else -> dto.stats?.total_user_str.orEmpty()
        }
        return LiveRoom(
            id = dto.id_str,
            webRid = webRid,
            title = dto.title.trim().ifBlank { "抖音直播" },
            ownerName = dto.owner?.nickname.orEmpty().trim(),
            ownerAvatarUrl = dto.owner?.avatar?.firstOrNull()?.let(::forceHttps),
            coverUrl = dto.cover?.url_list?.firstOrNull()?.let(::forceHttps),
            viewerText = viewer,
        )
    }

    private fun resolveStreamUrl(stream: LiveStreamDto?): String? {
        if (stream == null) return null
        val hls = stream.hls_pull_url_map.values.firstOrNull { it.isNotBlank() }
            ?: stream.hls_pull_url.takeIf { it.isNotBlank() }
            ?: stream.flv_pull_url.values.firstOrNull { it.isNotBlank() }
        return hls?.let(::forceHttps)?.takeIf { it.startsWith("https://") }
    }

    private fun forceHttps(url: String): String = when {
        url.startsWith("http://") -> "https://" + url.removePrefix("http://")
        else -> url
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val WEB_HOST = "https://www.douyin.com"
        private const val IES_HOST = "https://www.iesdouyin.com"
        private const val LIVE_HOST = "https://live.douyin.com"
    }
}

class ApiException(message: String) : Exception(message)
