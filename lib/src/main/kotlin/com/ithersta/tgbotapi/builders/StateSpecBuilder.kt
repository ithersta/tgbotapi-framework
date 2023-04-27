package com.ithersta.tgbotapi.builders

import com.ithersta.tgbotapi.FrameworkDslMarker
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.core.Handler
import com.ithersta.tgbotapi.core.StateChangeHandler
import com.ithersta.tgbotapi.core.StateSpec
import com.ithersta.tgbotapi.persistence.PersistedMessage
import dev.inmo.tgbotapi.bot.exceptions.MessageIsNotModifiedException
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.message.abstracts.ChatEventMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

@FrameworkDslMarker
public class StateSpecBuilder<U : User, S : MessageState> @PublishedApi internal constructor(
    private val priority: Int = 0,
    private val stateMapper: (MessageState) -> S?,
    private val userMapper: (User) -> U?
) {
    @PublishedApi
    internal val triggers: MutableList<StateSpec.Trigger<S, U, *>> = mutableListOf()

    @PublishedApi
    internal val commands: MutableList<BotCommand> = mutableListOf()
    private var onNewHandler: StateChangeHandler<S, U, Nothing?, Nothing?>? = null
    private var onEditHandler: StateChangeHandler<S, U, MessageId, Nothing?>? = null

    public fun render(block: PersistedMessageTemplateBuilder<S, U, *>.() -> Unit) {
        onNew {
            val template = PersistedMessageTemplateBuilder(this).apply(block).build()
            val message = send(chat, template.entities, replyMarkup = template.keyboard)
            state.persist(PersistedMessage(chat.id.chatId, message.messageId, state.snapshot, template.actions))
        }
        onEdit {
            val template = PersistedMessageTemplateBuilder(this).apply(block).build()
            runCatching {
                edit(
                    chatId = chat.id,
                    messageId = messageId,
                    entities = template.entities,
                    replyMarkup = template.keyboard
                )
            }.onFailure { exception ->
                if (exception !is MessageIsNotModifiedException) {
                    throw exception
                }
            }
            state.persist(PersistedMessage(chat.id.chatId, messageId, state.snapshot, template.actions))
        }
    }

    public fun onNewOrEdit(handler: StateChangeHandler<S, U, *, Nothing?>) {
        onNew(handler)
        onEdit(handler)
    }

    public fun onNew(handler: StateChangeHandler<S, U, Nothing?, Nothing?>) {
        check(onNewHandler == null) { "Only one onNew block allowed" }
        onNewHandler = handler
    }

    public fun onEdit(handler: StateChangeHandler<S, U, MessageId, Nothing?>) {
        check(onEditHandler == null) { "Only one onEdit block allowed" }
        onEditHandler = handler
    }

    public inline fun <reified Data : Any> on(noinline handler: Handler<S, U, MessageId, Data>) {
        triggers.add(
            StateSpec.Trigger(handler) { data ->
                runCatching {
                    val type = typeOf<Data>()
                    when {
                        type.isSubtypeOf(Pair::class.starProjectedType) -> {
                            val pair = data as? Pair<*, *> ?: return@runCatching null
                            (pair as? Data)?.takeIf {
                                pair.first!!::class.starProjectedType.isSubtypeOf(type.arguments[0].type!!) &&
                                        pair.second!!::class.starProjectedType.isSubtypeOf(type.arguments[1].type!!)
                            }
                        }

                        type.isSubtypeOf(ContentMessage::class.starProjectedType) -> {
                            val message = data as? ContentMessage<*> ?: return@runCatching null
                            (message as? Data)?.takeIf {
                                message.content::class.starProjectedType.isSubtypeOf(type.arguments.first().type!!)
                            }
                        }

                        type.isSubtypeOf(ChatEventMessage::class.starProjectedType) -> {
                            val message = data as? ChatEventMessage<*> ?: return@runCatching null
                            (message as? Data)?.takeIf {
                                message.chatEvent::class.starProjectedType.isSubtypeOf(type.arguments.first().type!!)
                            }
                        }

                        else -> {
                            data as? Data
                        }
                    }
                }.getOrNull()
            }
        )
    }

    @PublishedApi
    internal fun build(): StateSpec<U, S> = StateSpec(
        priority = priority,
        stateMapper = stateMapper,
        userMapper = userMapper,
        triggers = triggers,
        _onNewHandler = onNewHandler,
        _onEditHandler = onEditHandler,
        commands = commands
    )
}

public inline fun <reified U : User, reified S : MessageState> inState(
    priority: Int = 0,
    block: StateSpecBuilder<U, S>.() -> Unit
): StateSpec<U, S> = StateSpecBuilder(
    priority,
    stateMapper = { it as? S },
    userMapper = { it as? U }
).apply(block).build()

public inline fun <reified U : User> command(
    text: String,
    description: String?,
    priority: Int = 100,
    crossinline handler: Handler<MessageState, U, MessageId, TextMessage>
): StateSpec<U, MessageState> = inState<U, MessageState>(priority) {
    require(text.startsWith("/").not()) { "Command must not start with '/'" }
    val trigger = "/$text"
    description?.let { commands.add(BotCommand(text, it)) }
    on<TextMessage> {
        if (it.content.text == trigger) {
            handler(this, it)
        } else {
            fallthrough()
        }
    }
}
