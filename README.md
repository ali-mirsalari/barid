# Barid · برید

**Forward Iranian bank SMS to your finance app, privately.**
برید پیامک‌های تراکنش بانکی را امن به برنامهٔ مالی‌تان می‌رساند.

Barid is a small Android app that watches incoming SMS, keeps only bank transaction messages and sends them to a server you choose: [Darakav](https://darakav.com) (which makes Barid and is built in as a one-scan option), your own app, a webhook or an automation tool. One-time passwords and personal messages never leave the phone.

> Get it from [GitHub releases](https://github.com/ali-mirsalari/barid/releases) (F-Droid listing in progress).

## What it does

- **Pairs with one scan:** a QR code, a pairing link (`barid://pair?c=…`, `darakav://pair?c=…`) or a pasted setup code (`barid:1:…`, `darakav-sms:1:…`). Barid asks before connecting to any server other than Darakav's.
- **Works with any server:** an HTTP(S) address, an optional key in `Authorization: Bearer` or a header you name, and either Barid's JSON batch or your own body template. Server authors: see [docs/SERVERS.md](docs/SERVERS.md).
- **Filters on the phone:** a message needs an amount and, from a recognized bank, a transaction word or currency; from any other sender, a balance, deposit or withdrawal word (so invoices, shops and payment-app receipts stay on the phone). Never one-time codes (including transfer codes and messages with Android's code-autofill signature), never messages from personal mobile numbers (`SmsFilter`).
- **Works while closed:** a manifest `SMS_RECEIVED` receiver; multipart messages are joined.
- **Never loses a message:** a file queue in app storage (up to 2,000) and a WorkManager job that sends batches when online, with exponential backoff. It stops when the server rejects the key. Servers are expected to ignore repeats, so retries are safe.
- **Imports history:** optional (needs `READ_SMS`), bank messages from the last 60 days.
- **Shows what happened:** a Recent list with bank, deposit or withdrawal, amount and whether it was sent. It is read on the phone for display and never keeps the message text.
- **Stays running:** asks to be exempt from battery optimization, with a shortcut to Xiaomi's Autostart list, since those are the usual reasons forwarding silently stops.
- **Keeps the key private:** app-private storage, excluded from backups and device transfer.

## Design

Darakav's design language: an aurora backdrop with a faint grid, frosted-glass surfaces (real blur on Android 12+, near-solid below) and Darakav's navy brand tile. Colors: Material You from the wallpaper (Android 12+) or Darakav's presets Slate, Ocean, Iris, Teal, Clay and Forest, in light and dark, plus a Reduce transparency switch. English and Persian with full right-to-left layout, Persian digits and the Jalali calendar, Vazirmatn bundled. Built with Jetpack Compose and Material 3; QR codes are read with CameraX and ZXing, so no Google Play services are needed.

Requirements: Android 8.0+ (API 26).

## Privacy

Barid has no analytics, no ads and no server of its own. Messages go only to the server you connected, over HTTPS (plain HTTP is allowed only so you can test a server on your local network).

## Build

Android Gradle Plugin 8.7.3, Gradle 8.13 (wrapper), Kotlin 2.1, JDK 17–21, compile/target SDK 35, build-tools 35.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug   # app/build/outputs/apk/debug/app-debug.apk
```

The SDK location comes from `ANDROID_HOME` or `local.properties` (`sdk.dir=…`, not committed).

### From Iran

Google's SDK and Maven downloads (`dl.google.com`, `maven.google.com`) are unavailable from Iran. Use a mirror of Google's Maven, for example in `~/.gradle/gradle.properties`:

```properties
googleMavenMirror=https://maven.myket.ir
```

To build without installing the SDK, use Docker. Docker Hub images reach Iran through a registry mirror such as ArvanCloud (`"registry-mirrors": ["https://docker.arvancloud.ir"]` in `/etc/docker/daemon.json`). CircleCI's `cimg/android` image ships the SDK and JDK 21. Its SDK is readable only by the image's own user, so build a writable copy:

```sh
docker pull cimg/android:2026.10.1
cp -a . /tmp/barid && chmod -R a+rwX /tmp/barid && mkdir -p /tmp/barid-gradle && chmod a+rwX /tmp/barid-gradle
mkdir -p ~/.local/share/barid-dev/android && chmod a+rwX ~/.local/share/barid-dev/android
docker run --rm --network host -e GRADLE_USER_HOME=/g -v /tmp/barid-gradle:/g -v /tmp/barid:/w -w /w \
  -e ANDROID_USER_HOME=/k -v ~/.local/share/barid-dev/android:/k \
  cimg/android:2026.10.1 bash -c 'umask 000; ./gradlew --no-daemon -PgoogleMavenMirror=https://maven.myket.ir testDebugUnitTest assembleDebug lintDebug'
# APK: /tmp/barid/app/build/outputs/apk/debug/app-debug.apk
```

`--network host` avoids connection timeouts seen on Docker's bridge network. The `/k` mount keeps the debug signing key between runs; without it every container makes a new key and each test build must be uninstalled before the next one installs.

The GitHub workflow (`.github/workflows/build.yml`) builds, tests and lints the debug APK on every push and keeps it as a run artifact.

## Release

Create a release key once and keep it outside the repository (losing it means users must reinstall):

```sh
keytool -genkeypair -v -keystore ~/.config/barid/barid-release.jks -alias barid -keyalg RSA -keysize 4096 -validity 10000
```

Then create `keystore.properties` (ignored by Git):

```properties
storeFile=/home/<you>/.config/barid/barid-release.jks
storePassword=…
keyAlias=barid
keyPassword=…
```

For testing speed on a phone, `./gradlew assemblePreview` builds the optimized release code signed with the local debug key (installs over debug builds; not for distribution). Debug builds are much slower in Compose, especially in the first seconds.

Increase `versionCode` for every release; Android installs an update only when it is signed with the same key. Release APKs are built from the tag, not the working tree, with:

```sh
scripts/release-apk.sh 1.0.3 --upload   # dist/barid-1.0.3.apk, attached to the GitHub release v1.0.3
```

It clones the tag, builds and signs it in the Docker image above with `keystore.properties` and the keystore mounted read-only, checks the certificate and uploads the APK. F-Droid rebuilds the same tag and ships that APK's signature only if the two are identical ([docs/FDROID.md](docs/FDROID.md)). A plain `./gradlew assembleRelease` with `keystore.properties` works too, as long as it runs on the tagged commit.

Google Play requires its SMS permission declaration. Bazaar, Myket and F-Droid accept the app as is.

## License

Barid is free software under the [GNU General Public License v3.0](LICENSE). The name "Barid", the Barid logo and the Darakav name and logo are not covered by the license; see [TRADEMARKS.md](TRADEMARKS.md).
