# Install Pebble on Android TV

Download the APK from [GitHub Releases](https://github.com/itsvsk/pebble-tv-browser/releases/latest). Requires Android TV 7.0 (API 24) or newer and a current Android System WebView. The source-code ZIP is for development, not installation.

Transfer the APK to the TV, allow installation from your chosen file manager when Android asks, then open the APK. Launch Pebble from the TV app launcher. Pebble is distributed as a GitHub APK release.

## Install with ADB

Enable wireless debugging or connect by USB. Pair first if the TV requires pairing; the pairing port and connection port may differ. Replace the address below with the TV's current debugging address. These commands install and launch the app without changing the TV's display settings.

```sh
adb connect TV_IP:DEBUGGING_PORT
adb install -r pebble-v0.1.0.apk
adb shell am start -n com.tvbrowser.app/com.tvbrowser.app.MainActivity
```

Releases include `SHA256SUMS` and the public signing certificate. Verify the APK with `sha256sum -c SHA256SUMS` on Linux or `shasum -a 256 -c SHA256SUMS` on macOS.

Official releases share one signing key for updates. An experimental APK signed with another key cannot be updated in place; uninstalling it removes its app data. Debug builds use `com.tvbrowser.app.debug` and can coexist with the release app.

[Back to the product overview](../README.md)
