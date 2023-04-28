package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.StatefulContextImpl
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.persistence.MessageRepository
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.commands.BotCommandScope
import dev.inmo.tgbotapi.types.update.abstracts.Update

public class Dispatcher(
    stateSpecs: List<StateSpec<*, *>>,
    private val messageRepository: MessageRepository,
    private val updateTransformers: UpdateTransformers,
    private val getUser: GetUser
) {
    private val stateSpecs = stateSpecs.sortedByDescending { it.priority }

    public suspend fun BehaviourContext.handle(update: Update): Unit = with(updateTransformers) {
        val chat = update.toChat() ?: return
        val getUser = update.toUserId()?.let { { getUser(it) } } ?: return
        val chatId = chat.id.chatId
        val (state, messageId) = update.toMessageId()
            ?.let { messageId -> (messageRepository.get(chatId, messageId) ?: MessageState.Empty) to messageId }
            ?: messageRepository.getLast(chatId)
            ?: (MessageState.Empty to -1L)
        val data = update.toData()
        val actionKey = update.toActionKey()
        val action = actionKey?.let { messageRepository.getAction(chatId, messageId, it) }
        val stateAccessor = StateAccessor.Static(
            snapshot = state,
            _edit = { handleStateChange(chat, getUser, messageId, update, it, ::handleOnEdit) },
            _new = { handleStateChange(chat, getUser, null, update, it, ::handleOnNew) }
        )
        val context = StatefulContextImpl(bot, stateAccessor, chat, messageId, getUser(), update) {
            updateCommands(chat.id, getUser)
        }
        handle(context, update.onSuccess(), listOfNotNull(data, action, action?.to(data)))
    }

    private suspend fun BehaviourContext.updateCommands(chatId: ChatIdentifier, getUser: () -> User) {
        val scope = BotCommandScope.Chat(chatId)
        setMyCommands(stateSpecs.flatMap { it.commands(getUser()) }, scope)
    }

    private suspend fun <M : MessageId?> BehaviourContext.handleStateChange(
        chat: Chat,
        getUser: () -> User,
        messageId: M,
        update: Update,
        state: MessageState,
        handle: suspend (StatefulContextImpl<*, StateAccessor.Changing<*>, *, M>) -> Unit
    ) {
        val stateAccessor = StateAccessor.Changing(
            snapshot = state,
            _new = { handleStateChange(chat, getUser, null, update, it, ::handleOnNew) },
            _persist = { messageRepository.save(it) }
        )
        val context = StatefulContextImpl(bot, stateAccessor, chat, messageId, getUser(), update) {
            updateCommands(chat.id, getUser)
        }
        handle(context)
    }

    private suspend fun handle(
        context: StatefulContextImpl<*, StateAccessor.Static<*>, *, MessageId>,
        onSuccess: OnSuccess?,
        data: List<Any>
    ) = stateSpecs.any { it.handle(context, onSuccess, data) }

    private suspend fun handleOnEdit(context: StatefulContextImpl<*, StateAccessor.Changing<*>, *, MessageId>) =
        stateSpecs.any { it.handleOnEdit(context) }

    private suspend fun handleOnNew(context: StatefulContextImpl<*, StateAccessor.Changing<*>, *, Nothing?>) =
        stateSpecs.any { it.handleOnNew(context) }

    public interface UpdateTransformers {
        public fun Update.toData(): Any
        public fun Update.toChat(): Chat?
        public fun Update.toMessageId(): MessageId?
        public fun Update.toUserId(): UserId?
        public fun Update.toActionKey(): String?
        public fun Update.onSuccess(): (suspend TelegramBot.() -> Unit)?
    }
}
