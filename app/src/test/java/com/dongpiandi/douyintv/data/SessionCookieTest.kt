package com.dongpiandi.douyintv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCookieTest {
    @Test
    fun acceptsSessionWithoutSidGuard() {
        assertTrue(SessionCookie.hasSession("ttwid=abc; sessionid=token"))
    }

    @Test
    fun acceptsSessionIdSs() {
        assertTrue(SessionCookie.hasSession("sessionid_ss=token"))
    }

    @Test
    fun rejectsHeaderWithoutSessionId() {
        assertFalse(SessionCookie.hasSession("ttwid=abc; UIFID=def"))
    }

    @Test
    fun mergePrefersFreshCookieValues() {
        val merged = SessionCookie.merge(
            stored = "ttwid=old; sessionid=stale; UIFID=old-device",
            fresh = "ttwid=new; sessionid=fresh",
        )

        assertEquals(
            "ttwid=new; sessionid=fresh; UIFID=old-device",
            merged,
        )
    }

    @Test
    fun mergeKeepsStoredSessionWhenFreshHeaderHasNoSession() {
        val merged = SessionCookie.merge(
            stored = "sessionid=valid; UIFID=old-device",
            fresh = "ttwid=new",
        )

        assertEquals("sessionid=valid; UIFID=old-device; ttwid=new", merged)
    }

    @Test
    fun mergeRejectsHeadersWithoutSession() {
        assertNull(SessionCookie.merge("ttwid=old", "UIFID=new"))
    }

    @Test
    fun blankFreshSessionDoesNotOverwriteStoredLogin() {
        val merged = SessionCookie.merge(
            stored = "sessionid=valid; UIFID=old-device",
            fresh = "sessionid=; sessionid_ss=; ttwid=new",
        )

        assertEquals("sessionid=valid; UIFID=old-device; ttwid=new", merged)
    }

    @Test
    fun loginFingerprintIgnoresRotatingDeviceCookies() {
        val first = SessionCookie.loginSessionFingerprint(
            "sessionid=valid; ttwid=one; UIFID=old",
        )
        val second = SessionCookie.loginSessionFingerprint(
            "ttwid=two; sessionid=valid; UIFID=new",
        )

        assertEquals(first, second)
        assertFalse(first.isBlank())
    }

    @Test
    fun loginFingerprintChangesWhenLoginChanges() {
        val first = SessionCookie.loginSessionFingerprint("sessionid=one")
        val second = SessionCookie.loginSessionFingerprint("sessionid=two")

        assertFalse(first == second)
    }
}
