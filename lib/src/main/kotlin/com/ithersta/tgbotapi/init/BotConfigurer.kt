package com.ithersta.tgbotapi.init

import com.ithersta.tgbotapi.core.Dispatcher
import com.ithersta.tgbotapi.core.GetRole
import com.ithersta.tgbotapi.core.StateSpec
import com.ithersta.tgbotapi.core.runner.StatefulRunner
import com.ithersta.tgbotapi.engines.regularEngine
import com.ithersta.tgbotapi.init.plugins.Plugin
import com.ithersta.tgbotapi.persistence.MessageRepository
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith

public class BotConfigurer internal constructor() {
    internal class Result(
        val stateSpecs: List<StateSpec<*, *>>,
        val runners: List<StatefulRunner>,
        val messageRepository: MessageRepository,
        val updateTransformers: Dispatcher.UpdateTransformers,
        val getRole: GetRole,
        val telegramBot: TelegramBot,
    )

    private val stateSpecs = mutableSetOf<StateSpec<*, *>>()
    private val runners = mutableSetOf<StatefulRunner>()
    private var serializersModule = SerializersModule { }
    private var messageRepository: ((SerializersModule) -> MessageRepository)? = null
    private var updateTransformers: Dispatcher.UpdateTransformers? = null
    private var getRole: GetRole? = null
    private var telegramBot: TelegramBot? = null

    public fun install(plugin: Plugin) {
        with(plugin) { install() }
    }

    public fun messageRepository(get: (SerializersModule) -> MessageRepository) {
        messageRepository = get
    }

    public fun updateTransformers(updateTransformers: Dispatcher.UpdateTransformers) {
        this.updateTransformers = updateTransformers
    }

    public fun getRole(getRole: GetRole) {
        this.getRole = getRole
    }

    public fun telegramBot(telegramBot: TelegramBot) {
        this.telegramBot = telegramBot
    }

    public fun serializersModule(serializersModule: SerializersModule) {
        this.serializersModule = this.serializersModule.overwriteWith(serializersModule)
    }

    public fun add(stateSpec: StateSpec<*, *>): Boolean = stateSpecs.add(stateSpec)

    public fun add(runner: StatefulRunner): Boolean = runners.add(runner)

    @JvmName("addAllStateSpecs")
    public fun addAll(stateSpecs: List<StateSpec<*, *>>): Boolean = this.stateSpecs.addAll(stateSpecs)

    @JvmName("addAllRunners")
    public fun addAll(runners: List<StatefulRunner>): Boolean = this.runners.addAll(runners)

    internal fun finalize(): Result {
        val messageRepository = requireNotNull(messageRepository) { "messageRepository is not set" }
        return Result(
            stateSpecs = stateSpecs.toList(),
            runners = runners.toList(),
            messageRepository = messageRepository(serializersModule),
            updateTransformers = requireNotNull(updateTransformers) { "updateTransformers is not set" },
            getRole = requireNotNull(getRole) { "getRole is not set" },
            telegramBot = requireNotNull(telegramBot) { "telegramBot is not set" }
        )
    }
}

public suspend fun startBot(configure: BotConfigurer.() -> Unit) {
    val config = BotConfigurer().apply(configure).finalize()
    Dispatcher(
        stateSpecs = config.stateSpecs,
        messageRepository = config.messageRepository,
        updateTransformers = config.updateTransformers,
        getRole = config.getRole,
    ).run {
        config.telegramBot.buildBehaviourWithLongPolling {
            config.runners.forEach { it.block(statefulRunnerContext) }
            regularEngine().block(statefulRunnerContext)
        }.join()
    }
}