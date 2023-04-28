package com.ithersta.sample

import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.core.GetRole
import org.koin.core.annotation.Single

data class DefaultRole(val id: Long) : Role

@Single
fun getRole() = GetRole { DefaultRole(it.chatId) }
