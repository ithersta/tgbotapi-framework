package com.ithersta.sample

import com.ithersta.tgbotapi.autoconfigure.DialogueFlow
import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.builders.command
import com.ithersta.tgbotapi.builders.inState
import com.ithersta.tgbotapi.pagination.Pagination
import com.ithersta.tgbotapi.pagination.WithPagination
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.utils.row
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

private val numbers = (0..100)

@Single
class PaginationFlow : DialogueFlow {
    val command = command<Role>("pagination", description = "Пагинация") {
        state.new { SamplePaginationState(0) }
    }

    @Serializable
    data class SamplePaginationState(
        override val page: Int
    ) : MessageState, WithPagination<SamplePaginationState> {
        override fun withPage(page: Int) = copy(page = page)

        @Serializable
        object SampleAction : Action
    }

    val samplePagination = inState<Role, SamplePaginationState> {
        val pagination = Pagination()
        render {
            text = "Пагинация"
            keyboard = inlineKeyboard {
                numbers.drop(pagination.offset).take(pagination.limit).forEach { number ->
                    row {
                        actionButton(number.toString(), SamplePaginationState.SampleAction)
                    }
                }
                pagination.navigationRow(itemCount = numbers.count())
            }
        }
    }
}
