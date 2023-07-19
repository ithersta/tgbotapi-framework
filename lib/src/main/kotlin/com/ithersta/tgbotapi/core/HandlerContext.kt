package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.builders.FrameworkDslMarker
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.Chat

public typealias OnActionContext<R, S> = HandlerContext<S, StateAccessor.Static<S>, R, MessageId>
public typealias OnNewContext<R, S> = HandlerContext<S, StateAccessor.Changing<S>, R, out MessageId?>
public typealias OnEditContext<R, S> = HandlerContext<S, StateAccessor.Changing<S>, R, MessageId>

@FrameworkDslMarker
public interface HandlerContext<S : MessageState, SA : StateAccessor<S>, R : Role, M : MessageId?> : TelegramBot {
    public val state: SA
    public val chat: Chat
    public val messageId: M
    public val role: R
    public fun fallthrough()
    public suspend fun updateCommands()
}

public class HandlerContextImpl<S : MessageState, SA : StateAccessor<S>, R : Role, M : MessageId?>(
    private val telegramBot: TelegramBot,
    public override val state: SA,
    public override val chat: Chat,
    public override val messageId: M,
    public override val role: R,
    private val _updateCommands: suspend () -> Unit,
) : HandlerContext<S, SA, R, M>, TelegramBot by telegramBot {
    internal var shouldStop = false
    public override fun fallthrough() {
        shouldStop = false
    }

    override suspend fun updateCommands() {
        _updateCommands()
    }
}
