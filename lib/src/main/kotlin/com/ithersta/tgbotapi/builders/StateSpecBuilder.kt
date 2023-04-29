package com.ithersta.tgbotapi.builders

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.core.*
import com.ithersta.tgbotapi.persistence.PersistedMessage
import dev.inmo.tgbotapi.bot.exceptions.MessageIsNotModifiedException
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.abstracts.ChatEventMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

public typealias StateChangeHandlerReturningMessage<S, U, M, Data> =
        suspend HandlerContext<S, StateAccessor.Changing<S>, U, M>.(Data) -> Message

@FrameworkDslMarker
public class StateSpecBuilder<R : Role, S : MessageState> @PublishedApi internal constructor(
    private val priority: Int = 0,
    private val stateMapper: (MessageState) -> S?,
    private val roleMapper: (Role) -> R?,
    private val handleGlobalUpdates: Boolean,
) {
    @PublishedApi
    internal val triggers: MutableList<StateSpec.Trigger<S, R, *>> = mutableListOf()

    @PublishedApi
    internal val commands: MutableList<BotCommand> = mutableListOf()
    private var onNewHandler: StateChangeHandler<S, R, Nothing?, Nothing?>? = null
    private var onEditHandler: StateChangeHandler<S, R, MessageId, Nothing?>? = null

    public fun render(block: suspend PersistedMessageTemplateBuilder<S, R, *>.() -> Unit) {
        _onNew {
            val template = PersistedMessageTemplateBuilder(this).apply { block() }.build()
            val message = send(chat, template.entities, replyMarkup = template.keyboard ?: ReplyKeyboardRemove())
            state.persist(
                PersistedMessage(
                    chatId = chat.id.chatId,
                    messageId = message.messageId,
                    state = state.snapshot,
                    handleGlobalUpdates = handleGlobalUpdates,
                    actions = template.actions,
                ),
            )
        }
        _onEdit {
            val template = PersistedMessageTemplateBuilder(this).apply { block() }.build()
            runCatching {
                edit(
                    chatId = chat.id,
                    messageId = messageId,
                    entities = template.entities,
                    replyMarkup = template.keyboard,
                )
            }.onFailure { exception ->
                if (exception !is MessageIsNotModifiedException) {
                    throw exception
                }
            }
            state.persist(
                PersistedMessage(
                    chatId = chat.id.chatId,
                    messageId = messageId,
                    state = state.snapshot,
                    handleGlobalUpdates = handleGlobalUpdates,
                    actions = template.actions,
                ),
            )
        }
    }

    private fun _onNew(handler: StateChangeHandler<S, R, Nothing?, Nothing?>) {
        check(onNewHandler == null) { "Only one onNew block allowed" }
        onNewHandler = handler
    }

    private fun _onEdit(handler: StateChangeHandler<S, R, MessageId, Nothing?>) {
        check(onEditHandler == null) { "Only one onEdit block allowed" }
        onEditHandler = handler
    }

    public fun onNewOrEdit(handler: StateChangeHandlerReturningMessage<S, R, *, Nothing?>) {
        onNew(handler)
        onEdit(handler)
    }

    public fun onNew(handler: StateChangeHandlerReturningMessage<S, R, Nothing?, Nothing?>) {
        _onNew {
            val message = handler(null)
            state.persist(PersistedMessage(chat.id.chatId, message.messageId, state.snapshot, handleGlobalUpdates))
        }
    }

    public fun onEdit(handler: StateChangeHandlerReturningMessage<S, R, MessageId, Nothing?>) {
        _onEdit {
            val message = handler(null)
            state.persist(PersistedMessage(chat.id.chatId, message.messageId, state.snapshot, handleGlobalUpdates))
        }
    }

    public inline fun <reified Data : Any> on(noinline handler: Handler<S, R, MessageId, Data>) {
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
            },
        )
    }

    @PublishedApi
    internal fun build(): StateSpec<R, S> = StateSpec(
        priority = priority,
        stateMapper = stateMapper,
        roleMapper = roleMapper,
        triggers = triggers,
        _onNewHandler = onNewHandler,
        _onEditHandler = onEditHandler,
        commands = commands,
    )
}

/**
 * Constructs [StateSpec].
 *
 * @param R the type of user role this spec applies for.
 * @param S the type of message state this spec applies for.
 * @param priority the priority of this spec, a spec with
 * a bigger priority is called first.
 * @param handleGlobalUpdates if set to true, this spec
 * will handle updates that are not bound to the message (like `on<TextMessage>`)
 */
public inline fun <reified R : Role, reified S : MessageState> inState(
    priority: Int = 0,
    handleGlobalUpdates: Boolean = true,
    block: StateSpecBuilder<R, S>.() -> Unit,
): StateSpec<R, S> = StateSpecBuilder(
    priority,
    stateMapper = { it as? S },
    roleMapper = { it as? R },
    handleGlobalUpdates,
).apply(block).build()

/**
 * Constructs [StateSpec] handling a command.
 * Also adds the command to the command list.
 *
 * @param R the type of user role this command applies for.
 * @param text trigger text for the command, without `\`.
 * @param description description text for the command, will be visible in the command list.
 * @param priority the priority of this spec, a spec with
 * a bigger priority is called first.
 */
public inline fun <reified R : Role> command(
    text: String,
    description: String?,
    priority: Int = 100,
    crossinline handler: Handler<MessageState, R, MessageId, TextMessage>,
): StateSpec<R, MessageState> = inState<R, MessageState>(priority) {
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
