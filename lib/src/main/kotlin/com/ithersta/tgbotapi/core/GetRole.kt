package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.basetypes.Role
import dev.inmo.tgbotapi.types.UserId

public fun interface GetRole {
    public operator fun invoke(id: UserId): Role
}
