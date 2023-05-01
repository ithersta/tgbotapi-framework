# Пагинация

В качестве примера сделаем клавиатуру с числами от 1 до 100.

## Без пагинации

Создадим состояние и спецификацию.

```kotlin
@Serializable
class SamplePaginationState : MessageState

val samplePagination = inState<Role, SamplePaginationState> {
    val numbers = (1..100)
    render {
        text = "Числа от 1 до 100"
        keyboard = inlineKeyboard {
            numbers.forEach { number ->
                row {
                    actionButton(number.toString(), …)
                }
            }
        }
    }
}
```

## С пагинацией

Реализуем пагинацию.

1. Сделаем так, чтобы состояние реализовывало `WithPagination`.

    ```kotlin
    @Serializable
    data class SamplePaginationState(
        override val page: Int
    ) : MessageState, WithPagination<SamplePaginationState> {
        override fun withPage(page: Int) = copy(page = page)
    }
    ```

2. Изменим спецификацию.

   Теперь в блоке `render` доступны инструменты для пагинации:

   limit
   : количество элементов на одной странице

   offset
   : отступ в элементах от начала списка

   navigationRow(itemCount = …)
   : кнопки назад, вперёд, а также счётчик страниц

   Используем их.
    ```kotlin
    val samplePagination = inState<Role, SamplePaginationState> {
        val numbers = (1..100)
        render {
            text = "Числа от 1 до 100"
            keyboard = inlineKeyboard {
                numbers.drop(offset).take(limit).forEach { number ->
                    row {
                        actionButton(number.toString(), …)
                    }
                }
                navigationRow(itemCount = numbers.count())
            }
        }
    }
    ```

### Результат

![Результат](pagination.gif){ border-effect="rounded" }

[Полный код](https://github.com/ithersta/tgbotapi-framework/blob/main/sample/src/main/kotlin/com/ithersta/sample/PaginationFlow.kt)

