package com.ithersta.tgbotapi.entities

import com.ithersta.tgbotapi.StatefulContext
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.persistence.MessageRepository
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.callbackQueryUpdateOrNull
import dev.inmo.tgbotapi.extensions.utils.dataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.sourceChat
import dev.inmo.tgbotapi.extensions.utils.extensions.sourceUser
import dev.inmo.tgbotapi.extensions.utils.messageCallbackQueryOrNull
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.PreviewFeature

public class Dispatcher(
    messageSpecs: List<MessageSpec<*, *>>,
    private val messageRepository: MessageRepository,
    private val getUser: (UserId) -> User
) {
    private val messageSpecs = messageSpecs.sortedByDescending { it.priority }

    public suspend fun BehaviourContext.handle(update: Update) {
        val chat = update.toChat() ?: return
        val getUser = update.toUserGetter() ?: return
        val chatId = chat.id.chatId
        val (state, messageId) = update.toMessageId()
            ?.let { messageId -> (messageRepository.get(chatId, messageId) ?: MessageState.Empty) to messageId }
            ?: messageRepository.getLast(chatId)
            ?: (MessageState.Empty to -1L)
        val data = update.toData(chatId = chatId, messageId = messageId)
        val stateAccessor = StateAccessor.Static(
            snapshot = state,
            _edit = { handleStateChange(chat, getUser, messageId, it, ::handleOnEdit) },
            _new = { handleStateChange(chat, getUser, null, it, ::handleOnNew) }
        )
        val context = StatefulContext(bot, stateAccessor, chat, messageId, getUser())
        handle(context, data)
    }

    private suspend fun <M : MessageId?> BehaviourContext.handleStateChange(
        chat: Chat,
        getUser: () -> User,
        messageId: M,
        state: MessageState,
        handle: suspend (StatefulContext<*, StateAccessor.Changing<*>, *, M>) -> Unit
    ) {
        val stateAccessor = StateAccessor.Changing(
            snapshot = state,
            _new = { handleStateChange(chat, getUser, null, it, ::handleOnNew) },
            _persist = { messageRepository.save(it) }
        )
        val context = StatefulContext(bot, stateAccessor, chat, messageId, getUser())
        handle(context)
    }

    private suspend fun handle(context: StatefulContext<*, StateAccessor.Static<*>, *, MessageId>, data: Any) =
        messageSpecs.any { it.handle(context, data) }

    private suspend fun handleOnEdit(context: StatefulContext<*, StateAccessor.Changing<*>, *, MessageId>) =
        messageSpecs.any { it.handleOnEdit(context) }

    private suspend fun handleOnNew(context: StatefulContext<*, StateAccessor.Changing<*>, *, Nothing?>) =
        messageSpecs.any { it.handleOnNew(context) }

    @OptIn(PreviewFeature::class)
    private fun Update.toChat(): Chat? = sourceChat()

    private fun Update.toMessageId(): MessageId? {
        return callbackQueryUpdateOrNull()?.data?.messageCallbackQueryOrNull()?.message?.messageId
    }

    private fun Update.toData(chatId: Long, messageId: Long): Any {
        callbackQueryUpdateOrNull()?.data?.dataCallbackQueryOrNull()?.data?.let { key ->
            messageRepository.getAction(chatId, messageId, key)?.let { return it }
        }
        return data
    }

    @OptIn(PreviewFeature::class)
    private fun Update.toUserGetter() = sourceUser()?.id?.let { { getUser(it) } }
}
