# AIC Gaming AWS Client

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![WebRTC](https://img.shields.io/badge/WebRTC-333333?style=for-the-badge&logo=webrtc&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-%23FF9900.svg?style=for-the-badge&logo=amazon-aws&logoColor=white)

AIC (Android in Cloud) Gaming AWS Client is a native Android application designed to facilitate low-latency cloud gaming. It connects to an AWS-hosted gaming instance, streams the video/audio output via WebRTC, and seamlessly sends touch control inputs back to the server. 

This client is specifically optimized for interacting with cloud environments that utilize Scrcpy-based control mechanisms, making it ideal for streaming mobile games or Android environments hosted in the cloud.

## Features

- **Low-Latency Streaming:** Utilizes WebRTC (`io.getstream:stream-webrtc-android`) to ensure high-performance, real-time video and audio streaming from the AWS cloud instance.
- **WebSocket Signaling:** Built-in signaling client using OkHttp to negotiate WebRTC sessions (Offer, Answer, ICE Candidates) via a standard WebSocket server.
- **Cloud Touch Injection:** Captures multi-touch events on the local Android device, normalizes the coordinates, and transmits them over WebRTC Data Channels using the Scrcpy Control Message format.
- **Modern UI:** Built entirely with Jetpack Compose, offering a responsive and clean user interface.

## Architecture

The client application follows a standard modern Android architecture:
- **Signaling (`SignalingClient.kt`):** Connects to the AWS signaling server via WebSockets. It exchanges SDP (Session Description Protocol) and ICE candidates to establish the peer-to-peer WebRTC connection.
- **Streaming (`WebRTCManager.kt`):** Manages the WebRTC peer connection, attaching remote video tracks to the Compose UI.
- **Controls (`TouchHandler.kt`):** Intercepts Android `MotionEvent`s on the video surface, serializes them into byte buffers compliant with the Scrcpy touch injection protocol (`SC_CONTROL_MSG_TYPE_INJECT_TOUCH_EVENT`), and sends them across the WebRTC `DataChannel`.
- **UI (`MainScreen.kt` & `MainScreenViewModel.kt`):** Uses Jetpack Compose and ViewModels to manage the application state and display the WebRTC video sink.

## Prerequisites

- **Android Studio:** Ladybug or newer recommended.
- **Android SDK:** 
  - Minimum SDK: 24 (Android 7.0)
  - Target SDK: 36
- **Backend Infrastructure:** An active AWS backend running:
  1. A WebSocket signaling server.
  2. A WebRTC streaming service (e.g., Anbox Cloud, Redroid, or a custom WebRTC/Scrcpy bridge).

## Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/AIC_gaming_AWS_client.git
```

### 2. Open the Project
Open the `aic-client` directory in Android Studio. The Gradle sync should start automatically.

### 3. Configure the Signaling Server URL
You need to point the client to your AWS signaling server. Update the connection URL inside the app. This is typically passed to the `SignalingClient` upon initialization (e.g., `ws://your-aws-instance-ip:8080/signaling`).

### 4. Build and Run
- Connect a physical Android device or start an emulator (a physical device is highly recommended for testing WebRTC and touch controls).
- Click **Run** in Android Studio (or use `./gradlew installDebug`).

## Project Structure

```text
aic-client/
├── app/
│   ├── src/main/java/com/example/aicclient/
│   │   ├── data/                 # Repositories and data models
│   │   ├── theme/                # Jetpack Compose theme (Colors, Type)
│   │   ├── ui/main/              # Main UI screens and ViewModels
│   │   ├── MainActivity.kt       # Application entry point
│   │   ├── SignalingClient.kt    # WebSocket WebRTC signaling
│   │   ├── TouchHandler.kt       # Scrcpy touch event serialization
│   │   ├── WebRTCManager.kt      # PeerConnection and track management
│   │   └── ...
│   └── build.gradle.kts          # App-level build configurations
└── build.gradle.kts              # Project-level build configurations
```

## Control Protocol Details
The application sends touch events over the WebRTC Data Channel as binary data. The payload is structured as follows (Scrcpy protocol format):
- `1 byte`: Message Type (2 for `INJECT_TOUCH_EVENT`)
- `1 byte`: Action (0: DOWN, 1: UP, 2: MOVE)
- `8 bytes`: Pointer ID
- `4 bytes`: X Coordinate (Normalized against screen width)
- `4 bytes`: Y Coordinate (Normalized against screen height)
- `2 bytes`: Video Width
- `2 bytes`: Video Height
- `2 bytes`: Pressure
- `4 bytes`: Buttons

## Dependencies
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI Toolkit
- [Stream WebRTC Android](https://github.com/GetStream/webrtc-android) - WebRTC implementation
- [OkHttp](https://square.github.io/okhttp/) - WebSocket & Networking
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - Asynchronous programming

## License
[Add your license information here, e.g., MIT, Apache 2.0]
