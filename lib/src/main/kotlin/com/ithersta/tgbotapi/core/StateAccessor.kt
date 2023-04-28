package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.persistence.PersistedMessage
import dev.inmo.tgbotapi.types.chat.Chat

/**
 * Provides a way to access state and change it.
 *
 * @property snapshot the snapshot of the current state.
 */
public sealed class StateAccessor<out S : MessageState> private constructor(
    public val snapshot: S,
    private val _new: suspend (MessageState) -> Unit,
    private val _newForeign: suspend (Chat, MessageState) -> Unit,
) {
    /**
     * Send a new message with a specified state in this chat.
     *
     * @param map transform current state into the new one.
     */
    public suspend fun new(map: S.() -> MessageState): Unit = _new(map(snapshot))

    /**
     * Send a new message with a specified state in a different chat.
     *
     * @param chat the chat where the message will be sent.
     * @param create creates a new state.
     */
    public suspend fun new(chat: Chat, create: () -> MessageState): Unit = _newForeign(chat, create())

    public class Static<out S : MessageState> internal constructor(
        snapshot: S,
        _new: suspend (MessageState) -> Unit,
        _newForeign: suspend (Chat, MessageState) -> Unit,
        private val _edit: suspend (MessageState) -> Unit,
    ) : StateAccessor<S>(snapshot, _new, _newForeign) {
        /**
         * Edits the current message using a specified state.
         *
         * @param map transform current state into the new one.
         */
        public suspend fun edit(map: S.() -> MessageState): Unit = _edit(map(snapshot))
    }

    public class Changing<out S : MessageState> internal constructor(
        snapshot: S,
        _new: suspend (MessageState) -> Unit,
        _newForeign: suspend (Chat, MessageState) -> Unit,
        private val _persist: (PersistedMessage) -> Unit
    ) : StateAccessor<S>(snapshot, _new, _newForeign) {
        /**
         * Persists the message with its message id, state, and actions.
         *
         * @param message data that will be persisted.
         */
        public fun persist(message: PersistedMessage): Unit = _persist(message)
    }
}
