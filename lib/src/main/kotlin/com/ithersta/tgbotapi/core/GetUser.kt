package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.basetypes.User
import dev.inmo.tgbotapi.types.UserId

public fun interface GetUser {
    public operator fun invoke(id: UserId): User
}
