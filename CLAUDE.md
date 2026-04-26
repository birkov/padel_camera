# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

All build commands require Android Studio or a local Android SDK. Run from the project root:

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Lint
./gradlew lint

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.padelcamera.app.ExampleTest"
```

> `gradle-wrapper.jar` is not committed. Open in Android Studio and it will be downloaded automatically, or run `gradle wrapper` locally first.

## Architecture

The app is a single-Activity Android app (landscape-only, minSdk 26) that composes three independent concerns:

**1. Streaming pipeline** (`stream/StreamManager.kt`)  
Wraps RootEncoder's `RtmpCamera2`. The camera feed runs through an OpenGL pipeline (`OpenGlView`) that applies an `ObjectFilterRender` as a full-frame overlay. `startStream()` must be called after `startPreview()` — the filter is attached in `attachOverlayFilter()` which is called inside `startPreview()`.

**2. Overlay rendering** (`overlay/ScoreOverlayRenderer.kt`)  
Owns a single reusable `Bitmap` (1280×720 ARGB) and a `Canvas` backed by it. Each call to `render()` clears the bitmap and redraws the score bar at the top of the frame. The bitmap is passed to `ObjectFilterRender.setImage()` in `StreamManager.updateOverlay()`. The overlay is burned into the encoded video — it is not a separate UI layer.

**3. Data layer** (`api/`, `config/`)  
- `AppConfig` is loaded once from `assets/config.json` at startup.
- `ApiClient` is a singleton Retrofit instance. Both API calls use `@Url` so the full endpoint URLs come from `AppConfig`, not from the base URL.
- `MainActivity` owns two coroutine jobs on `lifecycleScope`: a one-shot players fetch and a periodic score poll. When `test_mode: true`, neither job hits the network.

## Configuration file

`app/src/main/assets/config.json` is the only place to change API endpoints and test mode:

```json
{
  "test_mode": true,
  "players_api_url": "https://...",
  "score_api_url": "https://...",
  "score_refresh_interval_seconds": 30,
  "youtube_rtmp_base_url": "rtmp://a.rtmp.youtube.com/live2"
}
```

Set `test_mode: false` and fill in real URLs before a live match. The YouTube stream key is entered by the user in the app UI at runtime and is never stored.

## Expected API response shapes

```json
// players endpoint
{ "player1_name": "Alice", "player2_name": "Bob" }

// score endpoint
{ "player1_score": 3, "player2_score": 1 }
```

## Key constraints

- **Physical device only** — Camera2 and RTMP do not work in the Android emulator.
- **Landscape forced** — `android:screenOrientation="landscape"` in the manifest and activity declaration. Do not add portrait layouts.
- **Overlay is in-stream** — the score bar is rendered into the video frames via OpenGL, not drawn on top of the UI. Changing its appearance means editing `ScoreOverlayRenderer`, not the XML layout.
- **Single bitmap reuse** — `ScoreOverlayRenderer` reuses the same `Bitmap` instance for performance. Never hold a reference to the returned bitmap across calls.
