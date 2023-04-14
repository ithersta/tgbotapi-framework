package com.ithersta.sample

import com.ithersta.tgbotapi.basetypes.User
import com.ithersta.tgbotapi.core.GetUser
import org.koin.core.annotation.Single

data class DefaultUser(val id: Long) : User

@Single
fun getUser() = GetUser { DefaultUser(it.chatId) }
