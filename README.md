<div align="center">
  <img src="art/banner.png" alt="Kinera" width="100%" />
</div>

[![Android CI](https://github.com/mel0k1/Kinera/actions/workflows/ci.yml/badge.svg)](https://github.com/mel0k1/Kinera/actions/workflows/ci.yml)

# Kinera

A manga and manhwa reader for Android, written in Kotlin with Jetpack Compose and Material 3 Expressive. It reads from the official [MangaDex API](https://api.mangadex.org), the Russian catalogs **MangaLib**, **Remanga** and **Desu.me**, your self-hosted **Komga** or **Suwayomi** server, local CBZ archives — and keeps your library on the device. No accounts, no tracking, no bundled content.

- Android 12+ (API 31)
- Kotlin 2.4 · Compose · Material 3 Expressive
- License: [GPL-3.0](LICENSE)

[Русская версия](README.ru.md)

## Screenshots

Real renders of the actual Compose UI, recorded on the JVM with [Roborazzi](https://github.com/takahirom/Roborazzi) (`:app:recordRoborazziDebug`).

| Library | Explore | Details | Reader (dark) |
| --- | --- | --- | --- |
| ![Library](art/screenshots/library_light.png) | ![Explore](art/screenshots/explore_light.png) | ![Details](art/screenshots/details_light.png) | ![Reader](art/screenshots/reader_dark.png) |

## Features

- **Library** — favourites with custom categories: long-press a cover to move the title between categories, and long-press a category chip to drag it into another order
- **Explore** — popular and latest titles, endless scroll, per-source tabs
- **Sources** — **MangaDex** (with an RU/EN language filter), **MangaLib**, **Remanga** and **Desu.me** — Russian catalogs — out of the box; self-hosted **Komga** and **Suwayomi** servers (URL + optional basic auth in Settings; covers and pages are authenticated automatically; a source picker inside Suwayomi in Settings)
- **Library auto-update** — Kinera checks favourites for new chapters in the background (every 6/12/24 hours, configurable), badges them on the shelf and clears the badge when you open the title; there is also a "check now" button in the Library toolbar
- **MangaLib / Remanga / Desu.me filters** — status chips and a genre picker right in Explore (and search), persisted between runs
- **Backup & restore** — export the whole library (favourites, categories, history, page bookmarks) to a human-readable JSON file and merge it back after a reinstall or on another device; the file is exchanged through the system file picker, categories are matched by name
- **Local sources** — import CBZ/ZIP archives through the system picker, or add a folder of loose images (subfolders become chapters); nothing is copied, pages are read straight from where they live
- **Offline downloads** — save chapters from the chapter list (one by one or all at once), a dedicated Downloads tab shows progress, retries and cleanup
- **Search** — debounced title search with load-more, across every configured source; with Suwayomi you pick which source installed on the server to search in, right above the results
- **Reader** — webtoon (continuous scroll) and paged modes, switchable on the fly; pinch-to-zoom and double-tap zoom in both modes; screen stays awake while you read
- **History** — automatic reading progress, resume where you stopped
- **Bookmarks** — mark any page from the reader and jump back to it from the manga details
- **Details** — chapter list with read markers, tags, description
- **Warm amber identity** — a hand-tuned cream-and-amber theme out of the box; optional wallpaper-based dynamic color in Settings
- **Expressive motion** — spring-based animation scheme, a floating pill navigation bar whose label unfolds on selection, press-scale feedback on cards, large flexible app bars, wavy progress indicators, morphing favourite toggle, shape-morphing loader and expressive empty states with the Kinera cat

## Build

Requirements: Android Studio (Ladybug or newer) and JDK 17.

```bash
git clone https://github.com/mel0k1/Kinera.git
cd Kinera
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

No Android Studio around? Every push to `main` runs [Android CI](https://github.com/mel0k1/Kinera/actions/workflows/ci.yml), which publishes a debug APK as a workflow artifact — grab the latest green run and install it.

Releases carry both APKs: an unsigned debug build and a **signed release build** (`Kinera-v…-release.apk`, the same key every version). To sign release builds yourself, put a keystore next to the repo as `kinera-release.jks` plus a `keystore.properties` file:

```properties
KINERA_KEYSTORE_FILE=kinera-release.jks
KINERA_KEYSTORE_PASSWORD=…
KINERA_KEY_ALIAS=kinera
KINERA_KEY_PASSWORD=…
```

Both files are git-ignored; on GitHub Actions the same values are read from the `KINERA_KEYSTORE_*` repository secrets.

Or open the folder in Android Studio and press Run.

## Architecture

A single `:app` module, split by package:

```
org.kinera.app
├── core/          domain models, UI state
├── data/
│   ├── mangadex/  Retrofit API, DTOs, MangaDex source
│   ├── mangalib/  MangaLib JSON API source (Russian catalog)
│   ├── remanga/   Remanga JSON API source (Russian catalog)
│   ├── desume/    Desu.me JSON API source (Russian catalog, mirror fallback)
│   ├── komga/     Komga REST source (self-hosted)
│   ├── suwayomi/  Suwayomi GraphQL source (self-hosted)
│   ├── selfhost/  basic-auth interceptor for self-hosted servers
│   ├── localsource/ CBZ import, scanning and page extraction
│   ├── downloads/ on-disk storage for downloaded chapters
│   ├── local/     Room database (favourites, history, categories, downloads)
│   ├── settings/  DataStore preferences
│   └── repository/ catalog, library and download repositories
├── di/            Koin modules
├── worker/        WorkManager jobs: chapter downloads and the library update
└── ui/
    ├── theme/     Material 3 Expressive theme and motion
    ├── components/ shared composables
    └── screens/   library, explore, search, details, reader, downloads, history, settings
```

UI is 100% Jetpack Compose. DI is Koin. Network is Retrofit + OkHttp with kotlinx.serialization. Images go through Coil with a 256 MB disk cache. Background work (chapter downloads, periodic library refresh) is WorkManager with a Koin-backed worker factory.

## Roadmap

- Per-source settings (language filters, source picker inside Suwayomi)
- Optionally strip downloads together with their chapters

## Credits and legal

- [Kotatsu](https://github.com/KotatsuApp/Kotatsu) — inspiration and the general idea of a free, open manga reader for Android
- [MangaDex](https://mangadex.org) — catalog data comes from their public API; Kinera is not affiliated with MangaDex
- [MangaLib](https://mangalib.me) — the largest Russian manga catalog, read through their public API
- [Remanga](https://remanga.org) — Russian manga catalog, read through their public API
- [Desu.me](https://desu.me) — Russian anime & manga portal, read through their public API
- [Komga](https://komga.org) and [Suwayomi](https://suwayomi.com) — supported self-hosted server APIs
- App icon and logo by [mel0k1](https://github.com/mel0k1)

Kinera does not host or provide any manga. It is a client for a public API, and everything you read belongs to its respective authors and publishers.

## License

This project is licensed under the [GNU GPL-3.0](LICENSE). If you fork it and distribute the result, the fork must stay open under the same license.
