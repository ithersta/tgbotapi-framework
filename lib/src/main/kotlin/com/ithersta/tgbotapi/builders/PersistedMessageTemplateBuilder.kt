package com.ithersta.tgbotapi.builders

import com.ithersta.tgbotapi.FrameworkDslMarker
import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.entities.MessageTemplate
import com.ithersta.tgbotapi.persistence.PersistedAction
import dev.inmo.tgbotapi.extensions.utils.formatting.toMarkdownTexts
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardRowBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regular

@FrameworkDslMarker
public class PersistedMessageTemplateBuilder {
    public var entities: List<TextSource> = emptyList()
    public var text: String
        get() = entities.toMarkdownTexts().joinToString("")
        set(value) {
            entities = buildEntities { regular(value) }
        }
    public var keyboard: InlineKeyboardMarkup? = null
    private val persistedActions = mutableListOf<PersistedAction>()

    public fun InlineKeyboardRowBuilder.actionButton(text: String, action: Action) {
        val persistedAction = PersistedAction.from(action)
        dataButton(text, persistedAction.key)
        persistedActions.add(persistedAction)
    }

    internal fun build() = MessageTemplate(entities, keyboard, persistedActions)
}
