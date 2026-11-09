package com.dongpiandi.douyintv.voice

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

internal data class QueuedDouyinVoiceCommand(
    val command: DouyinVoiceCommand,
    val enqueuedAtMs: Long = System.currentTimeMillis(),
)

internal object DouyinVoiceCommandQueue {
    private val commands = Channel<QueuedDouyinVoiceCommand>(capacity = Channel.UNLIMITED)

    val incoming = commands.receiveAsFlow()

    fun offer(command: DouyinVoiceCommand): Boolean =
        commands.trySend(QueuedDouyinVoiceCommand(command)).isSuccess
}
