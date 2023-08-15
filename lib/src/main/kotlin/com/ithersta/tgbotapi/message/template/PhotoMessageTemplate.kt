@file:Suppress("UnusedReceiverParameter")

package com.ithersta.tgbotapi.message.template

import com.ithersta.tgbotapi.message.MessageContext
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regular

public fun MessageContext<*, *, *>.photo(
    fileId: InputFile,
    text: String? = null,
    spoilered: Boolean = false,
    disableNotification: Boolean = false,
    protectContent: Boolean = false,
    replyToMessageId: MessageId? = null,
    allowSendingWithoutReply: Boolean? = null,
    replyMarkup: (InlineKeyboardBuilder.() -> Unit)?,
): PhotoMessageTemplate = PhotoMessageTemplate(
    fileId = fileId,
    entities = text?.let { buildEntities { regular(it) } },
    spoilered = spoilered,
    disableNotification = disableNotification,
    protectContent = protectContent,
    replyToMessageId = replyToMessageId,
    allowSendingWithoutReply = allowSendingWithoutReply,
    replyMarkup = replyMarkup?.let { inlineKeyboard(it) }
)

public fun MessageContext<*, *, *>.photo(
    fileId: InputFile,
    entities: TextSourcesList? = null,
    spoilered: Boolean = false,
    disableNotification: Boolean = false,
    protectContent: Boolean = false,
    replyToMessageId: MessageId? = null,
    allowSendingWithoutReply: Boolean? = null,
    replyMarkup: (InlineKeyboardBuilder.() -> Unit)?,
): PhotoMessageTemplate = PhotoMessageTemplate(
    fileId = fileId,
    entities = entities,
    spoilered = spoilered,
    disableNotification = disableNotification,
    protectContent = protectContent,
    replyToMessageId = replyToMessageId,
    allowSendingWithoutReply = allowSendingWithoutReply,
    replyMarkup = replyMarkup?.let { inlineKeyboard(it) }
)

public class PhotoMessageTemplate(
    public val fileId: InputFile,
    public val entities: TextSourcesList?,
    public val spoilered: Boolean,
    public val disableNotification: Boolean,
    public val protectContent: Boolean,
    public val replyToMessageId: MessageId?,
    public val allowSendingWithoutReply: Boolean?,
    public val replyMarkup: InlineKeyboardMarkup?,
) : MessageTemplate {
    override suspend fun TelegramBot.send(chat: Chat): Message {
        return sendPhoto(
            chat = chat,
            fileId = fileId,
            entities = entities ?: buildEntities {},
            spoilered = spoilered,
            disableNotification = disableNotification,
            protectContent = protectContent,
            replyToMessageId = replyToMessageId,
            allowSendingWithoutReply = allowSendingWithoutReply,
            replyMarkup = replyMarkup
        )
    }

    override suspend fun TelegramBot.edit(chat: Chat, messageId: MessageId): Message {
        return edit(
            chat = chat,
            messageId = messageId,
            media = TelegramMediaPhoto(fileId, entities ?: buildEntities {}),
            replyMarkup = replyMarkup
        )
    }
}
