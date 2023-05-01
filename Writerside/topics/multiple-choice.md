# Множественный выбор

Сделаем бота со множественным выбором, который спрашивает, что надеть.

## Готовимся
Типичный гардероб:

```kotlin
@Serializable
enum class Clothes {
  Socks, Shirt, Pants, Hat
}
```

## Делаем
Что уже надели, будем хранить в состоянии:

```kotlin
@Serializable
data class MultipleChoiceState(
  val selectedClothes: Set<Clothes> = emptySet(),
) : MessageState
```

Теперь реализуем `render` для нашего состояния.
Если одежда есть в списке выбранных,
будем рисовать кнопку с галочкой, иначе – без.

```kotlin
val multipleChoice = inState<DefaultRole, MultipleChoiceState> {
  render {
    text = "Что наденем?"
    keyboard = inlineKeyboard {
      Clothes.values().forEach { clothes ->
        row {
          if (clothes in state.snapshot.selectedClothes) {
            actionButton("✅ ${clothes.name}", UnselectAction(clothes))
          } else {
            actionButton(clothes.name, SelectAction(clothes))
          }
        }
      }
    }
  }
}
```

Для кнопок понадобится два типа действий: выбрать
и снять выбор.

```kotlin
@Serializable
class SelectAction(val clothes: Clothes) : Action

@Serializable
class UnselectAction(val clothes: Clothes) : Action
```

По нажатию кнопки с SelectAction будем
добавлять одежду во множество выбранных.

```kotlin
on<SelectAction> {
  state.edit { copy(selectedClothes = selectedClothes + it.clothes) }
}
```

А по нажатию кнопки с UnselectAction – убирать.

```kotlin
on<UnselectAction> {
  state.edit { copy(selectedClothes = selectedClothes - it.clothes) }
}
```

При вызове `state.edit` сообщение редактируется
автоматически в соответствии с обновлённым состоянием.

## Результат

![Результат](multiple-choice.gif){ border-effect="rounded" }

[Полный код](https://github.com/ithersta/tgbotapi-framework/blob/main/sample/src/main/kotlin/com/ithersta/sample/MultipleChoice.kt)
