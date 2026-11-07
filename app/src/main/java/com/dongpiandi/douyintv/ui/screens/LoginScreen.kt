package com.dongpiandi.douyintv.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.dongpiandi.douyintv.network.DouyinApi
import com.dongpiandi.douyintv.ui.theme.Accent
import com.dongpiandi.douyintv.ui.theme.FocusRing
import com.dongpiandi.douyintv.ui.theme.Ink
import com.dongpiandi.douyintv.ui.theme.Panel
import com.dongpiandi.douyintv.ui.theme.PanelRaised
import com.dongpiandi.douyintv.ui.theme.TextPrimary
import com.dongpiandi.douyintv.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    isSessionCookie: (String) -> Boolean,
    onLoggedIn: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val closeFocusRequester = remember { FocusRequester() }
    val refreshFocusRequester = remember { FocusRequester() }
    var loading by remember { mutableStateOf(true) }
    var qrVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var handled by remember { mutableStateOf(false) }
    var probeGeneration by remember { mutableStateOf(0) }
    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                userAgentString = DouyinApi.USER_AGENT
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            isFocusable = false
            isFocusableInTouchMode = false
            isLongClickable = false
            isHapticFeedbackEnabled = false
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(
                    this,
                    DESKTOP_LOGIN_JS,
                    setOf("*"),
                )
                WebViewCompat.addDocumentStartJavaScript(
                    this,
                    LOGIN_MODAL_FIT_JS,
                    setOf("*"),
                )
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress >= 65) {
                        loading = false
                    }
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    loading = true
                    qrVisible = false
                    error = null
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    loading = false
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    webError: android.webkit.WebResourceError?,
                ) {
                    if (request?.isForMainFrame == true) {
                        loading = false
                        error = "登录页加载失败，请检查网络后重试"
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                ): Boolean {
                    val uri = request?.url ?: return true
                    val scheme = uri.scheme?.lowercase()
                    val host = uri.host?.lowercase().orEmpty()
                    if (scheme != "https" && scheme != "http") return true
                    val allowed = host == "douyin.com" ||
                        host.endsWith(".douyin.com") ||
                        host.endsWith(".iesdouyin.com") ||
                        host.endsWith(".snssdk.com") ||
                        host.endsWith(".bytedance.com")
                    return !allowed
                }
            }
            loadUrl("https://www.douyin.com/user/self")
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.removeCallbacks(null)
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    LaunchedEffect(webView) {
        closeFocusRequester.requestFocus()
        while (isActive && !handled) {
            val cookie = CookieManager.getInstance().getCookie("https://www.douyin.com")
            if (cookie != null && isSessionCookie(cookie)) {
                handled = true
                CookieManager.getInstance().flush()
                onLoggedIn(cookie)
            }
            delay(700)
        }
    }

    LaunchedEffect(webView, probeGeneration) {
        delay(1_500)
        repeat(90) { attempt ->
            if (!isActive || handled || qrVisible) return@LaunchedEffect
            val result = webView.evaluateJavascriptSuspend(LOGIN_PROBE_JS)
            if (result.contains("qr", ignoreCase = true)) {
                webView.evaluateJavascriptSuspend(FIT_LOGIN_MODAL_JS)
                qrVisible = true
                error = null
                return@LaunchedEffect
            }
            if (attempt >= 89) {
                error = "二维码未出现，请点击“重新显示二维码”"
            }
            delay(1_000)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        val railWidth: Dp = if (maxWidth < 760.dp) 190.dp else 230.dp
        Row(modifier = Modifier.fillMaxSize()) {
            LoginRail(
                width = railWidth,
                loading = loading,
                qrVisible = qrVisible,
                error = error,
                closeFocusRequester = closeFocusRequester,
                refreshFocusRequester = refreshFocusRequester,
                onRefresh = {
                    error = null
                    qrVisible = false
                    loading = true
                    probeGeneration += 1
                    webView.reload()
                },
                onClose = onClose,
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 12.dp, bottom = 12.dp, end = 12.dp),
                color = Color.Black,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { webView },
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (loading) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 22.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Accent,
                                strokeWidth = 3.dp,
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                text = "正在打开抖音授权页",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginRail(
    width: Dp,
    loading: Boolean,
    qrVisible: Boolean,
    error: String?,
    closeFocusRequester: FocusRequester,
    refreshFocusRequester: FocusRequester,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
) {
    val statusText = when {
        error != null -> error
        loading -> "正在打开登录页"
        qrVisible -> "二维码已显示\n等待手机确认"
        else -> "正在唤起二维码"
    }
    val statusColor = when {
        error != null -> MaterialTheme.colorScheme.error
        qrVisible -> FocusRing
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF161A24), Ink),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "抖音",
                color = Accent,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "TV",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = "扫码登录",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "登录只用于获取账号授权\n视频仍在原生界面播放",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = "① 打开抖音 App\n② 首页左上角 → 扫一扫\n③ 手机确认后自动返回",
            color = TextSecondary.copy(alpha = 0.92f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.weight(1f))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PanelRaised.copy(alpha = 0.86f),
            shape = RoundedCornerShape(11.dp),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "●",
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        LoginButton(
            label = "重新显示二维码",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = refreshFocusRequester,
            onClick = onRefresh,
        )
        Spacer(Modifier.size(7.dp))
        LoginButton(
            label = "关闭",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = closeFocusRequester,
            primary = true,
            onClick = onClose,
        )
    }
}

@Composable
private fun LoginButton(
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .heightIn(min = 36.dp)
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
        color = when {
            focused -> FocusRing
            primary -> Accent
            else -> Panel
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (focused) FocusRing else Color.White.copy(alpha = 0.12f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            color = if (focused) Color(0xFF17120B) else TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private suspend fun WebView.evaluateJavascriptSuspend(script: String): String =
    suspendCancellableCoroutine { continuation ->
        post {
            evaluateJavascript(script) { result ->
                if (continuation.isActive) continuation.resume(result.orEmpty())
            }
        }
    }

private const val LOGIN_PROBE_JS = """
    (function () {
      const visible = (el) => {
        const rect = el.getBoundingClientRect();
        const style = getComputedStyle(el);
        return rect.width > 0 && rect.height > 0 &&
          style.display !== 'none' && style.visibility !== 'hidden';
      };
      const modal = Array.from(
        document.querySelectorAll('.douyin_login_new_class')
      ).find(visible);
      const qr = document.querySelector('#douyin_login_comp_scan_code_qrcode');
      if (modal && qr && visible(qr)) return 'qr';
      if (modal) return 'qr';
      const buttons = Array.from(document.querySelectorAll('button.semi-button, button'));
      let login = buttons.find((el) => {
        const text = (el.innerText || '').replace(/\s+/g, '').trim();
        return visible(el) && !el.disabled && text === '登录';
      });
      if (!login) {
        login = Array.from(document.querySelectorAll('a, div, span')).find((el) => {
          const text = (el.innerText || '').replace(/\s+/g, '').trim();
          return visible(el) && text === '登录';
        });
      }
      if (login) {
        const rect = login.getBoundingClientRect();
        const init = {
          bubbles: true,
          cancelable: true,
          composed: true,
          view: window,
          clientX: rect.left + rect.width / 2,
          clientY: rect.top + rect.height / 2,
          button: 0,
          buttons: 1,
        };
        try { login.focus({ preventScroll: true }); } catch (_) {}
        ['pointerover', 'pointerenter', 'pointerdown', 'mousedown'].forEach((type) => {
          const EventType = type.startsWith('pointer') ? PointerEvent : MouseEvent;
          login.dispatchEvent(new EventType(type, init));
        });
        init.buttons = 0;
        ['pointerup', 'mouseup', 'click'].forEach((type) => {
          const EventType = type.startsWith('pointer') ? PointerEvent : MouseEvent;
          login.dispatchEvent(new EventType(type, init));
        });
        return 'clicked';
      }
      const accountEntry = Array.from(
        document.querySelectorAll('a, div, span')
      ).find((el) => {
        const text = (el.innerText || '').replace(/\s+/g, '').trim();
        return visible(el) && text === '我的';
      });
      if (accountEntry && !globalThis.__douyinTvTriedAccountEntry) {
        globalThis.__douyinTvTriedAccountEntry = true;
        accountEntry.click();
        return 'clicked';
      }
      return 'waiting';
    })();
"""

private const val FIT_LOGIN_MODAL_JS = """
    (function () {
      const modal = document.querySelector('.douyin_login_new_class');
      if (!modal) return 'missing';
      const viewportHeight = Math.max(
        window.innerHeight || 0,
        document.documentElement.clientHeight || 0,
        600
      );
      document.documentElement.style.minHeight = viewportHeight + 'px';
      document.body.style.minHeight = viewportHeight + 'px';
      let node = modal.parentElement;
      while (node && node !== document.body) {
        const rect = node.getBoundingClientRect();
        if (rect.height < 2) {
          node.style.height = viewportHeight + 'px';
          node.style.minHeight = viewportHeight + 'px';
        }
        node = node.parentElement;
      }
      const modalRect = modal.getBoundingClientRect();
      if (modalRect.height > 0) {
        const desiredTop = Math.max(
          0,
          Math.round((viewportHeight - modalRect.height) / 2)
        );
        const currentMargin = parseFloat(modal.style.marginTop) || 0;
        const shift = Math.round(desiredTop - modalRect.top);
        if (Math.abs(shift) > 2) {
          modal.style.marginTop = (currentMargin + shift) + 'px';
        }
      }
      return 'fitted';
    })();
"""

private const val LOGIN_MODAL_FIT_JS = """
    (() => {
      const fit = () => {
        const modal = document.querySelector('.douyin_login_new_class');
        if (!modal) return;
        const viewportHeight = Math.max(
          window.innerHeight || 0,
          document.documentElement ? document.documentElement.clientHeight : 0,
          600
        );
        if (document.documentElement) {
          document.documentElement.style.minHeight = viewportHeight + 'px';
        }
        if (document.body) {
          document.body.style.minHeight = viewportHeight + 'px';
        }
        let node = modal.parentElement;
        while (node && node !== document.body) {
          const rect = node.getBoundingClientRect();
          if (rect.height < 2) {
            node.style.setProperty('height', viewportHeight + 'px', 'important');
            node.style.setProperty('min-height', viewportHeight + 'px', 'important');
          }
          node = node.parentElement;
        }
        const modalRect = modal.getBoundingClientRect();
        if (modalRect.height > 0) {
          const desiredTop = Math.max(
            0,
            Math.round((viewportHeight - modalRect.height) / 2)
          );
          const currentMargin = parseFloat(modal.style.marginTop) || 0;
          const shift = Math.round(desiredTop - modalRect.top);
          if (Math.abs(shift) > 2) {
            modal.style.marginTop = (currentMargin + shift) + 'px';
          }
        }
      };
      globalThis.__douyinTvFitLoginModal = fit;
      if (globalThis.__douyinTvFitLoginTimer) {
        clearInterval(globalThis.__douyinTvFitLoginTimer);
      }
      globalThis.__douyinTvFitLoginTimer = setInterval(fit, 500);
    })();
"""

private const val DESKTOP_LOGIN_JS = """
    (() => {
      const define = (target, property, value) => {
        try {
          Object.defineProperty(target, property, {
            get: () => value,
            configurable: true,
          });
        } catch (_) {}
      };
      define(Navigator.prototype, 'maxTouchPoints', 0);
      define(Navigator.prototype, 'platform', 'Win32');
      define(Navigator.prototype, 'userAgentData', {
        brands: [
          { brand: 'Google Chrome', version: '131' },
          { brand: 'Chromium', version: '131' },
        ],
        mobile: false,
        platform: 'Windows',
      });
      define(Screen.prototype, 'width', 1280);
      define(Screen.prototype, 'height', 720);
      define(Screen.prototype, 'availWidth', 1280);
      define(Screen.prototype, 'availHeight', 720);
    })();
"""
