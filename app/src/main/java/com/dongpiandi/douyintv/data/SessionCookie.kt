package com.dongpiandi.douyintv.data

import java.security.MessageDigest

internal object SessionCookie {
    private val sessionNames = setOf(
        "sessionid",
        "sessionid_ss",
        "sid_tt",
        "sid_guard",
    )

    fun hasSession(cookieHeader: String): Boolean =
        parse(cookieHeader).any { (name, value) ->
            name in sessionNames && value.isNotBlank()
        }

    fun merge(stored: String, fresh: String): String? {
        val cookies = linkedMapOf<String, String>()
        parse(stored).forEach { (name, value) -> cookies[name] = value }
        parse(fresh).forEach { (name, value) -> cookies[name] = value }
        val merged = cookies.entries.joinToString("; ") { (name, value) ->
            "$name=$value"
        }
        return merged.takeIf(::hasSession)
    }

    fun loginSessionFingerprint(cookieHeader: String): String {
        val loginCookies = parse(cookieHeader)
            .filter { (name, value) -> name in sessionNames && value.isNotBlank() }
            .sortedBy { it.first }
            .joinToString("\n") { (name, value) -> "$name=$value" }
        return MessageDigest.getInstance("SHA-256")
            .digest(loginCookies.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun parse(cookieHeader: String): List<Pair<String, String>> =
        cookieHeader.split(';').mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = part.substring(0, separator).trim()
            val value = part.substring(separator + 1).trim()
            if (name.isBlank() || value.isBlank()) {
                null
            } else {
                name to value
            }
        }
}
