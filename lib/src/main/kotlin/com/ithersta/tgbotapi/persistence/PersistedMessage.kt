package com.ithersta.tgbotapi.persistence

import com.ithersta.tgbotapi.basetypes.MessageState

public class PersistedMessage(
    public val chatId: Long,
    public val messageId: Long,
    public val state: MessageState,
    public val handleGlobalUpdates: Boolean,
    public val actions: List<PersistedAction> = emptyList()
)
