package com.ithersta.sample

import com.ithersta.sample.MultipleChoiceState.SelectAction
import com.ithersta.sample.MultipleChoiceState.UnselectAction
import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.builders.messageSpec
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.row
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Serializable
data class MultipleChoiceState(
    val selectedClothes: Set<Clothes> = emptySet()
) : MessageState {
    @Serializable
    class SelectAction(val clothes: Clothes) : Action

    @Serializable
    class UnselectAction(val clothes: Clothes) : Action
}

@Single
fun multipleChoice() = messageSpec<DefaultUser, MultipleChoiceState> {
    render {
        text = "Что наденем?"
        keyboard = inlineKeyboard {
            Clothes.values().forEach { clothes ->
                row {
                    if (clothes in state.snapshot.selectedClothes) {
                        actionButton("✅${clothes.name}", UnselectAction(clothes))
                    } else {
                        actionButton(clothes.name, SelectAction(clothes))
                    }
                }
            }
        }
    }
    on<SelectAction> {
        state.edit { copy(selectedClothes = selectedClothes + it.clothes) }
    }
    on<UnselectAction> {
        state.edit { copy(selectedClothes = selectedClothes - it.clothes) }
    }
}

@Single
fun commands() = messageSpec<DefaultUser, MessageState.Empty>(priority = 10) {
    on<TextMessage> {
        state.new { MultipleChoiceState() }
    }
}
