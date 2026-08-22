# RabbitHole 🐇

**Start anywhere. See where it takes you.**

An Android discovery & knowledge-exploration app. Enter any topic — a person, movie,
game, place, concept — and RabbitHole builds an interactive graph of related concepts
you can endlessly explore.

*Think: Obsidian's graph view meets Arc Search's minimalism.*

> ⚠️ **Status: Phase 1–2 of 7** — working UI with mocked data (Cyberpunk 2077, AI, Joji,
> Delhi, Black Holes, WWII, Formula 1). Real Wikipedia/Wikidata + AI layers come in Phase 5+.

## Screenshots

| Home | Discovery | Graph |
|---|---|---|
| search-first home | animated discovery flow | interactive force-graph |

*(add after first build)*

## Current features (Phase 1–2)

- 🎨 Dark-first Material 3 theme (#08090D canvas, violet/cyan accents)
- 🏠 Minimal search-first home: rotating examples, quick-start chips
- ✨ Animated "Building your rabbit hole…" discovery flow
- 🕸️ Interactive knowledge graph:
  - pinch zoom · two-finger pan · single-finger pan · node drag
  - tap to select → highlights connections, dims unrelated nodes
  - zoom in/out/reset floating controls
  - search-within-graph
  - compact type-color legend
- 📋 Node bottom sheet: description, connected-to list, action buttons
- 🧭 Bottom navigation: Home / Explore / Saved / History / Settings

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Coroutines/Flow ·
Retrofit + OkHttp · Kotlin Serialization · Room · Coil · Hilt

## Build

```bash
./gradlew assembleDebug
```

APK at `app/build/outputs/apk/debug/app-debug.apk`. CI builds on every push.

## Roadmap

- [x] **P1:** theme, navigation, home, bottom bar
- [x] **P2:** search, mock data, graph render, zoom/pan, selection
- [ ] P3: Take Me Deeper, Rabbit Hole mode, exploration path
- [ ] P4: Saved / History / Share persistence
- [ ] P5: Wikipedia/Wikidata API integration
- [ ] P6: AI relationship ranking with source verification
- [ ] P7: performance & accessibility polish

## License

MIT
