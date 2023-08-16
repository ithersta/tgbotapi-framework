# Действия (Actions)

Действия (Actions) предоставляют типобезопасный способ
задавать обработчики для инлайн кнопок.

> Действия хранятся в БД, а в кнопку записывается случайный ключ.
> 
> Это имеет два последствия:
> 
> 1. Кнопку нельзя подделать
> 2. Размер сериализованного действия не ограничен

## Использование

### Объявление
Действия должны быть `@Serializable` и реализовывать `Action`.

```kotlin
@Serializable
data class SampleAction(
    val someId: Long
) : Action
```

### Отправка кнопки
Чтобы отправить кнопку с действием, можно воспользоваться функцией `actionButton`
внутри блока `message`.

```kotlin
message {
    text(…) {
        row {
            actionButton("Какая-то кнопка", SampleAction(2))
        }
    }
}
```

### Обработчики
Чтобы обработать нажатие кнопки с действием, можно воспользоваться
одним из следующих способов:

Без CallbackQuery
: ```kotlin
  on<SampleAction> { action -> … }
  ```

С CallbackQuery
: ```kotlin
  on<Pair<SampleAction, MessageDataCallbackQuery>> { (action, query) ->
      …
  }
  ```
