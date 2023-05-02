# DialogueFlow

DialogueFlow позволяет боту автоматически находить
все спецификации состояний.

## Как использовать

Чтобы им воспользоваться, нужно пометить класс
аннотацией `@Single` и реализовать интерфейс `DialogueFlow`.
Спецификации состояний оформляются как свойства класса.

```kotlin
@Single
class SampleFlow : DialogueFlow {
    val command = command<Role>("sample", "Пример") {
        state.new { SomeState(…) }
    }

    @Serializable
    data class SomeState(…) : MessageState

    val someSpec = inState<…, SomeState> { … }
    val someOtherSpec = …
}
```

Теперь бот найдёт все `DialogueFlow` и все спецификации состояний в них.
