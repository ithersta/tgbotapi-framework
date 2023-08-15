package com.ithersta.tgbotapi.message

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.persistence.PersistedAction
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardRowBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton

public interface ActionButtonContext {
    public fun InlineKeyboardRowBuilder.actionButton(text: String, action: Action)
    public val persistedActions: List<PersistedAction>
}

internal class ListActionButtonContext internal constructor() : ActionButtonContext {
    override val persistedActions = mutableListOf<PersistedAction>()

    override fun InlineKeyboardRowBuilder.actionButton(
        text: String,
        action: Action
    ) {
        val persistedAction = PersistedAction.from(action)
        dataButton(text, persistedAction.key)
        persistedActions.add(persistedAction)
    }
}