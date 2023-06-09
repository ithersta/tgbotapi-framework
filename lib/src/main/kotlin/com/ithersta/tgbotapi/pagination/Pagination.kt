package com.ithersta.tgbotapi.pagination

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.builders.PersistedMessageTemplateBuilder
import com.ithersta.tgbotapi.builders.inState
import com.ithersta.tgbotapi.core.HandlerContext
import com.ithersta.tgbotapi.core.StateAccessor
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardBuilder
import dev.inmo.tgbotapi.utils.row

public val <S> HandlerContext<S, out StateAccessor<S>, *, *>.limit: Int
        where S : WithPagination<S>, S : MessageState
    get() = state.snapshot.limit

public val <S> HandlerContext<S, out StateAccessor<S>, *, *>.offset: Int
        where S : WithPagination<S>, S : MessageState
    get() = state.snapshot.page * limit

/**
 * Creates a navigation row with a [previous] page button, a [next] page button and a page counter.
 *
 * @param itemCount count of all items.
 * @param previous the text on a previous page button.
 * @param next the text on a next page button.
 */
context (InlineKeyboardBuilder)
public fun <S> PersistedMessageTemplateBuilder<S, *, *>.navigationRow(
    itemCount: Int, previous: String = "⬅️", next: String = "➡️"
) where S : WithPagination<S>, S : MessageState {
    val page = state.snapshot.page
    val maxPage = ((itemCount - 1) / limit).coerceAtLeast(0)
    if (maxPage == 0 && page == 0) return
    fun goToPage(newPage: Int) = GoToPageAction(newPage.coerceIn(0, maxPage))
    row {
        actionButton(if (page > 0) previous else " ", goToPage(page - 1))
        actionButton("${page + 1}/${maxPage + 1}", goToPage(page))
        actionButton(if (page < maxPage) next else " ", goToPage(page + 1))
    }
}

internal val withPaginationSpec = inState<Role, WithPagination<*>> {
    on<GoToPageAction> {
        state.edit { state.snapshot.withPage(it.page) }
    }
}
