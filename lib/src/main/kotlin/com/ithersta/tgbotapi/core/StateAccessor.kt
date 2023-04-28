package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.persistence.PersistedMessage
import dev.inmo.tgbotapi.types.chat.Chat

public sealed class StateAccessor<out S : MessageState> private constructor(
    public val snapshot: S,
    private val _new: suspend (MessageState) -> Unit,
    private val _newForeign: suspend (Chat, MessageState) -> Unit,
) {
    public suspend fun new(map: S.() -> MessageState): Unit = _new(map(snapshot))
    public suspend fun new(chat: Chat, create: () -> MessageState): Unit = _newForeign(chat, create())

    public class Static<out S : MessageState> internal constructor(
        snapshot: S,
        _new: suspend (MessageState) -> Unit,
        _newForeign: suspend (Chat, MessageState) -> Unit,
        private val _edit: suspend (MessageState) -> Unit,
    ) : StateAccessor<S>(snapshot, _new, _newForeign) {
        public suspend fun edit(map: S.() -> MessageState): Unit = _edit(map(snapshot))
    }

    public class Changing<out S : MessageState> internal constructor(
        snapshot: S,
        _new: suspend (MessageState) -> Unit,
        _newForeign: suspend (Chat, MessageState) -> Unit,
        private val _persist: (PersistedMessage) -> Unit
    ) : StateAccessor<S>(snapshot, _new, _newForeign) {
        public fun persist(message: PersistedMessage): Unit = _persist(message)
    }
}
