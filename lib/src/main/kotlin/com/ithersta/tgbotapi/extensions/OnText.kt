package com.ithersta.tgbotapi.extensions

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.builders.StateSpecBuilder
import com.ithersta.tgbotapi.core.OnActionHandler
import dev.inmo.tgbotapi.types.message.content.TextMessage

public inline fun <R : Role, S : MessageState> StateSpecBuilder<R, S>.onText(
    value: String,
    crossinline handler: OnActionHandler<R, S, TextMessage>
): Unit = on<TextMessage> { message ->
    if (message.content.text == value) {
        handler(message)
    } else {
        fallthrough()
    }
}
