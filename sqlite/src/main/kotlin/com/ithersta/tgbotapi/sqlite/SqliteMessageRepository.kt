package com.ithersta.tgbotapi.sqlite

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.persistence.MessageRepository
import com.ithersta.tgbotapi.persistence.PendingState
import com.ithersta.tgbotapi.persistence.PersistedMessage
import dev.inmo.tgbotapi.types.MessageId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * [MessageRepository] implementation based on SQLite.
 * Uses [protoBuf] to serialize states and actions.
 *
 * @param jdbc connection string
 * @param historyDepth amount of messages to keep
 */
@OptIn(ExperimentalSerializationApi::class)
public class SqliteMessageRepository(
    private val protoBuf: ProtoBuf,
    jdbc: String = "jdbc:sqlite:states.db",
    private val historyDepth: Int = 200,
) : MessageRepository {
    private val pendingUpdates = MutableSharedFlow<Unit>()

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
            cleanup(chatId = message.chatId, lastMessageId = message.messageId)
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

    override fun delete(chatId: Long, messageId: Long): Unit = transaction(db) {
        PersistedActions.deleteWhere {
            (PersistedActions.chatId eq chatId) and (PersistedActions.messageId eq messageId)
        }
        PersistedMessages.deleteWhere {
            (PersistedMessages.chatId eq chatId) and (PersistedMessages.messageId eq messageId)
        }
    }

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

    override suspend fun save(pendingState: PendingState.New) {
        val serializedState = protoBuf.encodeToByteArray<MessageState>(pendingState.state)
        transaction(db) {
            PendingStateUpdates.insert {
                it[chatId] = pendingState.chatId
                it[state] = serializedState
            }
        }
        pendingUpdates.emit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getPending(): Flow<PendingState> = merge(pendingUpdates, flowOf(Unit)).flatMapLatest {
        flow {
            generateSequence {
                transaction(db) {
                    PendingStateUpdates
                        .selectAll()
                        .limit(1)
                        .firstOrNull()
                }?.let {
                    PendingState(
                        id = it[PendingStateUpdates.id].value,
                        chatId = it[PendingStateUpdates.chatId],
                        state = runCatching {
                            protoBuf.decodeFromByteArray<MessageState>(it[PendingStateUpdates.state])
                        }.getOrNull()
                    )
                }
            }.forEach {
                emit(it)
                deletePending(it.id)
            }
        }
    }

    private fun deletePending(id: Long): Unit = transaction(db) {
        PendingStateUpdates.deleteWhere { PendingStateUpdates.id eq id }
    }

    private fun Transaction.cleanup(chatId: Long, lastMessageId: Long) {
        val lastKeptMessageId = lastMessageId - historyDepth
        PersistedActions.deleteWhere {
            (PersistedActions.chatId eq chatId) and (messageId less lastKeptMessageId)
        }
        PersistedMessages.deleteWhere {
            (PersistedMessages.chatId eq chatId) and (messageId less lastKeptMessageId)
        }
    }
}
