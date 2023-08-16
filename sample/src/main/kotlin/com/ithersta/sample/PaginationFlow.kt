package com.ithersta.sample

import com.ithersta.tgbotapi.autoconfigure.DialogueFlow
import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.init.plugins.WithPagination
import com.ithersta.tgbotapi.init.plugins.limit
import com.ithersta.tgbotapi.init.plugins.navigationRow
import com.ithersta.tgbotapi.init.plugins.offset
import com.ithersta.tgbotapi.message.template.text
import dev.inmo.tgbotapi.utils.row
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

private val numbers = (1..100)

@Single
class PaginationFlow : DialogueFlow() {
    val command = command<Role>("pagination", description = "Пагинация") {
        state.new { SamplePaginationState(0) }
    }

    @Serializable
    data class SamplePaginationState(
        override val page: Int,
    ) : MessageState, WithPagination<SamplePaginationState> {
        override fun withPage(page: Int) = copy(page = page)

        @Serializable
        object SampleAction : Action
    }

    val samplePagination = inState<Role, SamplePaginationState> {
        message {
            text("Числа от 1 до 100") {
                numbers.drop(offset).take(limit).forEach { number ->
                    row {
                        actionButton(number.toString(), SamplePaginationState.SampleAction)
                    }
                }
                navigationRow(itemCount = numbers.count())
            }
        }
    }
}
