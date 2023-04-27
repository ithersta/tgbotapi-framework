package com.ithersta.tgbotapi.persistence

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import dev.inmo.tgbotapi.types.MessageId

public interface MessageRepository {
    public fun save(message: PersistedMessage)
    public fun get(chatId: Long, messageId: Long): MessageState?
    public fun getLast(chatId: Long): Pair<MessageState, MessageId>?
    public fun getAction(chatId: Long, messageId: Long, key: String): Action?
}
