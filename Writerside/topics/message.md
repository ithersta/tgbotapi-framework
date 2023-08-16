# Блок message

Позволяет декларативно описывать сообщение,
которое отправляет бот при создании или изменении состояния.
Автоматически генерирует блоки onNew и onEdit.

## Примеры

Текст
: ```kotlin
  message {
    text("Текст")
  }
  ```

Текст с инлайн клавиатурой
: ```kotlin
  message {
    text("Текст") {
      row {
        actionButton("Кнопка", SomeAction())
      }
    }
  }
  ```

Фото
: ```kotlin
  message {
    photo(photoFromResources("/photo.jpg"))
  }
  ```

Фото с инлайн клавиатурой
: ```kotlin
  message {
    photo(photoFromResources("photo.jpg")) {
      row {
        actionButton("Кнопка", SomeAction())
      }
    }
  }
  ```
