@file:Suppress("UnusedReceiverParameter")

package com.ithersta.tgbotapi.message.template

import com.ithersta.tgbotapi.message.MessageContext
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regular

public fun MessageContext<*, *, *>.text(
    entities: TextSourcesList,
    disableWebPagePreview: Boolean? = null,
    disableNotification: Boolean = false,
    protectContent: Boolean = false,
    replyToMessageId: MessageId? = null,
    allowSendingWithoutReply: Boolean? = null,
    replyMarkup: (InlineKeyboardBuilder.() -> Unit)? = null,
): TextMessageTemplate = TextMessageTemplate(
    entities = entities,
    disableWebPagePreview = disableWebPagePreview,
    disableNotification = disableNotification,
    protectContent = protectContent,
    replyToMessageId = replyToMessageId,
    allowSendingWithoutReply = allowSendingWithoutReply,
    replyMarkup = replyMarkup?.let { inlineKeyboard(it) },
)

public fun MessageContext<*, *, *>.text(
    text: String,
    disableWebPagePreview: Boolean? = null,
    disableNotification: Boolean = false,
    protectContent: Boolean = false,
    replyToMessageId: MessageId? = null,
    allowSendingWithoutReply: Boolean? = null,
    replyMarkup: (InlineKeyboardBuilder.() -> Unit)? = null,
): TextMessageTemplate = TextMessageTemplate(
    entities = buildEntities { regular(text) },
    disableWebPagePreview = disableWebPagePreview,
    disableNotification = disableNotification,
    protectContent = protectContent,
    replyToMessageId = replyToMessageId,
    allowSendingWithoutReply = allowSendingWithoutReply,
    replyMarkup = replyMarkup?.let { inlineKeyboard(it) },
)

public class TextMessageTemplate(
    public val entities: TextSourcesList,
    public val disableWebPagePreview: Boolean?,
    public val disableNotification: Boolean,
    public val protectContent: Boolean,
    public val replyToMessageId: MessageId?,
    public val allowSendingWithoutReply: Boolean?,
    public val replyMarkup: InlineKeyboardMarkup?,
) : MessageTemplate {
    override suspend fun TelegramBot.send(chat: Chat): Message {
        return sendTextMessage(
            chat = chat,
            entities = entities,
            disableWebPagePreview = disableWebPagePreview,
            disableNotification = disableNotification,
            protectContent = protectContent,
            replyToMessageId = replyToMessageId,
            allowSendingWithoutReply = allowSendingWithoutReply,
            replyMarkup = replyMarkup,
        )
    }

    override suspend fun TelegramBot.edit(chat: Chat, messageId: MessageId): Message {
        return editMessageText(
            chat = chat,
            messageId = messageId,
            entities = entities,
            disableWebPagePreview = disableWebPagePreview,
            replyMarkup = replyMarkup,
        )
    }
}
