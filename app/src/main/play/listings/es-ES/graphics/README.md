Play listing graphics for the `es-ES` storefront.

## Screenshot order (all device classes)

1. `setup` — "Configura alertas en segundos"
2. `lines` — "Busca por línea o destino"
3. `stops` — "Confirma la parada exacta"
4. `monitoring` — "Sigue la llegada en tiempo real"
5. `scheduled` — "Gestiona varias alarmas"
6. `ringing` — "Recibe el aviso final"

## Directories

| Directory | Description |
|---|---|
| `phone-screenshots/` | Phone (portrait, 9:16) |
| `tablet-screenshots/` | 7-inch tablet (portrait) |
| `large-tablet-screenshots/` | 10-inch tablet (portrait) |

## Capture

```bash
# Auto-detect target from the only connected device
scripts/capture-play-screenshots.sh

# Auto-detect target for a specific device
scripts/capture-play-screenshots.sh emulator-5554

# Manual override
scripts/capture-play-screenshots.sh tablet emulator-5554
scripts/capture-play-screenshots.sh large-tablet emulator-5556
```

If you omit the serial, the script uses the only connected device. When multiple devices are connected, pass the adb serial. If you omit the target, the script auto-detects it from the device size using Android-style `sw600dp` and `sw720dp` thresholds.

Keep media directories limited to publishable image files only.
