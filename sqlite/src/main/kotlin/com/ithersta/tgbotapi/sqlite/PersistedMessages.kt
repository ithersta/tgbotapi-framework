package com.ithersta.tgbotapi.sqlite

import org.jetbrains.exposed.sql.Table

internal object PersistedMessages : Table() {
    val chatId = long("chat_id")
    val messageId = long("message_id")
    val state = binary("state")

    override val primaryKey = PrimaryKey(chatId, messageId)
}
