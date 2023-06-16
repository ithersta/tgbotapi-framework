package com.ithersta.tgbotapi.extensions

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.builders.StateSpecBuilder
import com.ithersta.tgbotapi.core.OnActionHandler
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.types.message.textsources.BotCommandTextSource
import dev.inmo.tgbotapi.types.message.textsources.RegularTextSource
import io.ktor.http.*

public inline fun <R : Role, S : MessageState> StateSpecBuilder<R, S>.onDeeplink(
    crossinline handler: OnActionHandler<R, S, String>
): Unit = on<TextMessage> { message ->
    runCatching {
        val textSources = message.content.textSources
        require(textSources.size == 2)
        val commandTextSource = textSources[0]
        require(commandTextSource is BotCommandTextSource)
        require(commandTextSource.command == "start")
        val payload = textSources[1]
        require(payload is RegularTextSource)
        payload.source.removePrefix(" ").decodeURLQueryComponent()
    }.onFailure {
        fallthrough()
    }.onSuccess { payload ->
        handler(payload)
    }
}
