#!/usr/bin/env bash
set -euo pipefail

# Ephemeral CI emulator only. The API 24 goldfish HAL uses a 15 ms period:
# 44100 Hz produces 661 frames, violating FastMixer's 16-frame block assumption.
# 48000 Hz produces 720 frames. Keep the real audio clock and mono playback tests.
# https://android.googlesource.com/device/generic/goldfish/+/03460f6d67cc7a268a6af582f2f671fdf2e33379/audio/audio_hw.c
[[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1 ]]
[[ "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" = 24 ]]
adb root
adb wait-for-device
adb remount
policy="${RUNNER_TEMP:?}/pebble-audio-policy.conf"
adb pull /system/etc/audio_policy.conf "$policy"
python3 - "$policy" <<'PY'
import pathlib
import re
import sys
path = pathlib.Path(sys.argv[1])
text, count = re.subn(r"(?m)^(\s*sampling_rates\s+)[0-9|]+", r"\g<1>48000", path.read_text(), count=1)
if count != 1:
    raise SystemExit("Expected the legacy primary output sampling-rate profile")
path.write_text(text)
PY
adb push "$policy" /system/etc/audio_policy.conf
adb shell setprop ctl.restart audioserver
for attempt in {1..20}; do
  audio="$(adb shell dumpsys media.audio_flinger)"
  if python3 -c 'import re,sys; s=sys.stdin.read(); frames=re.findall(r"HAL frame count: (\d+)", s); sys.exit(not ("Sample rate: 48000 Hz" in s and frames and all(int(n) % 16 == 0 for n in frames)))' <<< "$audio"; then
    printf '%s\n' "$audio"
    exit 0
  fi
  sleep 0.5
done
printf '%s\n' "$audio"
echo 'Android 7 emulator audio did not reach a block-aligned 48 kHz configuration' >&2
exit 1
