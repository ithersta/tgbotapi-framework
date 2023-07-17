package com.ithersta.tgbotapi.init.plugins

import arrow.core.raise.either
import arrow.resilience.Schedule
import com.ithersta.tgbotapi.core.runner.statefulRunner
import dev.inmo.micro_utils.coroutines.launchSafely
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.toChatId
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

public val DumpRunner: Plugin = dumpRunner()

public fun dumpRunner(
    interval: Duration = 8.hours,
    chatId: ChatId? = System.getenv()["DUMP_CHAT_ID"]?.toLong()?.toChatId(),
    endpoint: String? = System.getenv()["DUMP_ENDPOINT"],
): Plugin = Plugin {
    val logger = LoggerFactory.getLogger("DumpRunner")
    val runner = statefulRunner {
        if (chatId == null) {
            logger.info("DUMP_CHAT_ID is not set")
            return@statefulRunner
        }
        launchSafely(
            onException = { exception ->
                logger.error("Exception during dump", exception)
            },
        ) {
            if (endpoint == null) {
                send(chatId, "DUMP_ENDPOINT is not set")
                return@launchSafely
            }
            val api = DumpApi(endpoint)
            Schedule.spaced<Unit>(interval).repeat {
                api.get().onRight {
                    sendDocument(chatId, it.asMultipartFile("dump-${Instant.now()}.sql"))
                }.onLeft {
                    send(chatId, it)
                }
            }
        }
    }
    add(runner)
}

private class DumpApi(
    private val url: String,
    private val client: HttpClient = HttpClient { },
) {
    suspend fun get() = either {
        val response = client.get(url)
        if (response.status.isSuccess().not()) raise(response.bodyAsText())
        response.bodyAsChannel()
    }
}