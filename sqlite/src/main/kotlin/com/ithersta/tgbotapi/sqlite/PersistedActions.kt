package com.ithersta.tgbotapi.sqlite

import org.jetbrains.exposed.sql.Table

internal object PersistedActions : Table() {
    val chatId = long("chat_id")
    val messageId = long("message_id")
    val key = char("key", length = 64)
    val action = binary("action")

    override val primaryKey = PrimaryKey(chatId, messageId, key)
}
