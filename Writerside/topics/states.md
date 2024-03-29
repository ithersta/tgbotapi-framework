# Состояния

Состояния привязаны к сообщениям от бота.

## Объявление

Состояния должны быть `@Serializable` и реализовывать `MessageState`.

```kotlin
@Serializable
data class SampleState(
    val sampleData: Int
) : MessageState
```

## Спецификация состояния

Спецификация говорит, что нужно делать боту при создании нового сообщения
с этим состоянием (`onNew`), при редактировании (`onEdit`)
и при каком-то событии (например `on<TextMessage>`).

```kotlin
val spec = inState<SampleRole, SampleState> {
    onNew { … }
    onEdit { … }
    on<TextMessage> { … }
    on<FromUserMessage> { … }
    on<…> { … }
}
```

Обработчики `onNew` и `onEdit` должны возвращать
`Message`, чтобы бот мог ассоциировать состояние с сообщением.

Когда требуется отправить текстовое сообщение с инлайн клавиатурой,
можно воспользоваться блоком `message`, 
`onNew` и `onEdit` будут сгенерированы автоматически.

```kotlin
val spec = inState<…, …> {
    message {
        text("Текст сообщения") {
            row {
                actionButton(…, …)
            }
        }
    }
    …
}
```

## Манипуляции с состояниями

Для манипуляций с состояниями внутри обработчиков используется `state`:

state.snapshot
: текущее состояние

state.new { … }
: создать новое сообщение с указанным состоянием

state.edit { … }
: изменить состояние у текущего сообщения

state.delete()
: удалить текущее сообщение с его состоянием

state.new(chatId = …) { … }
: создать новое сообщение с состоянием в другом чате
