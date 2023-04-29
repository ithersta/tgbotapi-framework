package com.ithersta.tgbotapi.builders

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.core.StateAccessor
import com.ithersta.tgbotapi.core.HandlerContext
import com.ithersta.tgbotapi.persistence.PersistedAction
import dev.inmo.tgbotapi.extensions.utils.formatting.toMarkdownTexts
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardRowBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regular

internal class MessageTemplate(
    val entities: TextSourcesList,
    val keyboard: InlineKeyboardMarkup?,
    val actions: List<PersistedAction>,
)

@FrameworkDslMarker
public class PersistedMessageTemplateBuilder<S : MessageState, R : Role, M : MessageId?>(
    context: HandlerContext<S, StateAccessor.Changing<S>, R, M>,
) : HandlerContext<S, StateAccessor.Changing<S>, R, M> by context {
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
