package com.ithersta.tgbotapi.builders

import com.ithersta.tgbotapi.FrameworkDslMarker
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.core.Handler
import com.ithersta.tgbotapi.core.MessageSpec
import com.ithersta.tgbotapi.core.StateChangeHandler
import com.ithersta.tgbotapi.persistence.PersistedMessage
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.message.abstracts.ChatEventMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

@FrameworkDslMarker
public class MessageSpecBuilder<U : User, S : MessageState> @PublishedApi internal constructor(
    private val priority: Int = 0,
    private val stateMapper: (MessageState) -> S?,
    private val userMapper: (User) -> U?
) {
    @PublishedApi
    internal val triggers: MutableList<MessageSpec.Trigger<S, U, *>> = mutableListOf()
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
            }
            state.persist(PersistedMessage(chat.id.chatId, messageId, state.snapshot, template.actions))
        }
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
            MessageSpec.Trigger(handler) { data ->
                runCatching {
                    val type = typeOf<Data>()
                    when {
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
    internal fun build(): MessageSpec<S, U> = MessageSpec(
        priority = priority,
        stateMapper = stateMapper,
        userMapper = userMapper,
        triggers = triggers,
        _onNewHandler = onNewHandler,
        _onEditHandler = onEditHandler
    )
}

public inline fun <reified U : User, reified S : MessageState> messageSpec(
    priority: Int = 0,
    block: MessageSpecBuilder<U, S>.() -> Unit
): MessageSpec<S, U> = MessageSpecBuilder(
    priority,
    stateMapper = { it as? S },
    userMapper = { it as? U }
).apply(block).build()
