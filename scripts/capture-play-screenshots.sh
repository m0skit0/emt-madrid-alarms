#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB_BIN="${ADB_BIN:-$HOME/Android/Sdk/platform-tools/adb}"
PACKAGE_NAME="org.m0skit0.android.emtmadridalarms"
ACTIVITY_NAME="org.m0skit0.android.emtmadridalarms.ui.PlayStoreShowcaseActivity"
OUTPUT_DIR="$ROOT_DIR/app/src/main/play/listings/es-ES/graphics/phone-screenshots"
WAIT_SECONDS="${WAIT_SECONDS:-2}"

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

mkdir -p "$OUTPUT_DIR"

mapfile -t devices < <("$ADB_BIN" devices | awk 'NR > 1 && $2 == "device" { print $1 }')
if (( ${#devices[@]} == 0 )); then
  printf 'No adb device detected. Connect a device or start an emulator first.\n' >&2
  exit 1
fi

SERIAL_ARG=()
if (( ${#devices[@]} > 1 )); then
  if [[ -z "${ANDROID_SERIAL:-}" ]]; then
    printf 'Multiple adb devices detected. Set ANDROID_SERIAL to choose one.\n' >&2
    printf 'Devices:\n' >&2
    printf '  %s\n' "${devices[@]}" >&2
    exit 1
  fi
  SERIAL_ARG=(-s "$ANDROID_SERIAL")
fi

adb_cmd() {
  "$ADB_BIN" "${SERIAL_ARG[@]}" "$@"
}

read_setting() {
  adb_cmd shell settings get "$1" "$2" | tr -d '\r'
}

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
ROTATION_LOCK="$(read_setting system accelerometer_rotation)"
USER_ROTATION="$(read_setting system user_rotation)"

cleanup() {
  restore_setting global animator_duration_scale "$ANIMATOR_SCALE"
  restore_setting global transition_animation_scale "$TRANSITION_SCALE"
  restore_setting global window_animation_scale "$WINDOW_SCALE"
  restore_setting system accelerometer_rotation "$ROTATION_LOCK"
  restore_setting system user_rotation "$USER_ROTATION"
}

trap cleanup EXIT

write_setting global animator_duration_scale 0
write_setting global transition_animation_scale 0
write_setting global window_animation_scale 0
write_setting system accelerometer_rotation 0
write_setting system user_rotation 0

printf 'Building and installing debug app...\n'
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" installDebug >/dev/null

for scene_def in "${SCENES[@]}"; do
  index="${scene_def%%:*}"
  scene="${scene_def#*:}"
  local_file="$OUTPUT_DIR/$index.png"

  printf 'Capturing %s -> %s\n' "$scene" "$local_file"
  adb_cmd shell am force-stop "$PACKAGE_NAME" >/dev/null || true
  adb_cmd shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME" --es scene "$scene" >/dev/null
  sleep "$WAIT_SECONDS"
  adb_cmd exec-out screencap -p > "$local_file"
done

adb_cmd shell am force-stop "$PACKAGE_NAME" >/dev/null || true

printf 'Saved screenshots to %s\n' "$OUTPUT_DIR"
