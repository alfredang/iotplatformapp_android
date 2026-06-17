# IoTFlow Android

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-757575?logo=materialdesign&logoColor=white)
![Min SDK 24](https://img.shields.io/badge/min%20SDK-24-3DDC84?logo=android&logoColor=white)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)

A native **Kotlin + Jetpack Compose** Android client for the
[IoTFlow](https://iot.tertiaryinfotech.com/) self-hosted IoT platform
([backend](https://github.com/alfredang/iotplatform) ·
[iOS app](https://github.com/alfredang/iotplatformapp)). Add devices, monitor
live telemetry and keep an eye on alerts — from your Android phone.

<p align="center">
  <img src="screenshot.png" alt="IoTFlow Android — dashboard" width="300">
</p>

## Screenshots
| Login | Dashboard | Devices | Add device | Settings |
|---|---|---|---|---|
| <img src="play-assets/screenshots/01_login.png" width="150"> | <img src="play-assets/screenshots/02_dashboard.png" width="150"> | <img src="play-assets/screenshots/03_devices.png" width="150"> | <img src="play-assets/screenshots/04_add_device.png" width="150"> | <img src="play-assets/screenshots/05_settings.png" width="150"> |

## Features
- 🔐 Sign in / register against the IoTFlow backend (Auth.js credentials, cookie session)
- 📊 Dashboard — total / online / offline counts, active alerts, latest telemetry and recent alerts, pull-to-refresh
- 🧩 Devices — live online/offline status, per-device detail, swipe/tap to delete
- ➕ Add a device — name, type, location, protocol (HTTP / MQTT / WebSocket); copyable one-time device token
- ⚙️ Settings — account info, configurable server URL, sign out
- ▶️ Demo mode — explore the full UI with sample data, no backend needed

## Tech stack
| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Networking | OkHttp (persistent CookieJar → NextAuth CSRF + credentials flow) |
| JSON | kotlinx.serialization |
| Architecture | `ApiClient` (object) · `SessionViewModel` · Compose screens |
| Persistence | SharedPreferences (server URL, session cookies, demo flag) |
| Min SDK | 24 (Android 7.0) · Target 36 |

## Backend API
| Endpoint | Use |
|---|---|
| `GET /api/auth/csrf` + `POST /api/auth/callback/credentials` | Sign in |
| `POST /api/auth/register` | Create account |
| `GET /api/auth/session` | Restore session |
| `GET /api/dashboard/summary` | Dashboard counts, telemetry, alerts |
| `GET/POST /api/devices` · `DELETE /api/devices/:id` | List / add / remove devices |

## Build
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:bundleRelease      # signed release AAB (needs keystore.properties)
```

Release signing reads `keystore.properties` (gitignored). The signed bundle is
written to `app/build/outputs/bundle/release/app-release.aab`.

See [play-assets/STORE_LISTING.md](play-assets/STORE_LISTING.md) for the Google
Play listing copy, screenshots and submission checklist.
