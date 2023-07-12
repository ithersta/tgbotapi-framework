package com.ithersta.tgbotapi.builders

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.core.OnActionHandler
import com.ithersta.tgbotapi.core.StateSpec
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.message.content.TextMessage

public abstract class DialogueFlow {
    private val _stateSpecs = mutableListOf<StateSpec<*, *>>()
    public val stateSpecs: List<StateSpec<*, *>> = _stateSpecs

    public fun add(stateSpec: StateSpec<*, *>) {
        _stateSpecs.add(stateSpec)
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
    protected inline fun <reified R : Role, reified S : MessageState> inState(
        priority: Int = 0,
        handleGlobalUpdates: Boolean = true,
        block: StateSpecBuilder<R, S>.() -> Unit,
    ): StateSpec<R, S> = StateSpecBuilder(
        priority,
        stateMapper = { it as? S },
        roleMapper = { it as? R },
        handleGlobalUpdates,
    )
        .apply(block)
        .build()
        .also { add(it) }

    /**
     * Constructs [StateSpec] handling a command.
     * Also adds the command to the command list.
     *
     * @param R the type of user role this command applies for.
     * @param text trigger text for the command, without `/`.
     * @param description description text for the command, will be visible in the command list.
     * @param priority the priority of this spec, a spec with
     * a bigger priority is called first.
     */
    protected inline fun <reified R : Role> command(
        text: String,
        description: String?,
        priority: Int = 100,
        crossinline handler: OnActionHandler<R, MessageState, TextMessage>,
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
}
