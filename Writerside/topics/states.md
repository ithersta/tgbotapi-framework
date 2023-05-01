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
    on<…> { … }
    on<…> { … }
    on<…> { … }
}
```

Чаще всего `onNew` и `onEdit` можно заменить одним
блоком `render`.

```kotlin
val spec = inState<…, …> {
    render {
        text = "Текст сообщения"
        keyboard = inlineKeyboard { … }
    }
    …
}
```

## Манипуляции с состояниями

Для манипуляций с состояниями внутри обработчиков используется `state`:

```kotlin
on<…> {
    state.snapshot // текущее состояние
    state.new { … } // создать новое сообщение с указанным состоянием
    state.edit { … } // изменить состояние у текущего сообщения
    state.new(chatId = …) { … } // создать новое сообщение с состоянием в другом чате 
}
```
