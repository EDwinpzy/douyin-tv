package com.dongpiandi.douyintv.data

internal object SessionCookie {
    private val sessionPattern =
        Regex("""(?:^|;\s*)sessionid(?:_ss)?=[^;\s]+""")

    fun hasSession(cookieHeader: String): Boolean =
        sessionPattern.containsMatchIn(cookieHeader)
}
