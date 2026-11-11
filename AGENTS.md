# Working on Barid

Barid (برید) is an open-source (GPL-3.0) Android app that forwards Iranian bank transaction SMS to a paired server. It is made by Darakav; Darakav's server (`/api/sms/ingest`, private repository) is the built-in pairing target. Read README.md first.

## Rules

- Public repository: never commit keys, `keystore.properties`, `*.jks`, real SMS text, phone numbers, personal data or logs. Test fixtures use synthetic messages only.
- Privacy is the product: nothing leaves the phone except bank transaction messages to the paired server. One-time passwords are never forwarded. No analytics, ads or third-party SDKs that send data.
- Keep pairing compatible with Darakav: setup code `darakav-sms:1:<base64url JSON {u,k,n}>` (u = ingest URL, k = phone key, n = phone label), link `darakav://pair?c=<setup code>`, `POST` JSON `{"messages":[{from,text,receivedStamp}]}` with `Authorization: Bearer <key>`; 401 means the key was revoked.
- Any other server follows the public protocol in docs/SERVERS.md (`barid:1:` codes with u, k, h, n, t; `barid://pair`). Change it only compatibly (new optional fields) and update that page in the same change. A pairing that is not Darakav's over HTTPS (`Pairing.trusted`) is always confirmed by the owner before it is saved.
- Package and application ID `com.darakav.barid` never change (store updates depend on it).
- Bilingual: every user-facing string exists in `values/` (English) and `values-fa/` (Persian), and layouts work in RTL.
- Checks before every commit: `./gradlew testDebugUnitTest assembleDebug lintDebug` (from Iran use the Docker method in README.md). Report failures honestly.
- Commits: Conventional Commits (`type(scope): summary`), split by feature, no AI attribution. Work on a branch and open a pull request to `main`; do not push to `main` directly once the repository is public.

## State

- 2026-11-11: imported from Darakav's `android/` app (fresh history) and renamed; then fully redesigned in Jetpack Compose (owner-approved direction): Welcome, Connect (CameraX + ZXing scanner), Home (status, counts, Recent, battery and SMS-permission cards), Settings (theme, Material You + Darakav presets, reduce transparency, language), Recent, Privacy & open source.
- Design source of truth: Darakav's frontend tokens (`frontend/src/styles.css`, `palette-state.ts` in the Darakav repo), mirrored in `ui/theme/Theme.kt`. Glass = Haze blur + translucent tint + bright top edge (`ui/components/Surfaces.kt`).
- Visual check without a phone: Paparazzi 1.3.5 renders Compose screens on the JVM (layoutlib comes from Google Maven, use the mirror). Force `LocalLayoutDirection` to RTL for Persian; Paparazzi does not apply it to Compose.
- 2026-11-11: any-server support: Connect → Use another server (URL, key, header name, Barid JSON or a body template, Test / Send a test), `barid:1:` setup codes and `barid://pair` links, owner confirmation for non-Darakav servers, 401/403 and 422 handling for custom servers, public spec docs/SERVERS.md. Template servers get one message per request.
- 2026-11-11: delivery fix after owner testing (messages stuck as waiting): redirects followed within the same site, `Delivery` shared by the worker and the open app (on resume, after import, Send now), scheduling replaces a waiting retry, Home shows the last error. Repeats: `Seen` keeps hashed sender + normalized text for 120 days per server (cleared when the server URL changes or on disconnect); `ActivityLog.distinct` hides copies. Performance: Recent list is lazy per message, list glass skips the live blur (`glass(blur = false)`), formatters cached per locale.
- Android's regex engine (ICU) is stricter than the JVM used by unit tests: escape every literal brace (`\\{`, `\\}`). An unescaped `}` in `Payload` crashed every delivery on devices while all tests passed (fixed 2026-11-11). `Delivery` turns unexpected errors into a failed try; the last crash is kept on the phone (`CrashReport`) and shown on the next launch.
- Test builds for the owner's phone: `assemblePreview` (R8-optimized, debug-signed) with the persistent debug key in `~/.local/share/barid-dev/android` (README Docker command); builds before 2026-11-11 14:00 each had a throwaway key. Debug builds lag noticeably in Compose.
- Ideas not started: showing a pairing's setup code as a QR to move it to another phone; per-bank filters.
- 2026-11-11: release 1.0.0 (versionCode 1, tag v1.0.0). The repository was made public. F-Droid: metadata in `fastlane/metadata/android/{en-US,fa}`, recipe and steps in docs/FDROID.md; the owner submits the merge request to fdroiddata. GitHub releases carry an APK signed with Barid's release key (certificate SHA-256 a7e745ed02870a86d8261b174ae5e48ebe5ed6374793ca9b3775dfde5c01a82d, CN=Barid, O=Darakav). The key and its passwords live only on the owner's machine in `~/.config/barid/` (`barid-release.jks`, `keystore.properties`); never commit, print or upload them. Store screenshots are rendered with Paparazzi (synthetic data) at device resolution.
- Repository settings applied when it went public: website, topics, Projects off, squash merging only, auto-delete head branches, Dependabot alerts, private vulnerability reporting, secret scanning. From now on work on branches with pull requests to `main`.
