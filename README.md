# Cogno

Cogno is an Android course project and portfolio app prototype. The `refactor` branch is being migrated from a WebView prototype into a native Android implementation.

## Current Native Stack

- Kotlin
- Jetpack Compose
- Single `MainActivity`
- Compose Navigation
- Room
- Repository pattern
- Coroutines and Flow

## Source Set Note

Android Studio may still display the folder as `app/src/main/java`. This is the default Android source set name and is normal. The current main implementation files in that source set are Kotlin `.kt` files.

## WebView Assets

The original HTML/CSS/JavaScript files under `app/src/main/assets/` are intentionally kept as visual references during the Compose migration. They should not be batch-deleted.
