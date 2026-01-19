# 🎬 CineCam - Professional Cinematic Camera

A professional Android camera application with **real-time AI-powered background blur (bokeh)**, **high-quality video recording**, and **manual cinematic controls**.

![Android](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple)
![API](https://img.shields.io/badge/API-24%2B-brightgreen)

## ✨ Features

### 🎥 Cinematic Video Recording
- **High Bitrate Recording**: Up to 100 Mbps for maximum quality
- **Multiple Resolutions**: 720p, 1080p, 4K UHD
- **Cinematic Frame Rates**: 24, 30, 60 fps
- **180° Shutter Rule**: Automatic calculation for natural motion blur

### 📷 AI-Powered Bokeh
- **Real-time Background Blur**: MediaPipe Selfie Segmentation
- **Adjustable Intensity**: Control blur strength from subtle to dramatic
- **Edge Feathering**: Smooth transitions between subject and background

### 🎛️ Professional Manual Controls
- **ISO**: 100 - 6400
- **Shutter Speed**: 1/30 to 1/2000
- **Manual Focus**: Smooth focus slider with distance markers
- **Exposure Compensation**: ±2 EV

### 🎨 Cinematic Look
- **Aspect Ratios**: 16:9, 2.35:1 (Cinemascope), 1.85:1, 1:1
- **LUT Support**: Real-time color grading (Orange & Teal, Film Emulation, Noir)
- **Log Profile**: Flat recording for post-production grading

### 🔧 Technical Features
- **Electronic Stabilization (EIS)**: Software-based stabilization
- **External Mic Support**: AAC audio encoding
- **Clean Architecture**: MVVM with Hilt DI

## 📱 Requirements

- **Android 7.0** (API 24) or higher
- **OpenGL ES 3.0** support
- Camera with manual control support (recommended)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Compose UI  │  │  ViewModel  │  │     Components      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Use Cases  │  │   Models    │  │    Repository IF    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    Processing Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   CameraX   │  │  MediaPipe  │  │     OpenGL ES       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Build & Run

```bash
# Clone the repository
git clone https://github.com/yourusername/cinematiccamera.git

# Open in Android Studio
cd cinematiccamera

# Build the project
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### Download MediaPipe Model

Download the selfie segmentation model and place it in `app/src/main/assets/`:

```bash
# Download from MediaPipe
curl -o app/src/main/assets/selfie_segmenter.tflite \
  https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite
```

## 📂 Project Structure

```
app/
├── src/main/
│   ├── java/com/cinecam/cinematiccamera/
│   │   ├── ui/                    # Compose UI & Activities
│   │   │   ├── camera/            # Camera screen
│   │   │   ├── settings/          # Settings screen
│   │   │   ├── components/        # Reusable UI components
│   │   │   └── theme/             # App theme
│   │   ├── viewmodel/             # ViewModels
│   │   ├── domain/                # Business logic
│   │   │   ├── model/             # Data classes
│   │   │   ├── repository/        # Repository interfaces
│   │   │   └── usecase/           # Use cases
│   │   ├── data/                  # Repository implementations
│   │   ├── processing/            # Camera & AI processing
│   │   │   ├── camera/            # CameraX integration
│   │   │   └── segmentation/      # MediaPipe + Blur
│   │   └── di/                    # Hilt modules
│   ├── res/                       # Resources
│   └── assets/                    # ML models & LUTs
```

## 🎬 Usage

1. **Launch the app** and grant camera/audio permissions
2. **Frame your shot** using the live preview
3. **Enable Bokeh** by tapping the blur icon
4. **Adjust settings**:
   - Slide focus for manual control
   - Rotate exposure wheel for brightness
   - Select aspect ratio for cinematic look
5. **Tap the record button** to start/stop recording
6. **Find your videos** in `Movies/CineCam/`

## 📸 Screenshots

| Camera View | Settings | Recording |
|-------------|----------|-----------|
| *Coming soon* | *Coming soon* | *Coming soon* |

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose + Material3 |
| Camera | CameraX 1.3 |
| AI/ML | MediaPipe Vision Tasks |
| DI | Hilt 2.48 |
| Async | Kotlin Coroutines + Flow |
| Architecture | Clean Architecture + MVVM |

## 📜 License

MIT License - see [LICENSE](LICENSE) for details.

## 🤝 Contributing

Contributions are welcome! Please read our contributing guidelines first.

---

**Made with ❤️ for filmmakers and content creators**
