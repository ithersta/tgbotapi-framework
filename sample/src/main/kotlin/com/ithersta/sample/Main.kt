package com.ithersta.sample

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.builders.messageSpec
import com.ithersta.tgbotapi.engines.regularEngine
import com.ithersta.tgbotapi.entities.Dispatcher
import com.ithersta.tgbotapi.sqlite.SqliteMessageRepository
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.row
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.protobuf.ProtoBuf

class DefaultUser : User

@Serializable
class DefaultAction : Action

fun commandHandler() = messageSpec<User, MessageState> {
    on<TextMessage> { message ->
        when (message.content.text) {
            "/start" -> state.new { MessageState.Empty }
            else -> return@on fallthrough()
        }
    }
}

fun emptyMessage() = messageSpec<DefaultUser, MessageState.Empty> {
    render {
        text = "message"
        keyboard = inlineKeyboard {
            row {
                actionButton("some action", DefaultAction())
            }
        }
    }
    on<DefaultAction> {
        state.new { MessageState.Empty }
    }
    on<TextMessage> { message ->
        state.new { MessageState.Empty }
    }
}

val s = SerializersModule {
    polymorphic(MessageState::class) {
        subclass(MessageState.Empty::class)
    }
    polymorphic(Action::class) {
        subclass(DefaultAction::class)
    }
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun main() {
    val dispatcher = Dispatcher(
        listOf(commandHandler(), emptyMessage()),
        SqliteMessageRepository(ProtoBuf { serializersModule = s }),
        getUser = { DefaultUser() })
    val engine = dispatcher.regularEngine()
    telegramBot(token = System.getenv("TOKEN")).buildBehaviourWithLongPolling {
        engine.block.invoke(this)
    }.join()
}
