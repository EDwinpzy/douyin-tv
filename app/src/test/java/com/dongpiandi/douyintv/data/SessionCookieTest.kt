package com.dongpiandi.douyintv.data

import org.junit.Assert.assertFalse
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
}
