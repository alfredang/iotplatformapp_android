# IoTFlow — Google Play Store Listing

## App details
- **App name:** IoTFlow
- **Package / Application ID:** `com.tertiaryinfotech.iotflow`
- **Version:** 1.0 (versionCode 1)
- **Default language:** English (United States) – en-US
- **App or game:** App
- **Free or paid:** Free
- **Category:** Tools (alt: Productivity)
- **Contact email:** angch@tertiaryinfotech.com
- **Website:** https://iot.tertiaryinfotech.com

## Short description (max 80 chars)
Monitor your IoT devices, live telemetry and alerts from anywhere.

## Full description (max 4000 chars)
IoTFlow is the official mobile client for the IoTFlow self-hosted IoT platform
(https://iot.tertiaryinfotech.com). Connect your devices, stream real-time
telemetry, and stay on top of alerts — right from your phone.

KEY FEATURES
• Dashboard — see total, online and offline device counts and active alerts at
  a glance, plus the latest telemetry and most recent alerts. Pull to refresh.
• Devices — browse all your devices with live online/offline status, open a
  device to view its protocol, telemetry count, last-seen time and location.
• Add a device — name it, pick a type and protocol (HTTP REST, MQTT or
  WebSocket); the generated device token is shown once and copyable for flashing
  onto an ESP32, Arduino or Raspberry Pi.
• Alerts — threshold-based rule triggers and device-offline notifications.
• Settings — view your account, point the app at any self-hosted IoTFlow server,
  and sign out.
• Explore demo — try the full app with sample data, no account needed.

Built for makers, engineers and operations teams running environmental
monitoring, cold-chain logistics, industrial equipment monitoring and smart
building sensors. Bring your own self-hosted IoTFlow instance, or use the public
platform.

## Demo / review account
The app includes a built-in "Explore demo" button on the sign-in screen that
opens the entire app populated with sample data — no credentials required. Use
it for App Review. A live account can also be created via Sign up, or use the
public demo backend account:
- Email: admin@demo.io
- Password: password123

## Data safety (Play Console answers)
- Collects: Email address, name, password (for account sign-in to the IoTFlow
  backend). Device/telemetry data the user themselves registers.
- Data is transmitted over HTTPS to the user-configured IoTFlow server.
- No data shared with third parties. No ads. No location collected by the app.
- Account deletion: users manage/delete their account on their IoTFlow server.

## REQUIRED before submission
1. **Privacy policy URL** — Play requires one for apps with sign-in.
   https://iot.tertiaryinfotech.com/privacy does NOT exist yet (404). Publish a
   privacy policy and use its URL in Play Console → App content → Privacy policy.
2. A Google Play Developer account (one-time US$25) on the Google account used.

## Assets in this folder
- icon-512.png — 512×512 app icon (hi-res icon)
- feature-graphic-1024x500.png — feature graphic
- screenshots/ — 5 phone screenshots, 1080×2400 (login, dashboard, devices,
  add device, settings)

## Signed build to upload
- app/build/outputs/bundle/release/app-release.aab  (signed, ~11 MB)
- Upload keystore: keystore/upload-keystore.jks (alias: iotflow-upload). KEEP SAFE —
  it is the only key that can publish updates. Credentials in keystore.properties.
