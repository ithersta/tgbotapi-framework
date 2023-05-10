package com.ithersta.tgbotapi.engines

import com.ithersta.tgbotapi.core.Dispatcher
import com.ithersta.tgbotapi.core.runner.StatefulRunner
import com.ithersta.tgbotapi.core.runner.statefulRunner
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptionsAsync
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.utils.extensions.sourceChat
import dev.inmo.tgbotapi.utils.PreviewFeature
import kotlinx.coroutines.CancellationException

@OptIn(PreviewFeature::class)
public fun Dispatcher.regularEngine(
    exceptionHandler: suspend TelegramBot.(Throwable) -> Unit = { it.printStackTrace() },
): StatefulRunner = statefulRunner {
    applyPendingStates()
    allUpdatesFlow.subscribeSafelyWithoutExceptionsAsync(scope, { it.sourceChat() }) { update ->
        runCatching {
            handle(update)
        }.onFailure { exception ->
            if (exception is CancellationException) {
                throw exception
            } else {
                exceptionHandler(bot, exception)
            }
        }
    }
}
