package com.ithersta.sample

import com.ithersta.tgbotapi.autoconfigure.autoconfigureBot
import com.ithersta.tgbotapi.autoconfigure.generatedSerializersModule
import org.koin.core.context.startKoin
import org.koin.ksp.generated.defaultModule

suspend fun main() = startKoin {
    modules(defaultModule)
}.autoconfigureBot {
    serializersModule(generatedSerializersModule())
}
