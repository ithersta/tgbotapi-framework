package com.ithersta.tgbotapi.core

import arrow.resilience.Schedule
import arrow.resilience.retryOrElse
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.core.runner.StatefulRunnerContext
import com.ithersta.tgbotapi.persistence.MessageRepository
import com.ithersta.tgbotapi.persistence.PendingState
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.commands.BotCommandScope
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class Dispatcher(
    stateSpecs: List<StateSpec<*, *>>,
    private val messageRepository: MessageRepository,
    private val updateTransformers: UpdateTransformers,
    private val getRole: GetRole,
    private val pendingStateDelay: Duration = 0.2.seconds,
) {
    private val stateSpecs = stateSpecs.sortedByDescending { it.priority }
    private val unboundStateAccessor: UnboundStateAccessor =
        UnboundStateAccessorImpl { chatId, state -> messageRepository.save(PendingState.New(chatId.chatId, state)) }
    public val BehaviourContext.statefulRunnerContext: StatefulRunnerContext
        get() = StatefulRunnerContext(this, unboundStateAccessor)

    public fun BehaviourContext.applyPendingStates(): Job = launch {
        messageRepository.getPending().collect { pendingState ->
            delay(pendingStateDelay)
            if (pendingState.state != null) {
                (Schedule.exponential<Throwable>(pendingStateDelay) and Schedule.recurs(3)).retryOrElse({
                    val chat = getChat(pendingState.chatId.toChatId())
                    val getRole: suspend () -> Role = { getRole(UserId(pendingState.chatId)) }
                    val stateAccessor = StateAccessor.Changing(
                        snapshot = pendingState.state,
                        new = { handleStateChange(chat, getRole, null, it, ::handleOnNew) },
                        persist = { messageRepository.save(it) },
                        unboundStateAccessor = unboundStateAccessor
                    )
                    val context = HandlerContextImpl(bot, stateAccessor, chat, null, getRole()) {
                        updateCommands(chat.id, getRole)
                    }
                    handleOnNew(context)
                }) { exception, _ ->
                    System.err.println("Couldn't apply pending state for chat ${pendingState.chatId}")
                    exception.printStackTrace(System.err)
                }
            }
        }
    }

    public suspend fun BehaviourContext.handle(update: Update): Unit = with(updateTransformers) {
        val chat = update.toChat() ?: return
        val getRole: suspend () -> Role = update.toUserId()?.let { { getRole(it) } } ?: return
        val chatId = chat.id.chatId
        val (state, messageId) = update.toMessageId()
            ?.let { messageId -> (messageRepository.get(chatId, messageId) ?: MessageState.Empty) to messageId }
            ?: messageRepository.getLast(chatId)
            ?: (MessageState.Empty to -1L)
        val data = update.toData()
        val actionKey = update.toActionKey()
        val action = actionKey?.let { messageRepository.getAction(chatId, messageId, it) }
        val stateAccessor = StateAccessor.Static(
            snapshot = state,
            edit = { handleStateChange(chat, getRole, messageId, it, ::handleOnEdit) },
            delete = {
                messageRepository.delete(chatId, messageId)
                delete(chat, messageId)
            },
            new = { handleStateChange(chat, getRole, null, it, ::handleOnNew) },
            unboundStateAccessor = unboundStateAccessor
        )
        val context = HandlerContextImpl(bot, stateAccessor, chat, messageId, getRole()) {
            updateCommands(chat.id, getRole())
        }
        handle(context, update.onSuccess(), listOfNotNull(data, action, action?.to(data)))
    }

    private suspend fun BehaviourContext.updateCommands(chatId: ChatIdentifier, role: Role) {
        val scope = BotCommandScope.Chat(chatId)
        setMyCommands(stateSpecs.flatMap { it.commands(role) }, scope)
    }

    private suspend fun <M : MessageId?> BehaviourContext.handleStateChange(
        chat: Chat,
        getRole: suspend () -> Role,
        messageId: M,
        state: MessageState,
        handle: suspend (HandlerContextImpl<*, StateAccessor.Changing<*>, *, M>) -> Message?,
    ): Message? {
        val stateAccessor = StateAccessor.Changing(
            snapshot = state,
            new = { handleStateChange(chat, getRole, null, it, ::handleOnNew) },
            persist = { messageRepository.save(it) },
            unboundStateAccessor = unboundStateAccessor
        )
        val context = HandlerContextImpl(bot, stateAccessor, chat, messageId, getRole()) {
            updateCommands(chat.id, getRole)
        }
        return handle(context)
    }

    private suspend fun handle(
        context: HandlerContextImpl<*, StateAccessor.Static<*>, *, MessageId>,
        onSuccess: OnSuccess?,
        data: List<Any>,
    ) = stateSpecs.any { it.handle(context, onSuccess, data) }

    private suspend fun handleOnEdit(context: HandlerContextImpl<*, StateAccessor.Changing<*>, *, MessageId>) =
        stateSpecs.firstNotNullOfOrNull { it.handleOnEdit(context) }

    private suspend fun handleOnNew(context: HandlerContextImpl<*, StateAccessor.Changing<*>, *, Nothing?>) =
        stateSpecs.firstNotNullOfOrNull { it.handleOnNew(context) }

    public interface UpdateTransformers {
        public fun Update.toData(): Any
        public fun Update.toChat(): Chat?
        public fun Update.toMessageId(): MessageId?
        public fun Update.toUserId(): UserId?
        public fun Update.toActionKey(): String?
        public fun Update.onSuccess(): (suspend TelegramBot.() -> Unit)?
    }
}
