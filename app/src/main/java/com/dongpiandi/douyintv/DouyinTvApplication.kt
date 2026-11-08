package com.dongpiandi.douyintv

import android.app.Application
import android.webkit.CookieManager
import com.dongpiandi.douyintv.data.DeviceIdentityStore
import com.dongpiandi.douyintv.data.PlaybackProgressStore
import com.dongpiandi.douyintv.data.SessionStore
import com.dongpiandi.douyintv.network.AbogusSigner
import com.dongpiandi.douyintv.network.DouyinApi

class DouyinTvApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CookieManager.getInstance().setAcceptCookie(true)
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val signer = AbogusSigner()
    val identityStore = DeviceIdentityStore(application, signer)
    val sessionStore = SessionStore(application)
    val progressStore = PlaybackProgressStore(application)
    val api = DouyinApi(
        identityStore = identityStore,
        sessionStore = sessionStore,
        signer = signer,
    )
}
