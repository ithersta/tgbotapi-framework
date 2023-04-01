package com.ithersta.tgbotapi

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.entities.StateAccessor
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.Chat

public class StatefulContext<S : MessageState, SA : StateAccessor<S>, U : User, M : MessageId?>(
    private val telegramBot: TelegramBot,
    public val state: SA,
    public val chat: Chat,
    public val messageId: M,
    public val user: U
) : TelegramBot by telegramBot {
    internal var shouldStop = false
    public fun fallthrough() {
        shouldStop = false
    }
}
