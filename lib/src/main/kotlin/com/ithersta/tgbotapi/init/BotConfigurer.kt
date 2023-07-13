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
import kotlinx.serialization.modules.plus

public class BotConfigurer internal constructor(
    telegramBot: TelegramBot,
) : TelegramBot by telegramBot {
    internal class Result(
        val stateSpecs: List<StateSpec<*, *>>,
        val runners: List<StatefulRunner>,
        val serializersModule: SerializersModule,
    )

    private val plugins = mutableSetOf<Plugin>()
    private val stateSpecs = mutableSetOf<StateSpec<*, *>>()
    private val runners = mutableSetOf<StatefulRunner>()
    private var serializersModule = SerializersModule { }

    public fun install(plugin: Plugin): Boolean = plugins.add(plugin)
    public fun add(stateSpec: StateSpec<*, *>): Boolean = stateSpecs.add(stateSpec)
    public fun add(runner: StatefulRunner): Boolean = runners.add(runner)
    public fun add(serializersModule: SerializersModule) {
        this.serializersModule = this.serializersModule + serializersModule
    }

    @JvmName("addAllStateSpecs")
    public fun addAll(stateSpecs: List<StateSpec<*, *>>): Boolean = this.stateSpecs.addAll(stateSpecs)

    @JvmName("addAllRunners")
    public fun addAll(runners: List<StatefulRunner>): Boolean = this.runners.addAll(runners)

    internal fun finalize(): Result {
        plugins.forEach { plugin ->
            with(plugin) { install() }
        }
        return Result(
            stateSpecs = stateSpecs.toList(),
            runners = runners.toList(),
            serializersModule = serializersModule,
        )
    }
}

public suspend fun TelegramBot.configure(
    messageRepository: (SerializersModule) -> MessageRepository,
    updateTransformers: Dispatcher.UpdateTransformers,
    getRole: GetRole,
    configure: BotConfigurer.() -> Unit,
) {
    val config = BotConfigurer(telegramBot = this).apply(configure).finalize()
    Dispatcher(
        stateSpecs = config.stateSpecs,
        messageRepository = messageRepository(config.serializersModule),
        updateTransformers = updateTransformers,
        getRole = getRole,
    ).run {
        buildBehaviourWithLongPolling {
            config.runners.forEach { it.block(statefulRunnerContext) }
            regularEngine().block(statefulRunnerContext)
        }.join()
    }
}