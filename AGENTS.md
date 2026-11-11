# Working on Barid

Barid (برید) is an open-source (GPL-3.0) Android app that forwards Iranian bank transaction SMS to a paired server. It is made by Darakav; Darakav's server (`/api/sms/ingest`, private repository) is the built-in pairing target. Read README.md first.

## Rules

- Public repository: never commit keys, `keystore.properties`, `*.jks`, real SMS text, phone numbers, personal data or logs. Test fixtures use synthetic messages only.
- Privacy is the product: nothing leaves the phone except bank transaction messages to the paired server. One-time passwords are never forwarded. No analytics, ads or third-party SDKs that send data.
- Keep pairing compatible with Darakav: setup code `darakav-sms:1:<base64url JSON {u,k,n}>` (u = ingest URL, k = phone key, n = phone label), link `darakav://pair?c=<setup code>`, `POST` JSON `{"messages":[{from,text,receivedStamp}]}` with `Authorization: Bearer <key>`; 401 means the key was revoked.
- Package and application ID `com.darakav.barid` never change (store updates depend on it).
- Bilingual: every user-facing string exists in `values/` (English) and `values-fa/` (Persian), and layouts work in RTL.
- Checks before every commit: `./gradlew testDebugUnitTest assembleDebug lintDebug` (from Iran use the Docker method in README.md). Report failures honestly.
- Commits: Conventional Commits (`type(scope): summary`), split by feature, no AI attribution. Work on a branch and open a pull request to `main`; do not push to `main` directly once the repository is public.

## State

- 2026-11-11: imported from Darakav's `android/` app (fresh history) and renamed; then fully redesigned in Jetpack Compose (owner-approved direction): Welcome, Connect (CameraX + ZXing scanner), Home (status, counts, Recent, battery and SMS-permission cards), Settings (theme, Material You + Darakav presets, reduce transparency, language), Recent, Privacy & open source.
- Design source of truth: Darakav's frontend tokens (`frontend/src/styles.css`, `palette-state.ts` in the Darakav repo), mirrored in `ui/theme/Theme.kt`. Glass = Haze blur + translucent tint + bright top edge (`ui/components/Surfaces.kt`).
- Visual check without a phone: Paparazzi 1.3.5 renders Compose screens on the JVM (layoutlib comes from Google Maven, use the mirror). Force `LocalLayoutDirection` to RTL for Persian; Paparazzi does not apply it to Compose.
- Next: support for any server (custom URL, header and payload) and a public pairing-QR spec. The connect sheet already shows "Use another server" as Soon.
- Before making the repository public: set the website (https://darakav.com) and topics, turn off Projects, allow squash merging only, auto-delete head branches, enable Dependabot alerts, private vulnerability reporting and secret scanning.
