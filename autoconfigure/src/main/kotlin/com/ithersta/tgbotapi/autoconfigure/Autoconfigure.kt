package com.ithersta.tgbotapi.autoconfigure

import com.ithersta.tgbotapi.core.BehaviourContextRunner
import com.ithersta.tgbotapi.core.Dispatcher
import com.ithersta.tgbotapi.core.GetUser
import com.ithersta.tgbotapi.core.MessageSpec
import com.ithersta.tgbotapi.engines.regularEngine
import com.ithersta.tgbotapi.persistence.MessageRepository
import com.ithersta.tgbotapi.sqlite.SqliteMessageRepository
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.bot.settings.limiters.CommonLimiter
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import java.io.File

suspend fun autoconfigure(serializersModule: SerializersModule, module: Module) {
    val koin = startKoin { modules(module) }.koin
    val messageRepository = koin.getOrNull<MessageRepository>() ?: defaultMessageRepository(serializersModule)
    val getUser = koin.get<GetUser>()
    val messageSpecs = koin.getAll<MessageSpec<*, *>>()
    val telegramBot = koin.getOrNull<TelegramBot>() ?: defaultTelegramBot()
    val behaviourContextRunners = koin.getAll<BehaviourContextRunner>()
    val dispatcher = Dispatcher(
        messageSpecs = messageSpecs,
        messageRepository = messageRepository,
        getUser = getUser
    )
    telegramBot.buildBehaviourWithLongPolling {
        behaviourContextRunners.forEach { it.block(this) }
        dispatcher.regularEngine().block(this)
    }.join()
}

@OptIn(ExperimentalSerializationApi::class)
private fun defaultMessageRepository(serializersModule: SerializersModule): MessageRepository {
    val protoBuf = ProtoBuf { this.serializersModule = serializersModule }
    return SqliteMessageRepository(protoBuf)
}

private fun defaultTelegramBot() = telegramBot(
    token = System.getenv()["TOKEN_FILE"]?.let { File(it).readText() } ?: System.getenv("TOKEN")
) {
    requestsLimiter = CommonLimiter(lockCount = 30, regenTime = 1000)
    client = HttpClient(OkHttp)
}
