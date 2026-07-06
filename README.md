# Passport Photo 📸 (Android)

> Turn a selfie into a clean, compliant passport photo — background removed, distractions filtered out, and cropped to an official size. **100% on-device.**

![Platform](https://img.shields.io/badge/platform-Android%207%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![ML Kit](https://img.shields.io/badge/Google-ML%20Kit-EA4335?logo=google&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-green)

The Android sibling of the iOS app [Tertiary Passport Photo](https://apps.apple.com/us/app/tertiary-passport-photo/id6782430909) — same features, built natively with Kotlin, Jetpack Compose, and Google ML Kit.

## ✨ Features

- **One-tap selfie capture** with a front-camera view and a head-positioning oval guide (CameraX).
- **Automatic background removal** using ML Kit selfie segmentation — isolates you and drops everything else.
- **Pure white background** compositing that meets passport / visa requirements.
- **Distraction & noise filtering** — clutter behind you is removed with a soft, graded mask edge.
- **Face-aware cropping** that sizes and positions your head to official specifications (head height & eye line), using ML Kit face detection with real eye landmarks.
- **Per-country sizes** — pick your country (🇸🇬 Singapore by default) and the photo is cropped to that country's official passport dimensions.
- **300 DPI print-ready output** rendered at the exact pixel dimensions each authority expects (DPI stamped in EXIF).
- **Save to Gallery or Share** directly from the app.
- **Feedback & About tabs** — message us on WhatsApp, view app/developer info.
- **Privacy first** — every image is processed on-device. Nothing is uploaded.

## 📐 Supported Countries

The default is **Singapore (35 × 45 mm)**. Output pixel sizes are at 300 DPI.

| Country | Physical size | Output (300 DPI) |
|---------|---------------|------------------|
| 🇸🇬 Singapore *(default)* | 35 × 45 mm | 413 × 531 px |
| 🇺🇸 United States | 51 × 51 mm (2 × 2 in) | 602 × 602 px |
| 🇬🇧 United Kingdom | 35 × 45 mm | 413 × 531 px |
| 🇪🇺 EU / Schengen | 35 × 45 mm | 413 × 531 px |
| 🇦🇺 Australia | 35 × 45 mm | 413 × 531 px |
| 🇨🇦 Canada | 50 × 70 mm | 591 × 827 px |
| 🇮🇳 India | 35 × 45 mm | 413 × 531 px |
| 🇨🇳 China | 33 × 48 mm | 390 × 567 px |
| 🇯🇵 Japan | 35 × 45 mm | 413 × 531 px |
| 🇲🇾 Malaysia | 35 × 50 mm | 413 × 591 px |

## 🧠 How It Works

```
 Selfie  ──►  ML Kit selfie segmentation  ──►  Composite over white  ──►  Face-aware crop  ──►  Passport photo
             (background/clutter removed)      (per-pixel blend)         (head size + eye line)    (300 DPI)
```

1. **Segment** — ML Kit `SelfieSegmenter` (single-image mode) produces a per-pixel person-confidence mask at full input resolution.
2. **Composite** — each pixel is blended over pure white by its confidence, giving a soft, natural cut-out edge.
3. **Crop** — ML Kit `FaceDetection` (accurate mode + landmarks) locates the face and eyes; the crop is scaled so the head fills the spec's target fraction and the eye line sits at the required height, padding with white where needed.

## 🏗️ Tech Stack

- **Kotlin 2.0** / **Jetpack Compose** (Material 3) — declarative, native UI (Android 7.0+, API 24)
- **Google ML Kit** — on-device selfie segmentation & face detection (models bundled, no network)
- **CameraX** — front-camera capture with lifecycle-aware binding
- **Android Photo Picker** — permissionless library import
- **MediaStore + FileProvider** — gallery save and share sheet

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Ladybug or newer) with JDK 17+
- Android SDK 36 (compile/target), device or emulator on **Android 7.0+ (API 24)**

### Build & Run

```bash
git clone https://github.com/alfredang/passportphotoapp_android.git
cd passportphotoapp_android

./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press **Run**.

> Segmentation works best with a real person in frame. The emulator has a simulated camera — use **Choose from Library** there.

### Release build

Release signing reads a gitignored `keystore.properties` at the project root:

```properties
RELEASE_STORE_FILE=/path/to/upload-keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

```bash
./gradlew :app:bundleRelease
# AAB: app/build/outputs/bundle/release/app-release.aab
```

## 📁 Project Structure

```
passportphotoapp_android/
├── app/
│   ├── build.gradle.kts                 # Module config + release signing
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                         # Launcher icons, theme, FileProvider paths
│       └── java/com/tertiaryinfotech/passportphoto/
│           ├── MainActivity.kt          # App entry point
│           ├── PhotoSpec.kt             # Per-country size specifications
│           ├── PassportPhotoProcessor.kt# ML Kit segmentation + crop engine
│           ├── ImageIO.kt               # Decode / save (300 DPI) / share helpers
│           └── ui/
│               ├── RootScreen.kt        # Bottom nav: Photo · Feedback · About
│               ├── MakePhotoScreen.kt   # Main flow: hero, country picker, actions
│               ├── CameraScreen.kt      # CameraX capture + oval guide
│               ├── ResultScreen.kt      # Preview, save & share
│               ├── FeedbackScreen.kt    # Feedback tab (WhatsApp)
│               ├── AboutScreen.kt       # About tab (app + developer info)
│               └── Theme.kt             # Design tokens (Material 3)
├── gradle/libs.versions.toml            # Version catalog
├── CHANGELOG.md                         # Release notes (source of truth for Play)
└── PRIVACY.md                           # Privacy policy (on-device, no data collection)
```

## 🔒 Privacy

Passport Photo never sends your images anywhere. Capture, background removal, cropping, and export all happen locally on your device. See [PRIVACY.md](PRIVACY.md).

## 🍎 iOS Version

The iOS app is available on the App Store: [Tertiary Passport Photo](https://apps.apple.com/us/app/tertiary-passport-photo/id6782430909).

## 🙏 Acknowledgements

- [Google ML Kit](https://developers.google.com/ml-kit) — on-device selfie segmentation & face detection
- [Jetpack Compose](https://developer.android.com/compose) & [CameraX](https://developer.android.com/media/camera/camerax)
- Developed by [Tertiary Infotech Academy Pte. Ltd.](https://www.tertiaryinfotech.com)

## 📄 License

MIT
