package com.ithersta.tgbotapi.autoconfigure

import com.ithersta.tgbotapi.core.Dispatcher
import com.ithersta.tgbotapi.core.GetRole
import com.ithersta.tgbotapi.core.StateSpec
import com.ithersta.tgbotapi.core.runner.StatefulRunner
import com.ithersta.tgbotapi.engines.regularEngine
import com.ithersta.tgbotapi.persistence.MessageRepository
import com.ithersta.tgbotapi.sqlite.SqliteMessageRepository
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.bot.settings.limiters.CommonLimiter
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.KoinApplication
import java.io.File
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

suspend fun KoinApplication.autoconfigure(serializersModule: SerializersModule) {
    val messageRepository = koin.getOrNull<MessageRepository>() ?: defaultMessageRepository(serializersModule)
    val getRole = koin.get<GetRole>()
    val dialogueFlows = koin.getAll<DialogueFlow>()
    val stateSpecs = koin.getAll<StateSpec<*, *>>() + dialogueFlows.flatMap { flow ->
        flow::class.declaredMemberProperties
            .filter { it.returnType.isSubtypeOf(StateSpec::class.starProjectedType) }
            .map { it.getter.call(flow) as StateSpec<*, *> }
    }
    val telegramBot = koin.getOrNull<TelegramBot>() ?: defaultTelegramBot()
    val runners = koin.getAll<StatefulRunner>()
    val updateTransformers = koin.getOrNull<Dispatcher.UpdateTransformers>() ?: DefaultUpdateTransformers
    Dispatcher(
        stateSpecs = stateSpecs,
        messageRepository = messageRepository,
        updateTransformers = updateTransformers,
        getRole = getRole,
    ).run {
        telegramBot.buildBehaviourWithLongPolling {
            runners.forEach { it.block(statefulRunnerContext) }
            regularEngine().block(statefulRunnerContext)
        }.join()
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
}
