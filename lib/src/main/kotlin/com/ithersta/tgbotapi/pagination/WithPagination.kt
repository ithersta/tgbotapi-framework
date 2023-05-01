package com.ithersta.tgbotapi.pagination

import com.ithersta.tgbotapi.basetypes.MessageState

public interface WithPagination<T> where T : WithPagination<T>, T : MessageState {
    public val page: Int
    public fun withPage(page: Int) : T
}
