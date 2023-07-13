package com.ithersta.tgbotapi.init.plugins

import com.ithersta.tgbotapi.basetypes.MessageState
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

public val EmptyStatePlugin: Plugin = Plugin {
    add(SerializersModule {
        polymorphic(MessageState::class) {
            subclass(MessageState.Empty::class)
        }
    })
}