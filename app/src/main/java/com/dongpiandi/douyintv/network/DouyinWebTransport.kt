package com.dongpiandi.douyintv.network

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dongpiandi.douyintv.data.SessionCookie
import com.dongpiandi.douyintv.data.SessionStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.lang.ref.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class WebWriteResponse(
    val status: Int,
    val body: String,
    val guardResult: String,
)

class DouyinWebTransport(
    context: Context,
    private val sessionStore: SessionStore,
) {
    private val context = context.applicationContext
    private var hostReference: WeakReference<ViewGroup>? = null
    private var attachedParent: ViewGroup? = null
    private val mutex = Mutex()
    private val callbacks = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private var webView: WebView? = null
    private var pageReady: CompletableDeferred<Unit>? = null
    private var pageSessionFingerprint = ""

    private val bridge = object {
        @JavascriptInterface
        fun complete(id: String, payload: String) {
            callbacks.remove(id)?.complete(payload)
        }
    }

    fun attachHost(host: ViewGroup) {
        hostReference = WeakReference(host)
    }

    suspend fun post(
        path: String,
        body: String,
        queryParams: Map<String, String> = emptyMap(),
    ): WebWriteResponse = mutex.withLock {
        val sessionFingerprint = SessionCookie.loginSessionFingerprint(
            sessionStore.cookieHeader().orEmpty(),
        )
        val ready = withContext(Dispatchers.Main.immediate) {
            ensurePage(sessionFingerprint)
        }
        try {
            withTimeout(PAGE_READY_TIMEOUT_MS) {
                ready.await()
            }
        } catch (error: CancellationException) {
            invalidatePage()
            throw error
        } catch (error: Throwable) {
            invalidatePage()
            throw error
        }
        delay(SECURITY_WARMUP_MS)

        val id = UUID.randomUUID().toString()
        val result = CompletableDeferred<String>()
        callbacks[id] = result
        try {
            withContext(Dispatchers.Main.immediate) {
                val view = webView ?: throw ApiException("网页写请求尚未初始化")
                view.evaluateJavascript(
                    officialRequestScript(id, path, body, queryParams),
                    null,
                )
            }
            val raw = withTimeout(REQUEST_TIMEOUT_MS) {
                result.await()
            }
            snapshotUpdatedCookies()
            parseResponse(raw)
        } catch (error: CancellationException) {
            invalidatePage()
            throw error
        } catch (error: Throwable) {
            throw error
        } finally {
            callbacks.remove(id)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensurePage(sessionFingerprint: String): CompletableDeferred<Unit> {
        val existing = webView
        val existingReady = pageReady
        if (
            existing != null &&
            existingReady != null &&
            pageSessionFingerprint == sessionFingerprint
        ) {
            return existingReady
        }

        detachAndDestroy(existing)

        val ready = CompletableDeferred<Unit>()
        pageReady = ready
        pageSessionFingerprint = sessionFingerprint
        syncStoredCookies()
        val view = WebView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            alpha = 0f
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = false
                blockNetworkImage = true
                userAgentString = DouyinApi.USER_AGENT
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = true
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            addJavascriptInterface(bridge, BRIDGE_NAME)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (
                        newProgress >= PAGE_EVENT_FALLBACK_PROGRESS &&
                        webView === view &&
                        !ready.isCompleted
                    ) {
                        Log.d(TAG, "page usable at progress=$newProgress")
                        ready.complete(Unit)
                    }
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    Log.d(TAG, "page started host=${safeHost(url)}")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d(TAG, "page finished host=${safeHost(url)} ready=${ready.isCompleted}")
                    if (isDouyinPage(url) && !ready.isCompleted) {
                        ready.complete(Unit)
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    errorResponse: android.webkit.WebResourceResponse?,
                ) {
                    if (request?.isForMainFrame == true) {
                        Log.w(
                            TAG,
                            "page http error status=${errorResponse?.statusCode} " +
                                "host=${safeHost(request.url.toString())}",
                        )
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?,
                ) {
                    Log.w(
                        TAG,
                        "page error main=${request?.isForMainFrame} " +
                            "code=${error?.errorCode} host=${safeHost(request?.url?.toString())}",
                    )
                    if (request?.isForMainFrame == true && !ready.isCompleted) {
                        ready.completeExceptionally(
                            ApiException("网页写请求初始化失败，请检查网络"),
                        )
                    }
                }
            }
        }
        attachToHost(view)
        view.loadUrl(HOME_URL)
        view.postDelayed(
            {
                if (webView === view && !ready.isCompleted && isDouyinPage(view.url)) {
                    Log.d(TAG, "page usable after load fallback")
                    ready.complete(Unit)
                }
            },
            PAGE_EVENT_FALLBACK_MS,
        )
        webView = view
        return ready
    }

    private fun attachToHost(view: WebView) {
        val parent = hostReference?.get() ?: return
        if (view.parent != null) return
        val params = ViewGroup.LayoutParams(1, 1)
        runCatching { parent.addView(view, params) }
            .onSuccess { attachedParent = parent }
            .onFailure { error ->
                Log.w(TAG, "attach web host failed: ${error.message.orEmpty().take(80)}")
            }
    }

    private fun detachAndDestroy(view: WebView?) {
        if (view == null) return
        view.stopLoading()
        view.loadUrl("about:blank")
        (view.parent as? ViewGroup)?.removeView(view)
        attachedParent = null
        view.destroy()
    }

    private fun syncStoredCookies() {
        val cookieManager = CookieManager.getInstance()
        val cookies = linkedMapOf<String, String>()
        sessionStore.cookieHeader().orEmpty()
            .cookiePairs()
            .forEach { (name, value) -> cookies[name] = value }
        cookieManager.getCookie(COOKIE_URL).orEmpty()
            .cookiePairs()
            .forEach { (name, value) -> cookies[name] = value }
        if (cookies.isEmpty()) return
        cookies.forEach { (name, cookieValue) ->
            val cookie = "$name=$cookieValue; Path=/; Domain=.douyin.com; Secure; SameSite=Lax"
            cookieManager.setCookie(COOKIE_URL, cookie)
            cookieManager.setCookie(WRITE_COOKIE_URL, cookie)
        }
        cookieManager.flush()
    }

    private fun snapshotUpdatedCookies() {
        val cookieManager = CookieManager.getInstance()
        val fresh = cookieManager.getCookie(COOKIE_URL).orEmpty()
        if (fresh.isBlank()) return
        if (sessionStore.mergeCookieHeader(fresh)) {
            cookieManager.flush()
        }
    }

    private fun String.cookiePairs(): List<Pair<String, String>> =
        split(';').mapNotNull { part ->
            val value = part.trim()
            val separator = value.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = value.substring(0, separator).trim()
            val cookieValue = value.substring(separator + 1).trim()
            if (name.isBlank() || cookieValue.isBlank()) return@mapNotNull null
            name to cookieValue
        }

    private fun officialRequestScript(
        id: String,
        path: String,
        body: String,
        queryParams: Map<String, String>,
    ): String = """
        (() => {
          const id = ${JSONObject.quote(id)};
          const done = (payload) => {
            try {
              window.$BRIDGE_NAME.complete(id, JSON.stringify(payload));
            } catch (_) {}
          };
          (async () => {
            try {
              let runtime = globalThis.webpackChunkdouyin_web;
              for (let attempt = 0; attempt < 160; attempt += 1) {
                if (Array.isArray(runtime)) break;
                await new Promise((resolve) => setTimeout(resolve, 250));
                runtime = globalThis.webpackChunkdouyin_web;
              }
              if (!Array.isArray(runtime)) {
                throw new Error('官方请求运行时加载超时');
              }
              let requireModule = globalThis.__douyinTvWebpackRequire;
              if (typeof requireModule !== 'function') {
                runtime.push([
                  ['douyin-tv-' + Date.now() + '-' + Math.random()],
                  {},
                  (loadedRequire) => {
                    requireModule = loadedRequire;
                  }
                ]);
                globalThis.__douyinTvWebpackRequire = requireModule;
              }
              for (let attempt = 0; attempt < 40 && typeof requireModule !== 'function'; attempt += 1) {
                await new Promise((resolve) => setTimeout(resolve, 250));
              }
              if (typeof requireModule !== 'function') {
                throw new Error('官方请求运行时初始化超时');
              }
              let requestModule = requireModule(599090);
              for (let attempt = 0; attempt < 40 && (!requestModule || typeof requestModule.v_ !== 'function'); attempt += 1) {
                await new Promise((resolve) => setTimeout(resolve, 250));
                requestModule = requireModule(599090);
              }
              const request = requestModule && requestModule.v_;
              if (typeof request !== 'function') {
                throw new Error('官方请求层不可用');
              }
              const result = await request(
                ${JSONObject.quote(path)},
                ${JSONObject(queryParams).toString()},
                ${JSONObject.quote(body)},
                { timeout: 15000 }
              );
              done({
                status: 200,
                body: JSON.stringify(result || {}),
                guardResult: 'webpack'
              });
            } catch (error) {
              done({
                error: String(error && error.message ? error.message : error)
              });
            }
          })();
        })();
    """.trimIndent()

    private fun parseResponse(raw: String): WebWriteResponse {
        val value = runCatching { JSONObject(raw) }.getOrElse {
            throw ApiException("网页写请求回执无效")
        }
        value.optString("error").takeIf(String::isNotBlank)?.let { error ->
            throw ApiException("网页写请求失败：$error")
        }
        val status = value.optInt("status", 0)
        if (status !in 200..299) {
            throw ApiException("网页写请求失败：HTTP $status")
        }
        val body = value.optString("body")
        val guardResult = value.optString("guardResult")
        Log.d(
            TAG,
            "post completed status=$status bodyLength=${body.length} " +
                "guardResult=${guardResult.ifBlank { "none" }}",
        )
        return WebWriteResponse(
            status = status,
            body = body,
            guardResult = guardResult,
        )
    }

    private suspend fun invalidatePage() = withContext(Dispatchers.Main.immediate) {
        detachAndDestroy(webView)
        webView = null
        pageReady = null
        pageSessionFingerprint = ""
        callbacks.values.forEach { it.cancel() }
        callbacks.clear()
    }

    private fun isDouyinPage(url: String?): Boolean {
        val host = url?.let { runCatching { Uri.parse(it).host }.getOrNull() }
        return host == "www.douyin.com" || host == "douyin.com"
    }

    private fun safeHost(url: String?): String =
        url?.let { runCatching { Uri.parse(it).host }.getOrNull() }.orEmpty()

    private companion object {
        const val TAG = "DouyinTvWebTransport"
        const val HOME_URL = "https://www.douyin.com/"
        const val COOKIE_URL = "https://www.douyin.com/"
        const val WRITE_COOKIE_URL = "https://www-hj.douyin.com/"
        const val BRIDGE_NAME = "DouyinTvWriteBridge"
        const val PAGE_READY_TIMEOUT_MS = 25_000L
        const val PAGE_EVENT_FALLBACK_MS = 10_000L
        const val PAGE_EVENT_FALLBACK_PROGRESS = 80
        const val SECURITY_WARMUP_MS = 1_500L
        const val REQUEST_TIMEOUT_MS = 50_000L
    }
}
