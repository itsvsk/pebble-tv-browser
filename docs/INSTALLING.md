# Install Pebble on Android TV

Requires **Android TV 7.0 (API 24) or newer** and a current **Android System WebView**.

## Choose the installer

Open the [latest GitHub release](https://github.com/itsvsk/pebble-tv-browser/releases/latest), expand **Assets**, and download the file ending in **`.apk`**. For example, the v0.1.0 installer is `pebble-v0.1.0.apk`. Source-code archives and `mapping.txt` are development files, not installers.

## Install with a file manager

1. Transfer the APK to your TV over your local network or a USB drive.
2. Open it with a file manager. If Android asks, allow that file manager to install apps, then return to the APK and install it.
3. Open **Pebble** from the TV's app launcher.

Home starts without preinstalled Speed Dials. Enter a URL or search query; frequently visited pages will appear as you browse.

## Install with ADB

Enable debugging on the TV and use its supported USB or wireless connection. For wireless debugging, use the current address shown on the TV. If it asks you to pair first, run this command and enter the pairing code shown on the TV:

```sh
adb pair TV_IP:PAIRING_PORT
```

The pairing port and connection port may differ. Replace the placeholders below with the TV's current connection address and the APK you downloaded. Selecting the device explicitly avoids installing on another connected emulator or TV.

```sh
adb connect TV_IP:DEBUGGING_PORT
adb -s TV_IP:DEBUGGING_PORT install -r path/to/pebble-vX.Y.Z.apk
adb -s TV_IP:DEBUGGING_PORT shell am start -n com.tvbrowser.app/com.tvbrowser.app.MainActivity
```

For USB, use `adb devices` to find the TV's serial and pass it to `adb -s` in place of the wireless address. Pairing is only needed when the TV's wireless-debugging flow requests it.

## Verify the download

Download `SHA256SUMS` from the same release as the APK and keep both files in the same folder. In that folder, run:

```sh
# macOS
shasum -a 256 -c SHA256SUMS

# Linux
sha256sum -c SHA256SUMS
```

The APK should report **OK**. Releases also include the public signing certificate.

## Update an existing installation

Official releases share one signing key for updates. An experimental APK signed with another key cannot be updated in place; uninstalling it removes its app data. Debug builds use `com.tvbrowser.app.debug` and can coexist with the release app.

If Android reports a signature conflict, check whether the installed copy is an experimental build before uninstalling anything.

[Back to the product overview](../README.md)
