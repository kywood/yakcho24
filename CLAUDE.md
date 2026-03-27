# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Yakcho24** is an Android app for real-time medicinal herb identification using a YOLOv8 TFLite model via the device camera. Built with Kotlin + Jetpack Compose, MVVM architecture, and Hilt for DI.

## Build & Development Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a specific unit test class
./gradlew test --tests "com.gamenuri.yakcho24.ExampleUnitTest"

# Lint check
./gradlew lint

# Clean build
./gradlew clean
```

## Architecture

**Single-activity app** with Jetpack Navigation Compose:

- `MainActivity` → `MainScreen` (Scaffold + BottomNavBar + NavGraph)
- 4 tab screens: Camera (default), HerbList, Map, MyPage
- Navigation routes defined in `ui/navigation/Screen.kt`

**Layer structure:**
- `camera/` — ML inference (`HerbClassifier.kt` wraps TFLite YOLOv8) and camera frame conversion (`YuvToRgbConverter.kt`)
- `ui/screen/` — Composable screens (only Camera screen is implemented; others are placeholders)
- `viewmodel/` — `CameraViewModel` holds recognition state; `HerbViewModel` is empty
- `di/` — Hilt modules (`AppModule`, `NetworkModule` are both empty stubs)

## Key Components

**HerbClassifier** (`camera/HerbClassifier.kt`):
- Loads `assets/best_float16.tflite` (6.4 MB YOLOv8 model)
- Input: 640×640 normalized RGB bitmap
- Output: `DetectionResult` with label, confidence (threshold: 0.25), and bounding box
- Uses NNAPI delegate on API 28+, falls back to CPU
- Applies NMS with IoU threshold 0.45

**CameraScreen** (`ui/screen/camera/CameraScreen.kt`):
- CameraX `ImageAnalysis` captures frames every 100ms
- `YuvToRgbConverter` converts YUV_420_888 → RGB bitmap via RenderScript
- GPS overlay via Fused Location Provider + Geocoder
- Detection result shown in overlay card

## Dependency Management

All versions are centralized in `gradle/libs.versions.toml`. Add new dependencies there first, then reference via `libs.*` alias in `app/build.gradle.kts`.

Key versions:
- Kotlin: 2.0.21, Compose BOM: 2024.09.00, AGP: 8.9.1
- TFLite: 2.16.1, CameraX: 1.4.1, Hilt: 2.52, Room: 2.7.0

**Note:** `.tflite` files in `assets/` must not be compressed — this is enforced in `app/build.gradle.kts` via `aaptOptions`.

## Permissions

The app requires CAMERA (hardware feature, required) + ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION. Both are requested at runtime in `CameraScreen` using `ActivityResultContracts`.

## Development Status

Most screens (`HerbListScreen`, `HerbMapScreen`, `LoginScreen`) are UI placeholders. The DI modules and `HerbViewModel` are empty stubs pending backend integration (Retrofit/Room not yet wired up).


## TODO

### 진행중
- [ ] 학습 라벨링 데이터 썸네일 추출


### 완료
- [x] 카메라 줌
- [x] 카메라 화면 + GPS
- [x] TFLite 추론 + 바운딩박스
- [x] GPU Delegate 적용
- [x] 지도 탭 구현 + 실시간 위치 아이콘
- [x] 상단 GPS 주소 동 단위까지 표기

## google api key
- AIzaSyDwmOfI73SZmctxW3WKG2_mXebNtmn_wUI

