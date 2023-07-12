package com.ithersta.sample

import com.ithersta.sample.MultipleChoiceFlow.MultipleChoiceState.SelectAction
import com.ithersta.sample.MultipleChoiceFlow.MultipleChoiceState.UnselectAction
import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.builders.DialogueFlow
import com.ithersta.tgbotapi.builders.fromResources
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.utils.row
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Single
class MultipleChoiceFlow : DialogueFlow() {
    @Serializable
    data class MultipleChoiceState(
        val selectedClothes: Set<Clothes> = emptySet(),
    ) : MessageState {
        @Serializable
        class SelectAction(val clothes: Clothes) : Action

        @Serializable
        class UnselectAction(val clothes: Clothes) : Action
    }

    val multipleChoice = inState<DefaultRole, MultipleChoiceState> {
        render {
            text = "Что наденем?"
            photo = fromResources("/a.jpg")
            keyboard = inlineKeyboard {
                Clothes.entries.forEach { clothes ->
                    row {
                        if (clothes in state.snapshot.selectedClothes) {
                            actionButton("✅ ${clothes.name}", UnselectAction(clothes))
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

    val command = command<DefaultRole>("start", description = "начать") {
        state.new { MultipleChoiceState() }
        updateCommands()
    }
}
