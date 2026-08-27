# MimoTTS Android App

A custom Text-to-Speech (TTS) engine for Android that leverages the Xiaomi Mimo API to deliver high-quality Chinese speech synthesis. This app registers as a system TTS provider, allowing any app on your device to use Mimo's TTS capabilities.

## Features

- **System TTS Integration**: Registers as an Android TTS engine — use it anywhere in the system
- **Xiaomi Mimo API**: Powered by `api.xiaomimimo.com` for high-quality speech synthesis
- **Jetpack Compose UI**: Modern Material 3 design with adaptive navigation
- **Voice Asset Management**: Browse and manage voice assets from the API
- **Streaming Audio**: HTTP streaming with ExoPlayer for low-latency playback
- **Configurable Settings**: API key, voice selection, and model configuration

## Requirements

- Android 7.0 (API 24) or higher
- Internet connection
- Xiaomi Mimo API key

## Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/indusy55/MimoTTSAndroidApp.git
   ```

2. **Open in Android Studio**
   - Open the project in Android Studio
   - Sync Gradle files

3. **Configure API Key**
   - Launch the app
   - Navigate to **Settings**
   - Enter your Mimo API key

4. **Enable as System TTS**
   - Go to Android **Settings** > **System** > **Languages & input** > **Text-to-speech output**
   - Select **MimoTTS** as your preferred engine

## Architecture

```
app/
├── src/main/java/com/indusy55/mimottsapp/
│   ├── MainActivity.kt              # Main entry point with NavigationSuiteScaffold
│   ├── data/
│   │   ├── api/                     # Retrofit API interfaces and data classes
│   │   └── models/                  # Voice assets and data models
│   ├── service/
│   │   ├── MimoTtsService.kt        # Core TTS engine implementation
│   │   └── TtsDataCheckActivity.kt  # System TTS data check handler
│   └── ui/
│       ├── MimoViewModel.kt         # ViewModel for API and state management
│       ├── screens/                 # Compose UI screens
│       └── theme/                   # Material 3 theme configuration
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Networking**: Retrofit + OkHttp
- **Media**: ExoPlayer (Media3)
- **Architecture**: MVVM with ViewModel
- **Navigation**: NavigationSuiteScaffold (adaptive)

## Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Connect to Mimo API |
| `RECORD_AUDIO` | Required for TTS engine registration |

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Xiaomi Mimo API](https://api.xiaomimimo.com) for TTS capabilities
- Android Text-to-Speech framework
- Jetpack Compose and Material 3
