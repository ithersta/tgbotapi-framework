package com.ithersta.tgbotapi.entities

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.persistence.PersistedMessage

public sealed class StateAccessor<out S : MessageState> private constructor(
    public val snapshot: S,
    private val _new: suspend (MessageState) -> Unit,
) {
    public suspend fun new(map: (S) -> MessageState): Unit = _new(map(snapshot))

    public class Static<out S : MessageState> internal constructor(
        snapshot: S,
        _new: suspend (MessageState) -> Unit,
        private val _edit: suspend (MessageState) -> Unit,
    ) : StateAccessor<S>(snapshot, _new) {
        public suspend fun edit(map: (S) -> MessageState): Unit = _edit(map(snapshot))
    }

    public class Changing<out S : MessageState> internal constructor(
        snapshot: S,
        _new: suspend (MessageState) -> Unit,
        private val _persist: (PersistedMessage) -> Unit
    ) : StateAccessor<S>(snapshot, _new) {
        public fun persist(message: PersistedMessage): Unit = _persist(message)
    }
}
