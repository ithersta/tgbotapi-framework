package com.ithersta.tgbotapi.autoconfigure

import com.ithersta.tgbotapi.core.GetRole
import com.ithersta.tgbotapi.core.runner.StatefulRunner
import com.ithersta.tgbotapi.init.BotConfigurer
import com.ithersta.tgbotapi.init.plugins.EmptyStatePlugin
import com.ithersta.tgbotapi.init.plugins.Pagination
import com.ithersta.tgbotapi.init.startBot
import com.ithersta.tgbotapi.persistence.MessageRepository
import com.ithersta.tgbotapi.sqlite.SqliteMessageRepository
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.bot.settings.limiters.CommonLimiter
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.Koin
import org.koin.core.KoinApplication
import java.io.File

public fun BotConfigurer.autoconfigure(koin: Koin) {
    install(EmptyStatePlugin)
    install(Pagination)

    updateTransformers(DefaultUpdateTransformers)
    messageRepository { defaultMessageRepository(it) }
    addAll(koin.getAll<StatefulRunner>())
    addAll(koin.getAll<DialogueFlow>().flatMap { it.stateSpecs })
    getRole(koin.get<GetRole>())
    telegramBot(defaultTelegramBot())
}

public suspend fun KoinApplication.autoconfigureBot(
    configure: BotConfigurer.() -> Unit,
) {
    startBot {
        autoconfigure(koin)
        configure()
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun defaultMessageRepository(serializersModule: SerializersModule): MessageRepository {
    val protoBuf = ProtoBuf { this.serializersModule = serializersModule }
    return SqliteMessageRepository(protoBuf)
}

private fun defaultTelegramBot() = telegramBot(
    token = System.getenv()["TOKEN_FILE"]?.let { File(it).readText() } ?: System.getenv("TOKEN"),
) {
    requestsLimiter = CommonLimiter(lockCount = 30, regenTime = 1000)
    client = HttpClient(OkHttp)
}
