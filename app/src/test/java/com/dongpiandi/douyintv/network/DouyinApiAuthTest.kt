package com.dongpiandi.douyintv.network

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test

class DouyinApiAuthTest {
    @Test
    fun progressUidUsesLowercaseMd5() {
        val expected = MessageDigest.getInstance("MD5")
            .digest("123456789".toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }

        assertEquals(expected, accountUidParam("123456789"))
    }
}
