package com.dongpiandi.douyintv.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionStore(context: Context) {
    private val preferences =
        context.getSharedPreferences("douyin_tv_secure_session", Context.MODE_PRIVATE)

    fun cookieHeader(): String? {
        val encoded = preferences.getString(KEY_COOKIE, null) ?: return null
        return runCatching {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= IV_SIZE) return@runCatching null
            val iv = combined.copyOfRange(0, IV_SIZE)
            val payload = combined.copyOfRange(IV_SIZE, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(payload), Charsets.UTF_8)
        }.getOrElse {
            clear()
            null
        }?.takeIf { hasSessionCookie(it) }
    }

    fun saveCookieHeader(cookieHeader: String): Boolean {
        if (!hasSessionCookie(cookieHeader)) return false
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val payload = cipher.doFinal(cookieHeader.toByteArray(Charsets.UTF_8))
            val combined = cipher.iv + payload
            preferences.edit()
                .putString(KEY_COOKIE, Base64.encodeToString(combined, Base64.NO_WRAP))
                .apply()
            true
        }.getOrDefault(false)
    }

    fun hasSession(): Boolean = cookieHeader() != null

    fun clear() {
        preferences.edit().clear().apply()
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        }
    }

    fun hasSessionCookie(cookieHeader: String): Boolean =
        SESSION_PATTERN.containsMatchIn(cookieHeader)

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "douyin_tv_session"
        const val KEY_COOKIE = "cookie_ciphertext"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        val SESSION_PATTERN = Regex("""(?:^|;\s*)sessionid(?:_ss)?=[^;\s]+""")
    }
}
