package com.ithersta.tgbotapi

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.core.StateAccessor
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.update.abstracts.Update

public interface StatefulContext<S : MessageState, SA : StateAccessor<S>, R : Role, M : MessageId?> : TelegramBot {
    public val state: SA
    public val chat: Chat
    public val messageId: M
    public val role: R
    public val update: Update
    public fun fallthrough()
    public suspend fun updateCommands()
}

public class StatefulContextImpl<S : MessageState, SA : StateAccessor<S>, R : Role, M : MessageId?>(
    private val telegramBot: TelegramBot,
    public override val state: SA,
    public override val chat: Chat,
    public override val messageId: M,
    public override val role: R,
    public override val update: Update,
    private val _updateCommands: suspend () -> Unit
) : StatefulContext<S, SA, R, M>, TelegramBot by telegramBot {
    internal var shouldStop = false
    public override fun fallthrough() {
        shouldStop = false
    }

    override suspend fun updateCommands() {
        _updateCommands()
    }
}
