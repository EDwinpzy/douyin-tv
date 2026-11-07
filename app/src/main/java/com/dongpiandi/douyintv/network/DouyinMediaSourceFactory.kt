@file:androidx.media3.common.util.UnstableApi

package com.dongpiandi.douyintv.network

import android.content.Context
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

object DouyinMediaSourceFactory {
    fun create(context: Context, referer: String): DefaultMediaSourceFactory {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
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
        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
    }
}
