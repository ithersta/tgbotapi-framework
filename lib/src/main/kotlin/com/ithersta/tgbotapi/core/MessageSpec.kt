package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.StatefulContext
import com.ithersta.tgbotapi.StatefulContextImpl
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.MessageId

public typealias OnSuccess = suspend TelegramBot.() -> Unit
public typealias Handler<S, U, M, Data> = suspend StatefulContext<S, StateAccessor.Static<S>, U, M>.(Data) -> Unit
public typealias StateChangeHandler<S, U, M, Data> = suspend StatefulContext<S, StateAccessor.Changing<S>, U, M>.(Data) -> Unit

public class MessageSpec<U : User, S : MessageState> internal constructor(
    internal val priority: Int,
    private val stateMapper: (MessageState) -> S?,
    private val userMapper: (User) -> U?,
    private val triggers: List<Trigger<S, U, *>>,
    private val _onNewHandler: StateChangeHandler<S, U, Nothing?, Nothing?>?,
    private val _onEditHandler: StateChangeHandler<S, U, MessageId, Nothing?>?
) {
    private fun StatefulContext<*, *, *, *>.isApplicable() =
        stateMapper(state.snapshot) != null && userMapper(user) != null

    @PublishedApi
    internal class Trigger<S : MessageState, U : User, Data : Any>(
        private val handler: Handler<S, U, MessageId, Data>,
        private val mapper: (Any) -> Data?
    ) {
        suspend fun handle(
            context: StatefulContextImpl<S, StateAccessor.Static<S>, U, MessageId>,
            onSuccess: OnSuccess?,
            anyData: Any
        ) = mapper(anyData)?.let { data ->
            context.shouldStop = true
            handler(context, data)
            context.shouldStop.also { shouldStop ->
                if (shouldStop) {
                    onSuccess?.invoke(context)
                }
            }
        } ?: false
    }

    @Suppress("UNCHECKED_CAST")
    internal suspend fun handle(
        context: StatefulContextImpl<*, StateAccessor.Static<*>, *, MessageId>,
        onSuccess: OnSuccess?,
        data: Any
    ): Boolean =
        triggers
            .takeIf { context.isApplicable() }
            ?.any {
                it.handle(context as StatefulContextImpl<S, StateAccessor.Static<S>, U, MessageId>, onSuccess, data)
            } ?: false

    @Suppress("UNCHECKED_CAST")
    internal suspend fun handleOnNew(context: StatefulContextImpl<*, StateAccessor.Changing<*>, *, Nothing?>): Boolean =
        _onNewHandler
            ?.takeIf { context.isApplicable() }
            ?.let {
                context.shouldStop = true
                it.invoke(context as StatefulContext<S, StateAccessor.Changing<S>, U, Nothing?>, null)
                context.shouldStop
            } ?: false

    @Suppress("UNCHECKED_CAST")
    internal suspend fun handleOnEdit(context: StatefulContextImpl<*, StateAccessor.Changing<*>, *, MessageId>): Boolean =
        _onEditHandler
            ?.takeIf { context.isApplicable() }
            ?.let {
                context.shouldStop = true
                it.invoke(context as StatefulContext<S, StateAccessor.Changing<S>, U, MessageId>, null)
                context.shouldStop
            } ?: false
}
