package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.StatefulContext
import com.ithersta.tgbotapi.StatefulContextImpl
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.MessageId

public typealias OnSuccess = suspend TelegramBot.() -> Unit
public typealias Handler<S, U, M, Data> = suspend StatefulContext<S, StateAccessor.Static<S>, U, M>.(Data) -> Unit
public typealias StateChangeHandler<S, U, M, Data> = suspend StatefulContext<S, StateAccessor.Changing<S>, U, M>.(Data) -> Unit

public class StateSpec<R : Role, S : MessageState> internal constructor(
    internal val priority: Int,
    private val stateMapper: (MessageState) -> S?,
    private val roleMapper: (Role) -> R?,
    private val triggers: List<Trigger<S, R, *>>,
    private val commands: List<BotCommand>,
    private val _onNewHandler: StateChangeHandler<S, R, Nothing?, Nothing?>?,
    private val _onEditHandler: StateChangeHandler<S, R, MessageId, Nothing?>?
) {
    private fun StatefulContext<*, *, *, *>.isApplicable() =
        stateMapper(state.snapshot) != null && roleMapper(role) != null

    @PublishedApi
    internal class Trigger<S : MessageState, R : Role, Data : Any>(
        private val handler: Handler<S, R, MessageId, Data>,
        private val mapper: (Any) -> Data?
    ) {
        suspend fun handle(
            context: StatefulContextImpl<S, StateAccessor.Static<S>, R, MessageId>,
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
        data: List<Any>
    ): Boolean {
        if (context.isApplicable().not()) return false
        return triggers.asSequence()
            .flatMap { trigger -> data.map { trigger to it } }
            .any { (trigger, data) ->
                trigger.handle(
                    context = context as StatefulContextImpl<S, StateAccessor.Static<S>, R, MessageId>,
                    onSuccess = onSuccess,
                    anyData = data
                )
            }
    }

    @Suppress("UNCHECKED_CAST")
    internal suspend fun handleOnNew(context: StatefulContextImpl<*, StateAccessor.Changing<*>, *, Nothing?>): Boolean =
        _onNewHandler
            ?.takeIf { context.isApplicable() }
            ?.let {
                context.shouldStop = true
                it.invoke(context as StatefulContext<S, StateAccessor.Changing<S>, R, Nothing?>, null)
                context.shouldStop
            } ?: false

    @Suppress("UNCHECKED_CAST")
    internal suspend fun handleOnEdit(context: StatefulContextImpl<*, StateAccessor.Changing<*>, *, MessageId>): Boolean =
        _onEditHandler
            ?.takeIf { context.isApplicable() }
            ?.let {
                context.shouldStop = true
                it.invoke(context as StatefulContext<S, StateAccessor.Changing<S>, R, MessageId>, null)
                context.shouldStop
            } ?: false

    internal fun commands(role: Role): List<BotCommand> = roleMapper(role)?.let { commands } ?: emptyList()
}
