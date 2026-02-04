<div align="center">
<img src="Web/logo.svg" width="128" height="128" style="border-radius: 20%">

# Lisyo
### Synchronized Music Listening for Android

[![Android](https://raw.githubusercontent.com/ziadOUA/m3-Markdown-Badges/master/badges/Android/Android.svg)](https://developer.android.com/android)
[![Kotlin](https://raw.githubusercontent.com/ziadOUA/m3-Markdown-Badges/master/badges/Kotlin/Kotlin.svg)](https://kotlinlang.org/)
[![NodeJS](https://raw.githubusercontent.com/ziadOUA/m3-Markdown-Badges/master/badges/NodeJS/NodeJS.svg)](https://nodejs.org/)
[![ExpressJS](https://raw.githubusercontent.com/ziadOUA/m3-Markdown-Badges/master/badges/Express/Express.svg)](https://expressjs.com/)
[![SocketIO](https://raw.githubusercontent.com/ziadOUA/m3-Markdown-Badges/master/badges/SocketIO/SocketIO.svg)](https://socket.io/)

Lisyo is a real-time synchronized music listening application for Android, allowing users to listen to music together in public or private rooms.

[Features](#-key-features) • [Architecture](#-architecture) • [Tech Stack](#-tech-stack)

</div>

---

## 🏗️ Architecture

Lisyo uses a **Hybrid Client-Server Architecture** to ensure low-latency synchronization while leveraging the vast library of YouTube Music.

### 1. Railway Backend (The Conductor)
The backend is a lightweight Node.js/Express server responsible for **State & Synchronization**.
*   **Room Management:** Handles creating, joining, and listing rooms.
*   **Session Management:** Tracks connected users and hosts.
*   **Precise Synchronization:** Calculates time offsets (NTP style) and broadcasts playback events with server timestamps.
*   **Shared State:** Manages the Song Queue and Chat messages.

### 2. Android Client (The Player)
The client is a native Android app built with Jetpack Compose.
*   **Audio Engine:** Uses **ExoPlayer** for high-quality playback.
*   **Stream Extraction:** Directly interacts with **YouTube's InnerTube API** to extract audio stream URLs on the device.
    *   *Efficiency:* Reduces server bandwidth and avoids IP blocking.
*   **Reactive UI:** Updates instantly based on socket events.

---

## 🚀 Key Features

*   **👥 Public Rooms:** Join active sessions from the lobby.
*   **💬 Real-time Chat:** Message others in the room.
*   **🎵 Live Queue:** Add songs to a shared playlist.
*   **⏱️ Sync:** Music starts and stops exactly at the same time for everyone.

---

## 🛠️ Tech Stack

### Mobile (Android)
*   **Language:** Kotlin
*   **UI:** Jetpack Compose
*   **Playback:** Media3 (ExoPlayer)
*   **Networking:** Retrofit, Ktor, Socket.IO Client
*   **Extraction:** NewPipeExtractor (InnerTube)

### Backend
*   **Runtime:** Node.js
*   **Framework:** Express
*   **Real-time:** Socket.IO
*   **Metadata:** YouTube-Music-API

---

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.