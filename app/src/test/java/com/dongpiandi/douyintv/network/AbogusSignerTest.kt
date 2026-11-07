package com.dongpiandi.douyintv.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbogusSignerTest {
    @Test
    fun matchesDeterministicNodeVector() {
        var state = 123456789L
        fun seededRandom(): Double {
            state = (state * 1_664_525L + 1_013_904_223L) and 0xFFFF_FFFFL
            return state / 4_294_967_296.0
        }

        val signer = AbogusSigner(
            nowProvider = { FIXED_NOW },
            random = ::seededRandom,
            startedAt = FIXED_NOW,
        )
        val query =
            "device_platform=webapp&aid=6383&channel=channel_pc_web&count=20&tag_id=" +
                "&Seo-Flag=0&refresh_index=1&webid=7521298911878202918" +
                "&verifyFp=verify_test_fixture"

        val actual = signer.sign(
            query = query,
            userAgent = USER_AGENT,
            platform = "Win32",
            browser = "Chrome",
            now = FIXED_NOW,
            initAt = FIXED_NOW,
            requestCounter = 7,
        )

        assertEquals(EXPECTED, actual)
    }

    @Test
    fun signaturesHaveTheExpectedShapeAndVary() {
        val signer = AbogusSigner()
        val first = signer.sign("aid=6383&count=20")
        val second = signer.sign("aid=6383&count=20")

        assertTrue(first.length in 160..200)
        assertTrue(first.any { it == '-' || it == '/' || it == '+' })
        assertNotEquals(first, second)
    }

    private companion object {
        const val FIXED_NOW = 1_700_000_000_000L
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        const val EXPECTED =
            "Efs5kFUJDq8VedFbmOjl9aVlJHnlNs8f7MTdWPATCNK6C70aNSEOuNcgjxoh4uEsz8BwhF376jUAbxVc0zUTpo9pwmkvSQXWPUQA96fL/HZDTskh9p6TCEbEzvscUATYQ/Q-E5wX1Uln2oQ3NHcsA1K9C/tH5ubMsrNyd5RUex2-6aJYnqazHE=="
    }
}
