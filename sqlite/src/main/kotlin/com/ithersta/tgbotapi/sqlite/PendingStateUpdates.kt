package com.ithersta.tgbotapi.sqlite

import org.jetbrains.exposed.dao.id.LongIdTable

internal object PendingStateUpdates : LongIdTable() {
    val chatId = long("chat_id")
    val state = binary("state")
}
