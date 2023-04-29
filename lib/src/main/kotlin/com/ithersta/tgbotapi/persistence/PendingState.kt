package com.ithersta.tgbotapi.persistence

import com.ithersta.tgbotapi.basetypes.MessageState

public data class PendingState(
    val id: Long,
    val chatId: Long,
    val state: MessageState?,
) {
    public data class New(
        val chatId: Long,
        val state: MessageState,
    )
}
