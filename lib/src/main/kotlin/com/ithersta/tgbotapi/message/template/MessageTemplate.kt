package com.ithersta.tgbotapi.message.template

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.message.abstracts.Message

public sealed interface MessageTemplate {
    public suspend fun TelegramBot.send(chat: Chat): Message
    public suspend fun TelegramBot.edit(chat: Chat, messageId: MessageId): Message
}
