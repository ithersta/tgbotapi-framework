package com.ithersta.sample

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.builders.messageSpec
import com.ithersta.tgbotapi.engines.regularEngine
import com.ithersta.tgbotapi.entities.Dispatcher
import com.ithersta.tgbotapi.sqlite.SqliteMessageRepository
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.protobuf.ProtoBuf

class DefaultUser : User

@Serializable
data class CounterState(
    val number: Int = 0
) : MessageState {
    @Serializable
    object Plus : Action

    @Serializable
    object Minus : Action
}

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

fun commandHandler() = messageSpec<User, MessageState> {
    on<TextMessage> { message ->
        when (message.content.text) {
            "/counter" -> state.new { CounterState() }
            else -> return@on fallthrough()
        }
    }
}

val s = SerializersModule {
    polymorphic(MessageState::class) {
        subclass(MessageState.Empty::class)
        subclass(CounterState::class)
    }
    polymorphic(Action::class) {
        subclass(CounterState.Plus::class)
        subclass(CounterState.Minus::class)
    }
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun main() {
    val dispatcher = Dispatcher(
        listOf(commandHandler(), counter()),
        SqliteMessageRepository(ProtoBuf { serializersModule = s }),
        getUser = { DefaultUser() })
    val engine = dispatcher.regularEngine()
    telegramBot(token = System.getenv("TOKEN")).buildBehaviourWithLongPolling {
        engine.block.invoke(this)
    }.join()
}
