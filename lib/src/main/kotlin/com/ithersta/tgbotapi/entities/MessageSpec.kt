package com.ithersta.tgbotapi.entities

import com.ithersta.tgbotapi.StatefulContext
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import dev.inmo.tgbotapi.types.MessageId

public typealias Handler<S, U, M, Data> = suspend StatefulContext<S, StateAccessor.Static<S>, U, M>.(Data) -> Unit
public typealias StateChangeHandler<S, U, M, Data> = suspend StatefulContext<S, StateAccessor.Changing<S>, U, M>.(Data) -> Unit

public class MessageSpec<S : MessageState, U : User> internal constructor(
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
            context: StatefulContext<S, StateAccessor.Static<S>, U, MessageId>,
            anyData: Any
        ) = mapper(anyData)?.let { data ->
            context.shouldStop = true
            handler(context, data)
            context.shouldStop
        } ?: false
    }

    @Suppress("UNCHECKED_CAST")
    internal suspend fun handle(context: StatefulContext<*, StateAccessor.Static<*>, *, MessageId>, data: Any): Boolean =
        triggers
            .takeIf { context.isApplicable() }
            ?.any {
                it.handle(context as StatefulContext<S, StateAccessor.Static<S>, U, MessageId>, data)
            } ?: false

    @Suppress("UNCHECKED_CAST")
    internal suspend fun handleOnNew(context: StatefulContext<*, StateAccessor.Changing<*>, *, Nothing?>): Boolean =
        _onNewHandler
            ?.takeIf { context.isApplicable() }
            ?.let {
                context.shouldStop = true
                it.invoke(context as StatefulContext<S, StateAccessor.Changing<S>, U, Nothing?>, null)
                context.shouldStop
            } ?: false

    @Suppress("UNCHECKED_CAST")
    internal suspend fun handleOnEdit(context: StatefulContext<*, StateAccessor.Changing<*>, *, MessageId>): Boolean =
        _onEditHandler
            ?.takeIf { context.isApplicable() }
            ?.let {
                context.shouldStop = true
                it.invoke(context as StatefulContext<S, StateAccessor.Changing<S>, U, MessageId>, null)
                context.shouldStop
            } ?: false
}
