package com.ithersta.tgbotapi.builders

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.core.OnActionHandler
import com.ithersta.tgbotapi.core.OnEditHandler
import com.ithersta.tgbotapi.core.OnNewHandler
import com.ithersta.tgbotapi.core.StateSpec
import com.ithersta.tgbotapi.persistence.PersistedMessage
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.message.abstracts.ChatEventMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

@FrameworkDslMarker
public class StateSpecBuilder<R : Role, S : MessageState> @PublishedApi internal constructor(
    private val priority: Int = 0,
    private val stateMapper: (MessageState) -> S?,
    private val roleMapper: (Role) -> R?,
    private val preferredHandleGlobalUpdates: Boolean,
) {
    @PublishedApi
    internal val triggers: MutableList<StateSpec.Trigger<S, R, *>> = mutableListOf()

    @PublishedApi
    internal var hasNonActionTrigger: Boolean = false

    public val commands: MutableList<BotCommand> = mutableListOf()
    private var onNewHandler: OnNewHandler<R, S>? = null
    private var onEditHandler: OnEditHandler<R, S>? = null
    private val handleGlobalUpdates get() = preferredHandleGlobalUpdates && hasNonActionTrigger

    public fun render(block: suspend PersistedMessageTemplateBuilder<S, R, *>.() -> Unit) {
        _onNew {
            val template = PersistedMessageTemplateBuilder(this).apply { block() }.build()
            val message = send(chat, template)
            state.persist(
                PersistedMessage(
                    chatId = chat.id.chatId,
                    messageId = message.messageId,
                    state = state.snapshot,
                    handleGlobalUpdates = this@StateSpecBuilder.handleGlobalUpdates,
                    actions = template.actions,
                ),
            )
            message
        }
        _onEdit {
            val template = PersistedMessageTemplateBuilder(this).apply { block() }.build()
            val message = edit(chat, messageId, template)
            state.persist(
                PersistedMessage(
                    chatId = chat.id.chatId,
                    messageId = messageId,
                    state = state.snapshot,
                    handleGlobalUpdates = this@StateSpecBuilder.handleGlobalUpdates,
                    actions = template.actions,
                ),
            )
            message
        }
    }

    private fun _onNew(handler: OnNewHandler<R, S>) {
        check(onNewHandler == null) { "Only one onNew block allowed" }
        onNewHandler = handler
    }

    private fun _onEdit(handler: OnEditHandler<R, S>) {
        check(onEditHandler == null) { "Only one onEdit block allowed" }
        onEditHandler = handler
    }

    public fun onNewOrEdit(handler: OnNewHandler<R, S>) {
        onNew(handler)
        onEdit(handler)
    }

    public fun onNew(handler: OnNewHandler<R, S>) {
        _onNew {
            handler().also {
                state.persist(
                    PersistedMessage(
                        chat.id.chatId, it.messageId, state.snapshot,
                        this@StateSpecBuilder.handleGlobalUpdates
                    )
                )
            }
        }
    }

    public fun onEdit(handler: OnEditHandler<R, S>) {
        _onEdit {
            handler().also {
                state.persist(
                    PersistedMessage(
                        chat.id.chatId, it.messageId, state.snapshot,
                        this@StateSpecBuilder.handleGlobalUpdates
                    )
                )
            }
        }
    }

    public inline fun <reified Data : Any> on(noinline handler: OnActionHandler<R, S, Data>) {
        if (!Data::class.isSubclassOf(Action::class)) {
            hasNonActionTrigger = true
        }
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
public inline fun <reified R : Role, reified S : MessageState> StateSpec(
    priority: Int = 0,
    handleGlobalUpdates: Boolean = true,
    block: StateSpecBuilder<R, S>.() -> Unit,
): StateSpec<R, S> = StateSpecBuilder(
    priority,
    stateMapper = { it as? S },
    roleMapper = { it as? R },
    handleGlobalUpdates,
).apply(block).build()
