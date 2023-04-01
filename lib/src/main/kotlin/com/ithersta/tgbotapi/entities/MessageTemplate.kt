package com.ithersta.tgbotapi.entities

import com.ithersta.tgbotapi.persistence.PersistedAction
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList

internal class MessageTemplate(
    val entities: TextSourcesList,
    val keyboard: InlineKeyboardMarkup?,
    val actions: List<PersistedAction>
)
