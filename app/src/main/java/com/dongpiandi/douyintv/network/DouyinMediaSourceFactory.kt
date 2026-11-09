@file:androidx.media3.common.util.UnstableApi

package com.dongpiandi.douyintv.network

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DouyinMediaSourceFactory {
    fun create(
        context: Context,
        referer: String,
        cookieHeader: String? = null,
        cacheMedia: Boolean = false,
    ): DefaultMediaSourceFactory {
        val baseFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(DouyinApi.USER_AGENT)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to referer,
                    "Accept" to "*/*",
                ),
            )
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
        val upstreamFactory = cookieHeader
            ?.takeIf(String::isNotBlank)
            ?.let { cookie ->
                ResolvingDataSource.Factory(
                    baseFactory,
                    SessionMediaResolver(referer = referer, cookieHeader = cookie),
                )
            }
            ?: baseFactory
        val dataSourceFactory = if (cacheMedia) {
            CacheDataSource.Factory()
                .setCache(MediaCache.get(context))
                .setUpstreamDataSourceFactory(upstreamFactory)
        } else {
            upstreamFactory
        }
        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
    }
}

private object MediaCache {
    @Volatile
    private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache =
        cache ?: synchronized(this) {
            cache ?: SimpleCache(
                File(context.cacheDir, "short-video-cache"),
                LeastRecentlyUsedCacheEvictor(128L * 1024L * 1024L),
                StandaloneDatabaseProvider(context.applicationContext),
            ).also { cache = it }
        }
}

internal fun requiresSessionMediaResolution(url: HttpUrl): Boolean {
    val host = url.host.lowercase()
    val trustedHost = host == "douyin.com" ||
        host.endsWith(".douyin.com") ||
        host == "iesdouyin.com" ||
        host.endsWith(".iesdouyin.com")
    return trustedHost &&
        (
            url.encodedPath.startsWith("/aweme/v1/play/") ||
                url.encodedPath.startsWith("/aweme/v1/playwm/")
            )
}

internal fun shouldAttachSessionCookie(current: HttpUrl, originHost: String): Boolean =
    current.host.equals(originHost, ignoreCase = true) ||
        current.host.endsWith(".douyin.com", ignoreCase = true) ||
        current.host.equals("douyin.com", ignoreCase = true) ||
        current.host.endsWith(".iesdouyin.com", ignoreCase = true) ||
        current.host.equals("iesdouyin.com", ignoreCase = true)

private class SessionMediaResolver(
    private val referer: String,
    private val cookieHeader: String,
) : ResolvingDataSource.Resolver {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    private val cache = ConcurrentHashMap<String, CachedResolution>()

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val original = dataSpec.uri.toString().toHttpUrlOrNull() ?: return dataSpec
        if (!requiresSessionMediaResolution(original)) return dataSpec
        val cacheKey = original.toString()
        val now = System.currentTimeMillis()
        val cached = cache[cacheKey]?.takeIf { it.expiresAtMs > now }
        if (cached != null) return dataSpec.withResolution(cached)
        val resolution = resolve(original) ?: return dataSpec
        if (resolution.cacheable) cache[cacheKey] = resolution
        return dataSpec.withResolution(resolution)
    }

    private fun resolve(original: HttpUrl): CachedResolution? {
        var current = original
        var redirects = 0
        while (redirects++ <= MAX_REDIRECTS) {
            val builder = Request.Builder()
                .url(current)
                .header("User-Agent", DouyinApi.USER_AGENT)
                .header("Referer", referer)
                .header("Accept", "*/*")
                .header("Range", "bytes=0-0")
            if (shouldAttachSessionCookie(current, original.host)) {
                builder.header("Cookie", cookieHeader)
            }
            val response = try {
                client.newCall(builder.build()).execute()
            } catch (_: IOException) {
                return null
            }
            response.use {
                if (it.code in REDIRECT_CODES) {
                    val location = it.header("Location") ?: return null
                    current = current.resolve(location) ?: return null
                    return@use
                }
                return CachedResolution(
                    url = current.toString(),
                    attachCookie = shouldAttachSessionCookie(current, original.host),
                    expiresAtMs = System.currentTimeMillis() + RESOLUTION_TTL_MS,
                    cacheable = it.isSuccessful,
                )
            }
        }
        return null
    }

    private fun DataSpec.withResolution(resolution: CachedResolution): DataSpec {
        val resolved = withUri(Uri.parse(resolution.url))
        return if (resolution.attachCookie) {
            resolved.withAdditionalHeaders(mapOf("Cookie" to cookieHeader))
        } else {
            resolved
        }
    }

    private data class CachedResolution(
        val url: String,
        val attachCookie: Boolean,
        val expiresAtMs: Long,
        val cacheable: Boolean,
    )

    private companion object {
        const val MAX_REDIRECTS = 5
        const val RESOLUTION_TTL_MS = 2 * 60_000L
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
