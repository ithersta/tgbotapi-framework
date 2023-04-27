package com.ithersta.sample

import com.ithersta.tgbotapi.autoconfigure.autoconfigure
import org.koin.core.context.startKoin
import org.koin.ksp.generated.defaultModule

suspend fun main() = startKoin {
    modules(defaultModule)
}.autoconfigure()
