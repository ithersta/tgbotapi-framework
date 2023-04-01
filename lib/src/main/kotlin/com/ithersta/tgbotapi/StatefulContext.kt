package com.ithersta.tgbotapi

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.entities.StateAccessor
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.Chat

public interface StatefulContext<S : MessageState, SA : StateAccessor<S>, U : User, M : MessageId?> : TelegramBot {
    public val state: SA
    public val chat: Chat
    public val messageId: M
    public val user: U
    public fun fallthrough()
}

public class StatefulContextImpl<S : MessageState, SA : StateAccessor<S>, U : User, M : MessageId?>(
    private val telegramBot: TelegramBot,
    public override val state: SA,
    public override val chat: Chat,
    public override val messageId: M,
    public override val user: U
) : StatefulContext<S, SA, U, M>, TelegramBot by telegramBot {
    internal var shouldStop = false
    public override fun fallthrough() {
        shouldStop = false
    }
}
