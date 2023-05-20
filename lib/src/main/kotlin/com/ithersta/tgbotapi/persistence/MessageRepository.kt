package com.ithersta.tgbotapi.persistence

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import dev.inmo.tgbotapi.types.MessageId
import kotlinx.coroutines.flow.Flow

public interface MessageRepository {
    public fun save(message: PersistedMessage)
    public fun get(chatId: Long, messageId: Long): MessageState?
    public fun delete(chatId: Long, messageId: Long)
    public fun getLast(chatId: Long): Pair<MessageState, MessageId>?
    public fun getAction(chatId: Long, messageId: Long, key: String): Action?
    public suspend fun save(pendingState: PendingState.New)
    public suspend fun getPending(): Flow<PendingState>
}
