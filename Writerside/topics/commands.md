# Команды

Команды имеют более высокий приоритет по умолчанию
и автоматически добавлются в список команд в соответствии с ролью.

## Объявление

```kotlin
val startCommand = command<Role>("start", description = "начать") {
    state.new { MultipleChoiceState() }
    updateCommands()
}
```

updateCommands()
: обновить список команд у пользователя
