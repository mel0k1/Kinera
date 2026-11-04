<div align="center">
  <img src="art/banner.png" alt="Kinera" width="100%" />
</div>

# Kinera

A manga and manhwa reader for Android, written in Kotlin with Jetpack Compose and Material 3 Expressive. It reads from the official [MangaDex API](https://api.mangadex.org) and keeps your library on the device. No accounts, no tracking, no bundled content.

- Android 12+ (API 31)
- Kotlin 2.4 · Compose · Material 3 Expressive
- License: [GPL-3.0](LICENSE)

[Русская версия](README.ru.md)

## Features

- **Library** — favourites with custom categories, long-press to remove
- **Explore** — popular and latest titles from MangaDex, endless scroll
- **Search** — debounced title search
- **Reader** — webtoon (continuous scroll) and paged modes, switchable on the fly; screen stays awake while you read
- **History** — automatic reading progress, resume where you stopped
- **Details** — chapter list with read markers, tags, description
- **Material You** — dynamic color from your wallpaper, or the built-in warm amber theme
- **Expressive motion** — spring-based animation scheme, wavy progress indicators, morphing favourite toggle

## Screenshots

Coming with the first release build. CI assembles a debug APK on every push, so nothing here is hand-waved — the code builds.

## Build

Requirements: Android Studio (Ladybug or newer) and JDK 17.

```bash
git clone https://github.com/mel0k1/Kinera.git
cd Kinera
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

Or open the folder in Android Studio and press Run.

## Architecture

A single `:app` module, split by package:

```
org.kinera.app
├── core/          domain models, UI state
├── data/
│   ├── mangadex/  Retrofit API, DTOs, MangaDex source
│   ├── local/     Room database (favourites, history, categories)
│   ├── settings/  DataStore preferences
│   └── repository/ catalog and library repositories
├── di/            Koin modules
└── ui/
    ├── theme/     Material 3 Expressive theme and motion
    ├── components/ shared composables
    └── screens/   library, explore, search, details, reader, history, settings
```

UI is 100% Jetpack Compose. DI is Koin. Network is Retrofit + OkHttp with kotlinx.serialization. Images go through Coil with a 256 MB disk cache.

## Roadmap

- Local sources (CBZ/ZIP archives and folders)
- Pinch-to-zoom in webtoon mode
- Download manager for offline reading
- More catalog sources behind a common interface
- Bookmarks inside chapters

## Credits and legal

- [Kotatsu](https://github.com/KotatsuApp/Kotatsu) — inspiration and the general idea of a free, open manga reader for Android
- [MangaDex](https://mangadex.org) — catalog data comes from their public API; Kinera is not affiliated with MangaDex
- App icon and logo by [mel0k1](https://github.com/mel0k1)

Kinera does not host or provide any manga. It is a client for a public API, and everything you read belongs to its respective authors and publishers.

## License

This project is licensed under the [GNU GPL-3.0](LICENSE). If you fork it and distribute the result, the fork must stay open under the same license.
