package com.ithersta.tgbotapi.sqlite

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.persistence.MessageRepository
import com.ithersta.tgbotapi.persistence.PersistedMessage
import dev.inmo.tgbotapi.types.MessageId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@OptIn(ExperimentalSerializationApi::class)
public class SqliteMessageRepository(
    private val protoBuf: ProtoBuf,
    jdbc: String = "jdbc:sqlite:states.db",
) : MessageRepository {
    private val db = Database.connect(jdbc, "org.sqlite.JDBC").also {
        transaction(it) {
            SchemaUtils.createMissingTablesAndColumns(PersistedMessages, PersistedActions, PendingStateUpdates)
        }
    }

    override fun save(message: PersistedMessage) {
        val serializedState = protoBuf.encodeToByteArray<MessageState>(message.state)
        val serializedActions = message.actions.map { it.key to protoBuf.encodeToByteArray<Action>(it.action) }
        transaction(db) {
            PersistedActions.deleteWhere {
                (chatId eq message.chatId) and (messageId eq message.messageId)
            }
            PersistedMessages.replace {
                it[chatId] = message.chatId
                it[messageId] = message.messageId
                it[state] = serializedState
                it[handleGlobalUpdates] = message.handleGlobalUpdates
            }
            PersistedActions.batchInsert(serializedActions) { (key, action) ->
                this[PersistedActions.chatId] = message.chatId
                this[PersistedActions.messageId] = message.messageId
                this[PersistedActions.key] = key
                this[PersistedActions.action] = action
            }
        }
    }

    override fun get(chatId: Long, messageId: Long): MessageState? = transaction(db) {
        PersistedMessages
            .slice(PersistedMessages.state)
            .select { (PersistedMessages.chatId eq chatId) and (PersistedMessages.messageId eq messageId) }
            .firstOrNull()?.get(PersistedMessages.state)
    }?.runCatching {
        protoBuf.decodeFromByteArray<MessageState>(this)
    }?.getOrNull()

    override fun getLast(chatId: Long): Pair<MessageState, MessageId>? = transaction(db) {
        PersistedMessages
            .slice(PersistedMessages.state, PersistedMessages.messageId)
            .select { (PersistedMessages.chatId eq chatId) and PersistedMessages.handleGlobalUpdates }
            .orderBy(PersistedMessages.messageId, SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.let { it[PersistedMessages.state] to it[PersistedMessages.messageId] }
    }?.runCatching {
        protoBuf.decodeFromByteArray<MessageState>(first) to second
    }?.getOrNull()

    override fun getAction(chatId: Long, messageId: Long, key: String): Action? = transaction(db) {
        PersistedActions
            .slice(PersistedActions.action)
            .select {
                (PersistedActions.chatId eq chatId) and
                        (PersistedActions.messageId eq messageId) and
                        (PersistedActions.key eq key)
            }
            .firstOrNull()
            ?.get(PersistedActions.action)
    }?.runCatching {
        protoBuf.decodeFromByteArray<Action>(this)
    }?.getOrNull()

    override fun addPending(chatId: Long, state: MessageState) {
        val serializedState = protoBuf.encodeToByteArray<MessageState>(state)
        transaction(db) {
            PendingStateUpdates.insert {
                it[PendingStateUpdates.chatId] = chatId
                it[PendingStateUpdates.state] = serializedState
            }
        }
    }
}
