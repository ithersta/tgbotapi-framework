package com.ithersta.sample

import com.ithersta.tgbotapi.autoconfigure.autoconfigure
import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.builders.messageSpec
import com.ithersta.tgbotapi.core.GetUser
import com.ithersta.tgbotapi.core.runInBehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.days

data class DefaultUser(val id: Long) : User

@Single
fun getUser() = GetUser { DefaultUser(it.chatId) }

@Serializable
data class CounterState(
    val number: Int = 0
) : MessageState {
    @Serializable
    object Plus : Action

    @Serializable
    object Minus : Action
}

@Single
fun counter() = messageSpec<DefaultUser, CounterState> {
    render {
        text = state.snapshot.number.toString()
        keyboard = flatInlineKeyboard {
            actionButton("-", CounterState.Minus)
            actionButton("+", CounterState.Plus)
        }
    }
    on<CounterState.Minus> {
        state.edit { copy(number = number - 1) }
    }
    on<CounterState.Plus> {
        state.edit { copy(number = number + 1) }
    }
}

@Single
fun commandHandler() = messageSpec<User, MessageState> {
    on<TextMessage> { message ->
        when (message.content.text) {
            "/counter" -> state.new { CounterState() }
            else -> return@on fallthrough()
        }
    }
}

@Single
fun doSomethingDaily() = runInBehaviourContext {
    launch {
        while (true) {
            delay(1.days)
        }
    }
}

suspend fun main() = autoconfigure()
