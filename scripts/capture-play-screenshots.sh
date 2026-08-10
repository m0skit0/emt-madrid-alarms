#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB_BIN="${ADB_BIN:-$HOME/Android/Sdk/platform-tools/adb}"
PACKAGE_NAME="org.m0skit0.android.emtmadridalarms"
ACTIVITY_NAME="org.m0skit0.android.emtmadridalarms.ui.PlayStoreShowcaseActivity"
WAIT_SECONDS="${WAIT_SECONDS:-2}"

TARGET=""
REQUESTED_SERIAL=""

case "${1:-}" in
  phone|tablet|large-tablet)
    TARGET="$1"
    REQUESTED_SERIAL="${2:-}"
    ;;
  "")
    ;;
  *)
    REQUESTED_SERIAL="$1"
    ;;
esac

SCENES=(
  "1:setup"
  "2:lines"
  "3:stops"
  "4:monitoring"
  "5:scheduled"
  "6:ringing"
)

if [[ ! -x "$ADB_BIN" ]]; then
  printf 'adb not found at %s\n' "$ADB_BIN" >&2
  exit 1
fi

if [[ ! -f "$ROOT_DIR/gradlew" ]]; then
  printf 'Could not find gradlew from %s\n' "$ROOT_DIR" >&2
  exit 1
fi

mapfile -t devices < <("$ADB_BIN" devices | awk 'NR > 1 && $2 == "device" { print $1 }')
if (( ${#devices[@]} == 0 )); then
  printf 'No adb device detected. Connect a device or start an emulator first.\n' >&2
  exit 1
fi

SERIAL_ARG=()
if [[ -n "$REQUESTED_SERIAL" ]]; then
  matched=false
  for device in "${devices[@]}"; do
    if [[ "$device" == "$REQUESTED_SERIAL" ]]; then
      matched=true
      break
    fi
  done
  if [[ "$matched" != true ]]; then
    printf 'Requested device not connected: %s\n' "$REQUESTED_SERIAL" >&2
    printf 'Devices:\n' >&2
    printf '  %s\n' "${devices[@]}" >&2
    exit 1
  fi
  SERIAL_ARG=(-s "$REQUESTED_SERIAL")
elif (( ${#devices[@]} == 1 )); then
  SERIAL_ARG=(-s "${devices[0]}")
else
  printf 'Multiple adb devices detected. Pass the adb serial as the second argument.\n' >&2
  printf 'Usage: %s <phone|tablet|large-tablet> [adb-serial]\n' "$0" >&2
  printf 'Devices:\n' >&2
  printf '  %s\n' "${devices[@]}" >&2
  exit 1
fi

adb_cmd() {
  "$ADB_BIN" "${SERIAL_ARG[@]}" "$@"
}

resolve_target_config() {
  case "$1" in
    phone)
      OUTPUT_DIR="$ROOT_DIR/app/src/main/play/listings/es-ES/graphics/phone-screenshots"
      SHOWCASE_MODE="phone"
      ;;
    tablet)
      OUTPUT_DIR="$ROOT_DIR/app/src/main/play/listings/es-ES/graphics/tablet-screenshots"
      SHOWCASE_MODE="tablet"
      ;;
    large-tablet)
      OUTPUT_DIR="$ROOT_DIR/app/src/main/play/listings/es-ES/graphics/large-tablet-screenshots"
      SHOWCASE_MODE="tablet"
      ;;
    *)
      printf 'Usage: %s [phone|tablet|large-tablet] [adb-serial]\n' "$0" >&2
      exit 1
      ;;
  esac
}

read_wm_value() {
  adb_cmd shell wm "$1" | tr -d '\r'
}

detect_target() {
  local size_output density_output width height density smallest_px smallest_dp

  size_output="$(read_wm_value size)"
  density_output="$(read_wm_value density)"

  if [[ "$size_output" =~ ([0-9]+)x([0-9]+) ]]; then
    width="${BASH_REMATCH[1]}"
    height="${BASH_REMATCH[2]}"
  else
    printf 'Could not parse device size from: %s\n' "$size_output" >&2
    exit 1
  fi

  if [[ "$density_output" =~ ([0-9]+) ]]; then
    density="${BASH_REMATCH[1]}"
  else
    printf 'Could not parse device density from: %s\n' "$density_output" >&2
    exit 1
  fi

  if (( width < height )); then
    smallest_px="$width"
  else
    smallest_px="$height"
  fi

  smallest_dp=$(( smallest_px * 160 / density ))

  if (( smallest_dp >= 720 )); then
    TARGET="large-tablet"
  elif (( smallest_dp >= 600 )); then
    TARGET="tablet"
  else
    TARGET="phone"
  fi

  printf 'Auto-detected target %s (smallest width %sdp; %sx%s @ %sdpi)\n' \
    "$TARGET" "$smallest_dp" "$width" "$height" "$density"
}

read_setting() {
  adb_cmd shell settings get "$1" "$2" | tr -d '\r'
}

if [[ -z "$TARGET" ]]; then
  detect_target
fi

resolve_target_config "$TARGET"

mkdir -p "$OUTPUT_DIR"

write_setting() {
  adb_cmd shell settings put "$1" "$2" "$3" >/dev/null
}

restore_setting() {
  local namespace="$1"
  local key="$2"
  local value="$3"
  if [[ "$value" == "null" ]]; then
    adb_cmd shell settings delete "$namespace" "$key" >/dev/null || true
  else
    write_setting "$namespace" "$key" "$value"
  fi
}

ANIMATOR_SCALE="$(read_setting global animator_duration_scale)"
TRANSITION_SCALE="$(read_setting global transition_animation_scale)"
WINDOW_SCALE="$(read_setting global window_animation_scale)"

cleanup() {
  restore_setting global animator_duration_scale "$ANIMATOR_SCALE"
  restore_setting global transition_animation_scale "$TRANSITION_SCALE"
  restore_setting global window_animation_scale "$WINDOW_SCALE"
}

trap cleanup EXIT

write_setting global animator_duration_scale 0
write_setting global transition_animation_scale 0
write_setting global window_animation_scale 0

printf 'Building and installing debug app (target: %s)...\n' "$TARGET"
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" installDebug >/dev/null

for scene_def in "${SCENES[@]}"; do
  index="${scene_def%%:*}"
  scene="${scene_def#*:}"
  local_file="$OUTPUT_DIR/$index.png"

  printf 'Capturing %s -> %s\n' "$scene" "$local_file"
  adb_cmd shell am force-stop "$PACKAGE_NAME" >/dev/null || true
  adb_cmd shell am start -W -n "$PACKAGE_NAME/$ACTIVITY_NAME" \
    --es scene "$scene" --es mode "$SHOWCASE_MODE" >/dev/null
  sleep "$WAIT_SECONDS"
  adb_cmd shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  sleep 1
  adb_cmd exec-out screencap -p > "$local_file"
done

adb_cmd shell am force-stop "$PACKAGE_NAME" >/dev/null || true

printf 'Saved screenshots to %s\n' "$OUTPUT_DIR"
