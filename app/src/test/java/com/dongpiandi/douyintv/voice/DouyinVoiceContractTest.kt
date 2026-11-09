package com.dongpiandi.douyintv.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DouyinVoiceContractTest {
    @Test
    fun parsesSearchCommandAndNormalizesQuery() {
        val command =
            DouyinVoiceCommand.fromExtras(
                operation = "SEARCH",
                query = "  三国演义  ",
                openFirstResult = true,
            )

        assertEquals(DouyinVoiceOperation.SEARCH, command?.operation)
        assertEquals("三国演义", command?.query)
        assertTrue(command?.openFirstResult == true)
    }

    @Test
    fun searchWithoutQueryIsRejected() {
        assertNull(
            DouyinVoiceCommand.fromExtras(
                operation = "search",
                query = "   ",
                openFirstResult = false,
            ),
        )
    }

    @Test
    fun parsesPlaybackAndEngagementOperations() {
        val expected =
            mapOf(
                "pause" to DouyinVoiceOperation.PAUSE,
                "play" to DouyinVoiceOperation.PLAY,
                "next" to DouyinVoiceOperation.NEXT,
                "previous" to DouyinVoiceOperation.PREVIOUS,
                "comment" to DouyinVoiceOperation.COMMENT,
                "favorite" to DouyinVoiceOperation.FAVORITE,
                "unfavorite" to DouyinVoiceOperation.UNFAVORITE,
                "author" to DouyinVoiceOperation.AUTHOR,
            )

        expected.forEach { (wire, operation) ->
            val command =
                DouyinVoiceCommand.fromExtras(
                    operation = wire.uppercase(),
                    query = null,
                    openFirstResult = true,
                )
            assertEquals(operation, command?.operation)
            assertFalse(command?.openFirstResult == true)
        }
    }

    @Test
    fun rejectsUnknownOperation() {
        assertNull(
            DouyinVoiceCommand.fromExtras(
                operation = "delete_everything",
                query = "三国演义",
                openFirstResult = true,
            ),
        )
    }
}
