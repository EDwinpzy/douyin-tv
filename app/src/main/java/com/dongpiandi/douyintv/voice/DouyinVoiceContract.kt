package com.dongpiandi.douyintv.voice

internal enum class DouyinVoiceOperation(
    val wireValue: String,
) {
    SEARCH("search"),
    PLAY("play"),
    PAUSE("pause"),
    NEXT("next"),
    PREVIOUS("previous"),
    COMMENT("comment"),
    FAVORITE("favorite"),
    UNFAVORITE("unfavorite"),
    AUTHOR("author"),
    ;

    companion object {
        fun fromWire(value: String?): DouyinVoiceOperation? =
            entries.firstOrNull { operation ->
                operation.wireValue.equals(value?.trim(), ignoreCase = true)
            }
    }
}

internal data class DouyinVoiceCommand(
    val operation: DouyinVoiceOperation,
    val query: String = "",
    val openFirstResult: Boolean = false,
) {
    companion object {
        private const val MAX_QUERY_LENGTH = 80

        fun fromExtras(
            operation: String?,
            query: String?,
            openFirstResult: Boolean,
        ): DouyinVoiceCommand? {
            val parsedOperation = DouyinVoiceOperation.fromWire(operation) ?: return null
            val normalizedQuery = query.orEmpty().trim().take(MAX_QUERY_LENGTH)
            if (parsedOperation == DouyinVoiceOperation.SEARCH && normalizedQuery.isEmpty()) {
                return null
            }
            return DouyinVoiceCommand(
                operation = parsedOperation,
                query = normalizedQuery,
                openFirstResult =
                    parsedOperation == DouyinVoiceOperation.SEARCH && openFirstResult,
            )
        }
    }
}

internal object DouyinVoiceContract {
    const val EXTRA_OPERATION = "operation"
    const val EXTRA_QUERY = "query"
    const val EXTRA_OPEN_FIRST_RESULT = "openFirstResult"
    const val COMMAND_PROVIDER_AUTHORITY =
        "com.dongpiandi.douyintv.voice.commands"
    const val COMMAND_PROVIDER_METHOD = "dispatch"
    const val EXTRA_ACCEPTED = "accepted"
    const val MAIN_ACTIVITY = "com.dongpiandi.douyintv.MainActivity"
    const val VOICE_ENTRY_ACTIVITY =
        "com.dongpiandi.douyintv.voice.VoiceEntryActivity"
}
