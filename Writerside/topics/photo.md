# Фото

## Из ресурсов

Для эффективной отправки фото из ресурсов используется
функция `fromResources`.
После первой отправки файл кэшируется и не отправляется повторно.

```kotlin
render {
    photo = fromResources("/photo.jpg")
    …
}
```