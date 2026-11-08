package com.dongpiandi.douyintv.data

import android.content.Context
import com.dongpiandi.douyintv.network.AbogusSigner
import java.security.SecureRandom

class DeviceIdentityStore(
    context: Context,
    private val signer: AbogusSigner,
) {
    private val preferences =
        context.getSharedPreferences("douyin_tv_device_identity", Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    val webId: String
        get() = preferences.getString(KEY_WEB_ID, null) ?: randomDigits(19).also {
            preferences.edit().putString(KEY_WEB_ID, it).apply()
        }

    val verifyFp: String
        get() = preferences.getString(KEY_VERIFY_FP, null) ?: signer.verifyFp().also {
            preferences.edit().putString(KEY_VERIFY_FP, it).apply()
        }

    val uifid: String
        get() = preferences.getString(KEY_UIFID, null) ?: randomHex(80).also {
            preferences.edit().putString(KEY_UIFID, it).apply()
        }

    val msToken: String
        get() = preferences.getString(KEY_MS_TOKEN, null) ?: randomAlphaNumeric(120).also {
            preferences.edit().putString(KEY_MS_TOKEN, it).apply()
        }

    fun ttwid(): String? = preferences.getString(KEY_TTWID, null)

    fun saveTtwid(value: String) {
        preferences.edit().putString(KEY_TTWID, value).apply()
    }

    fun clearTtwid() {
        preferences.edit().remove(KEY_TTWID).apply()
    }

    private fun randomDigits(length: Int): String = buildString(length) {
        append(secureRandom.nextInt(9) + 1)
        repeat(length - 1) {
            append(secureRandom.nextInt(10))
        }
    }

    private fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return buildString(byteCount * 2) {
            bytes.forEach { byte ->
                val value = byte.toInt() and 255
                append(HEX[value ushr 4])
                append(HEX[value and 15])
            }
        }
    }

    private fun randomAlphaNumeric(length: Int): String = buildString(length) {
        repeat(length) {
            append(MS_TOKEN_ALPHABET[secureRandom.nextInt(MS_TOKEN_ALPHABET.length)])
        }
    }

    private companion object {
        const val KEY_WEB_ID = "web_id"
        const val KEY_VERIFY_FP = "verify_fp"
        const val KEY_UIFID = "uifid"
        const val KEY_TTWID = "ttwid"
        const val KEY_MS_TOKEN = "ms_token"
        const val HEX = "0123456789abcdef"
        const val MS_TOKEN_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789="
    }
}
