package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.StatefulContextImpl
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.persistence.MessageRepository
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.callbackQueryUpdateOrNull
import dev.inmo.tgbotapi.extensions.utils.dataCallbackQueryOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.sourceChat
import dev.inmo.tgbotapi.extensions.utils.extensions.sourceUser
import dev.inmo.tgbotapi.extensions.utils.messageCallbackQueryOrNull
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.PreviewFeature

public class Dispatcher(
    messageSpecs: List<MessageSpec<*, *>>,
    private val messageRepository: MessageRepository,
    private val getUser: GetUser
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
        val data = update.toData(chatId = chatId, messageId = messageId) { answer(it) }
        val stateAccessor = StateAccessor.Static(
            snapshot = state,
            _edit = { handleStateChange(chat, getUser, messageId, it, ::handleOnEdit) },
            _new = { handleStateChange(chat, getUser, null, it, ::handleOnNew) }
        )
        val context = StatefulContextImpl(bot, stateAccessor, chat, messageId, getUser())
        handle(context, data)
    }

    private suspend fun <M : MessageId?> BehaviourContext.handleStateChange(
        chat: Chat,
        getUser: () -> User,
        messageId: M,
        state: MessageState,
        handle: suspend (StatefulContextImpl<*, StateAccessor.Changing<*>, *, M>) -> Unit
    ) {
        val stateAccessor = StateAccessor.Changing(
            snapshot = state,
            _new = { handleStateChange(chat, getUser, null, it, ::handleOnNew) },
            _persist = { messageRepository.save(it) }
        )
        val context = StatefulContextImpl(bot, stateAccessor, chat, messageId, getUser())
        handle(context)
    }

    private suspend fun handle(context: StatefulContextImpl<*, StateAccessor.Static<*>, *, MessageId>, data: Any) =
        messageSpecs.any { it.handle(context, data) }

    private suspend fun handleOnEdit(context: StatefulContextImpl<*, StateAccessor.Changing<*>, *, MessageId>) =
        messageSpecs.any { it.handleOnEdit(context) }

    private suspend fun handleOnNew(context: StatefulContextImpl<*, StateAccessor.Changing<*>, *, Nothing?>) =
        messageSpecs.any { it.handleOnNew(context) }

    @OptIn(PreviewFeature::class)
    private fun Update.toChat(): Chat? = sourceChat()

    private fun Update.toMessageId(): MessageId? {
        return callbackQueryUpdateOrNull()?.data?.messageCallbackQueryOrNull()?.message?.messageId
    }

    private suspend fun Update.toData(chatId: Long, messageId: Long, answer: suspend (DataCallbackQuery) -> Unit): Any {
        callbackQueryUpdateOrNull()?.data?.dataCallbackQueryOrNull()?.let { query ->
            messageRepository.getAction(chatId, messageId, query.data)?.let {
                answer(query)
                return it
            }
        }
        return data
    }

    @OptIn(PreviewFeature::class)
    private fun Update.toUserGetter() = sourceUser()?.id
        .let { it ?: toChat()?.id?.chatId?.let { chatId -> UserId(chatId) } }
        ?.let { { getUser(it) } }
}
