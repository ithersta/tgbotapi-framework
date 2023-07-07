package com.ithersta.tgbotapi.autoconfigure

import com.ithersta.tgbotapi.core.Dispatcher
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.answers.answer
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
import kotlinx.coroutines.runBlocking

@OptIn(PreviewFeature::class)
object DefaultUpdateTransformers : Dispatcher.UpdateTransformers {
    override fun Update.toData(): Any = data
    override fun Update.toChat(): Chat? = sourceChat()

    override fun Update.toMessageId(): MessageId? =
        callbackQueryUpdateOrNull()?.data?.messageCallbackQueryOrNull()?.message?.messageId

    override fun Update.toUserId(): UserId? = sourceUser()?.id
        .let { it ?: toChat()?.id?.chatId?.let { chatId -> UserId(chatId) } }

    override fun Update.toActionKey(): String? =
        callbackQueryUpdateOrNull()?.data?.dataCallbackQueryOrNull()?.data

    override fun Update.onSuccess(): (suspend TelegramBot.() -> Unit)? =
        callbackQueryUpdateOrNull()?.data?.let { query ->
            { answer(query) }
        }
}
