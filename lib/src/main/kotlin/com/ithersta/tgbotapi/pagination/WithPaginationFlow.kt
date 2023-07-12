package com.ithersta.tgbotapi.pagination

import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.builders.DialogueFlow

internal class WithPaginationFlow : DialogueFlow() {
    val withPagination = inState<Role, WithPagination<*>> {
        on<GoToPageAction> {
            state.edit { state.snapshot.withPage(it.page) }
        }
    }
}