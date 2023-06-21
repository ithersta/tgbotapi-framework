package com.ithersta.tgbotapi.builders

import com.ithersta.tgbotapi.basetypes.Action
import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.core.HandlerContext
import com.ithersta.tgbotapi.core.StateAccessor
import com.ithersta.tgbotapi.persistence.PersistedAction
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.exceptions.MessageIsNotModifiedException
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.utils.formatting.toMarkdownTexts
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardRowBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.TextedContent
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regular
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import korlibs.time.DateTime
import java.util.concurrent.ConcurrentHashMap

private val fileIdCache: ConcurrentHashMap<String, FileId> = ConcurrentHashMap<String, FileId>()

internal class MessageTemplate(
    val entities: TextSourcesList,
    val keyboard: InlineKeyboardMarkup,
    val photo: InputFile?,
    val actions: List<PersistedAction>,
    val cache: (FileId) -> Unit,
)

internal suspend fun TelegramBot.send(chat: Chat, template: MessageTemplate): ContentMessage<TextedContent> =
    template.photo?.let { photo ->
        sendPhoto(chat, photo, template.entities, replyMarkup = template.keyboard).also {
            template.cache(it.content.media.fileId)
        }
    } ?: run {
        send(chat, template.entities, replyMarkup = template.keyboard)
    }

internal suspend fun TelegramBot.edit(
    chat: Chat, messageId: MessageId, template: MessageTemplate
): Message = runCatching {
    template.photo?.let { photo ->
        edit(
            chatId = chat.id,
            messageId = messageId,
            media = TelegramMediaPhoto(photo, template.entities),
            replyMarkup = template.keyboard
        ).also {
            template.cache(it.content.media.fileId)
        }
    } ?: run {
        edit(
            chatId = chat.id,
            messageId = messageId,
            entities = template.entities,
            replyMarkup = template.keyboard,
        )
    }
}.onFailure { exception ->
    if (exception !is MessageIsNotModifiedException) {
        throw exception
    }
}.getOrElse {
    object : Message {
        override val chat = chat
        override val date = DateTime.now()
        override val messageId = messageId
    }
}

@FrameworkDslMarker
public class PersistedMessageTemplateBuilder<S : MessageState, R : Role, M : MessageId?>(
    context: HandlerContext<S, StateAccessor.Changing<S>, R, M>,
) : HandlerContext<S, StateAccessor.Changing<S>, R, M> by context {
    public var entities: List<TextSource> = emptyList()
    public var text: String
        get() = entities.toMarkdownTexts().joinToString("")
        set(value) {
            entities = buildEntities { regular(value) }
        }
    public var keyboard: InlineKeyboardMarkup = inlineKeyboard { }
    public var photo: InputFile? = null
    internal var cacheKey: String? = null
    private val persistedActions = mutableListOf<PersistedAction>()

    public fun InlineKeyboardRowBuilder.actionButton(text: String, action: Action) {
        val persistedAction = PersistedAction.from(action)
        dataButton(text, persistedAction.key)
        persistedActions.add(persistedAction)
    }

    internal fun build() = MessageTemplate(
        entities = entities,
        keyboard = keyboard,
        photo = photo,
        actions = persistedActions,
        cache = { fileId ->
            cacheKey?.let { fileIdCache[it] = fileId }
        },
    )
}

@Suppress("NOTHING_TO_INLINE")
public inline fun PersistedMessageTemplateBuilder<*, *, *>.fromResources(path: String): InputFile {
    return cachedFile(path) { {}.javaClass.getResourceAsStream(path)?.asInput() ?: error("Resource not found") }
}

public fun PersistedMessageTemplateBuilder<*, *, *>.cachedFile(key: String, input: () -> Input): InputFile {
    return fileIdCache[key] ?: run {
        cacheKey = key
        InputFile.fromInput(key, input)
    }
}
