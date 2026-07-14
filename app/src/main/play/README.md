Play Store metadata tracked for Gradle Play Publisher.

- `default-language.txt` sets the canonical listing locale.
- `listings/es-ES/` stores the Spanish storefront copy.
- `listings/es-ES/graphics/phone-screenshots/` is the target folder for exported PNG screenshots.
- Keep Play media directories limited to image files only; put docs in parent folders such as `graphics/README.md`.
- Use the debug previews in `PlayStoreShowcasePreviews.kt` as the source scenes for screenshot capture.
- Or run `scripts/capture-play-screenshots.sh` with a connected adb device or emulator.
- Publish metadata with `./gradlew publishListing` once the final PNGs are in place.
