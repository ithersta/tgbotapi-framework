package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.persistence.PersistedMessage
import dev.inmo.tgbotapi.types.IdChatIdentifier

/**
 * Provides a way to create a new state.
 */
public interface UnboundStateAccessor {
    /**
     * Send a new message with the specified state in the specified chat.
     *
     * @param chatId the chat where the message will be sent.
     * @param state returns new state.
     */
    public suspend fun new(chatId: IdChatIdentifier, state: () -> MessageState)
}

internal class UnboundStateAccessorImpl(
    new: suspend (IdChatIdentifier, MessageState) -> Unit,
) : UnboundStateAccessor {
    private val _new = new
    override suspend fun new(chatId: IdChatIdentifier, state: () -> MessageState) = _new(chatId, state())
}

/**
 * Provides a way to access state and change it.
 *
 * @property snapshot the snapshot of the current state.
 */
public sealed class StateAccessor<out S : MessageState> private constructor(
    public val snapshot: S,
    new: suspend (MessageState) -> Unit,
    unboundStateAccessor: UnboundStateAccessor,
) : UnboundStateAccessor by unboundStateAccessor {
    private val _new = new

    /**
     * Send a new message with the specified state in this chat.
     *
     * @param map transform current state into the new one.
     */
    public suspend fun new(map: S.() -> MessageState): Unit = _new(map(snapshot))

    public class Static<out S : MessageState> internal constructor(
        snapshot: S,
        new: suspend (MessageState) -> Unit,
        edit: suspend (MessageState) -> Unit,
        unboundStateAccessor: UnboundStateAccessor,
    ) : StateAccessor<S>(snapshot, new, unboundStateAccessor) {
        private val _edit = edit

        /**
         * Edits the current message using the specified state.
         *
         * @param map transform current state into the new one.
         */
        public suspend fun edit(map: S.() -> MessageState): Unit = _edit(map(snapshot))
    }

    public class Changing<out S : MessageState> internal constructor(
        snapshot: S,
        new: suspend (MessageState) -> Unit,
        persist: (PersistedMessage) -> Unit,
        unboundStateAccessor: UnboundStateAccessor,
    ) : StateAccessor<S>(snapshot, new, unboundStateAccessor) {
        private val _persist = persist

        /**
         * Persists the message with its message id, state, and actions.
         *
         * @param message data that will be persisted.
         */
        public fun persist(message: PersistedMessage): Unit = _persist(message)
    }
}
