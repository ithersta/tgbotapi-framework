package com.ithersta.tgbotapi.cache

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.types.ChatId
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import java.util.concurrent.ConcurrentHashMap

private val fileIdCache: ConcurrentHashMap<String, FileId> = ConcurrentHashMap()
private val cacheChatId = ChatId(105293829)

private suspend fun TelegramBot.cached(
    key: String,
    input: () -> Input,
    send: suspend (ChatId, MultipartFile) -> FileId
): FileId {
    return fileIdCache[key] ?: run {
        val fileId = send(cacheChatId, InputFile.fromInput(key, input))
        fileIdCache[key] = fileId
        fileId
    }
}

public suspend fun TelegramBot.cachedFile(key: String, input: () -> Input): FileId {
    return cached(key, input) { chatId, file ->
        sendDocument(chatId, file).content.media.fileId
    }
}

public suspend inline fun TelegramBot.fileFromResources(path: String): FileId {
    return cachedFile(path) { {}.javaClass.getResourceAsStream(path)?.asInput() ?: error("Resource not found") }
}

public suspend fun TelegramBot.cachedPhoto(key: String, input: () -> Input): FileId {
    return cached(key, input) { chatId, file ->
        sendPhoto(chatId, file).content.media.fileId
    }
}

public suspend inline fun TelegramBot.photoFromResources(path: String): FileId {
    return cachedPhoto(path) { {}.javaClass.getResourceAsStream(path)?.asInput() ?: error("Resource not found") }
}
