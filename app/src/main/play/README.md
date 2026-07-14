Play Store metadata tracked for Gradle Play Publisher.

- `default-language.txt` sets the canonical listing locale.
- `listings/es-ES/` stores the Spanish storefront copy.
- `listings/es-ES/graphics/` stores screenshot PNGs organised by device class:
  - `phone-screenshots/`
  - `tablet-screenshots/`
  - `large-tablet-screenshots/`
- Keep Play media directories limited to image files only; put docs in parent folders such as `graphics/README.md`.
- Use the debug previews in `PlayStoreShowcasePreviews.kt` as the source scenes for screenshot capture.
- Or run `scripts/capture-play-screenshots.sh` with a connected adb device or emulator:
  ```bash
  scripts/capture-play-screenshots.sh
  scripts/capture-play-screenshots.sh emulator-5554
  scripts/capture-play-screenshots.sh tablet emulator-5554
  ```
- With no target argument, the script auto-detects `phone`, `tablet`, or `large-tablet` from the device size.
- Publish metadata with `./gradlew publishListing` once the final PNGs are in place.
