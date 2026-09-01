# El Piedrero 📱🃏🪨
### Open Source Stone & Call Tracker for the Canary Ronda Card Game

[🇪🇸 Español](README.md) • [🇬🇧 English](README_EN.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-24%2B-brightgreen.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Android CI](https://github.com/DevNaranjo/El-Piedrero/actions/workflows/android.yml/badge.svg)](https://github.com/DevNaranjo/El-Piedrero/actions)

**El Piedrero** is a native open-source Android application designed to keep track of scores ("piedras") in the traditional **Canary Ronda** card game conveniently, automatically, and 100% offline (no Internet connection required).

Play with a single phone in the center of the table or synchronize scores across multiple devices via **Local Wi-Fi and QR codes**, playing authentic sound effects for each traditional call and play (*Ronda, Parranda, Caracol, Caracolillo, Majo, Limpiar, Majo y Limpio, and ¡Buenas!*), featuring a persistent history of the **last 30 games**.

---

## 📥 Direct Download

If you simply want to install and play with family and friends:
* Download the ready-to-use installer: **[`ElPiedrero.apk`](ElPiedrero.apk)**.
* Compatible with any Android smartphone or tablet running **Android 7.0 (Nougat)** or higher.

---

## ✨ Key Features

* 📱 **Local Mode (Center Table):** A single smartphone in the center of the table tracks scores for all teams. Zero setup, no Wi-Fi needed.
* 🌐 **Local Network Multiplayer (Wi-Fi + QR):**
  * The host opens the table and displays a QR code.
  * Players join instantly by scanning the QR with their camera (no IP addresses to type).
  * **Anti-Cheating & Team Permissions:** Each player can only add points to their own team; opponent scorecards remain locked to prevent mistakes or cheating.
  * **Synchronized Sound Cues:** When a player calls a play, the associated sound triggers simultaneously across all phones on the table.
* 📜 **Game History (Last 30 Games):** Automatic rotation tracking the last 30 finished games with date, time, 21-stone winner, and team point breakdown.
* 👥 **Dynamic Table Capacity:**
  * **2 Players** (1 vs 1).
  * **3 Players** (Trio with 3 independent teams: A, B, and C).
  * **4 Players** (2 vs 2 in pairs).
* 🛡️ **Network Security & Performance:**
  * Secure NDJSON protocol with DoS protection (>32 KB frame cutoff).
  * Strict nominal score validation on the Host server.
  * Reusable byte buffer in CameraX preventing Garbage Collector frame stutter at 60 FPS.

---

## 🃏 Canary Ronda Scoring System

The game is played to a total of **21 Stones ("Piedras")**:
* **11 Bad Stones / "Malas" (0 to 11):** The initial phase of the match.
* **10 Good Stones / "Buenas" (12 to 21):** Upon scoring stone 11, the team automatically enters **"Buenas"**, triggering the traditional **"¡Buenas!"** audio cue and haptic vibration across the table.
* **Victory:** The first team to complete 10 Buenas (21 total stones) wins the match.

### 🎵 Official Calls and Table Plays

| Play / Call | Stones Added | Traditional Description | Native Audio (`assets/`) |
| :--- | :---: | :--- | :--- |
| **Ronda** | **+1** | 2 identical cards in hand. | 🔊 `Ronda.mp3` |
| **Parranda** | **+3** | 3 identical cards in hand. | 🔊 `Parranda.mp3` |
| **Caracol** | **+4** | 3 consecutive cards in hand. | 🔊 `Caracol.mp3` |
| **Caracolillo** | **+5** | 3 consecutive cards of the same suit. | 🔊 `Caracolillo.mp3` |
| **Majo** | **+1** | Playing a card identical to the one just played by the preceding player. | 🔊 `Majo.mp3` |
| **Limpiar** | **+1** | Collecting all cards and leaving the table completely clear. | 🔊 `Limpio.mp3` |
| **Majo y Limpio** | **+2** | Matching opponent's card and clearing the table at the same time. | 🔊 `Majo-y-limpio.mp3` |
| **Manual Adjust (+ / -)** | **+1 / -1** | Manual score adjustment at any time. | 📳 Click / Haptic |

---

## 🚀 Recent Changelog (v1.0)

* 🏷️ **New Identity "El Piedrero":** Complete update of brand, titles, and system manifest.
* 📜 **Persistent Game History:** Rotating history of the last 30 games with detailed scorecards (`HistoryScreen.kt` & `GameHistoryRepository.kt`).
* 🃏 **New Traditional Plays:** Added *Limpiar (+1)*, *Majo (+1)*, *Majo y Limpio (+2)*, and *Caracolillo (+5)* with associated audio and haptics.
* 🛡️ **Network Hardening & Anti-Cheating:** Strict 32 KB frame limit on TCP sockets against DoS attacks and nominal score validation on the Host server.
* ⚡ **Performance Optimization:** Reusable byte buffer in CameraX to eliminate GC stutter at 60 FPS and batch JNI pixel rendering for QR code generation.
* 🧪 **Automated Testing & CI/CD:** Unit test suite for scoring rules (`TeamScoreTest.kt`) and GitHub Actions integration workflow.
* 🌍 **Open Source Release:** Structured repository under the MIT License with `CONTRIBUTING.md` and portable Gradle Toolchains compilation.

---

## 🏗️ Architecture & Tech Stack

The project follows **Clean Architecture** and reactive **MVVM/MVI**:

```
app/src/main/java/com/app/rondacanaria/
├── data/
│   ├── audio/        # RondaAudioPlayer (sound playback with synthetic fallback & haptics)
│   ├── history/      # GameHistoryRepository (offline JSON persistence for 30 games)
│   ├── model/        # Network models, NDJSON envelopes, CantoType, TeamScore
│   └── network/      # SocketServer, SocketClient, and local networking utilities
├── domain/
│   ├── model/        # ConnectionInfo (room data encoded in QR code)
│   └── usecase/      # HostGameUseCase & ClientGameUseCase (game & network logic)
└── ui/
    ├── qr/           # QrCodeGenerator (JNI-accelerated) & QrCameraScanner (CameraX)
    ├── screens/      # ModeSelection, Lobby, HostLobby, Scanner, Scoreboard, History
    ├── MainActivity.kt
    └── ScoreViewModel.kt
```

---

## 🛠️ Building & Development

### Prerequisites
* Android Studio Iguana (2023.2.1) or newer.
* JDK 17 (automatically downloaded and managed via Gradle Toolchains).

### Terminal Commands
```bash
# Clone the repository
git clone https://github.com/DevNaranjo/El-Piedrero.git
cd El-Piedrero

# Run unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug
```

---

## 🤝 Contributing

Contributions are welcome! Please check the [Contributing Guide](CONTRIBUTING_EN.md) for code style guidelines and Pull Request workflows.

---

## 📄 License

This project is licensed under the **MIT License** in the name of **DevNaranjo (2026)**. See the [LICENSE](LICENSE) file for details.
