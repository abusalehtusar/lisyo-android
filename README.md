<div align="center">
<img src="Web/logo.svg" width="128" height="128" style="border-radius: 20%">

# Lisyo
### Synchronized Music Listening for Android

Lisyo is a real-time synchronized music listening application for Android, allowing users to listen to music together in public or private rooms.

<p align="center">
    <a href="https://developer.android.com/android"><img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Android/android2.svg"></a>
    <a href="https://kotlinlang.org/"><img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Kotlin/kotlin2.svg"></a>
    <a href="https://nodejs.org/"><img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/NodeJS/nodejs2.svg"></a>
    <a href="https://expressjs.com/"><img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Express/express2.svg"></a>
    <a href="https://socket.io/"><img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/SocketIO/socketio2.svg"></a>
</p>

[Features](#-key-features) • [Architecture](#-architecture) • [Tech Stack](#-tech-stack)

</div>

---

## 🏗️ Architecture

Lisyo uses a **Hybrid Client-Server Architecture** to ensure low-latency synchronization while leveraging the vast library of YouTube Music.

### 1. Backend (The Conductor)
<img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/NodeJS/nodejs2.svg" height="25"> <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Express/express2.svg" height="25"> <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/SocketIO/socketio2.svg" height="25">

The backend is a lightweight Node.js/Express server responsible for **State & Synchronization**.
*   **Room Management:** Handles creating, joining, and listing rooms.
*   **Session Management:** Tracks connected users and hosts.
*   **Precise Synchronization:** Calculates time offsets (NTP style) and broadcasts playback events with server timestamps.
*   **Shared State:** Manages the Song Queue and Chat messages.

### 2. Android Client (The Player)
<img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Android/android2.svg" height="25"> <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Kotlin/kotlin2.svg" height="25">

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
*   **Language:** Kotlin <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Kotlin/kotlin2.svg" height="20">
*   **UI:** Jetpack Compose
*   **Playback:** Media3 (ExoPlayer)
*   **Networking:** Retrofit, Ktor, Socket.IO Client
*   **Extraction:** NewPipeExtractor (InnerTube)

### Backend
*   **Runtime:** Node.js <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/NodeJS/nodejs2.svg" height="20">
*   **Framework:** Express <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Express/express2.svg" height="20">
*   **Real-time:** Socket.IO <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/SocketIO/socketio2.svg" height="20">
*   **Metadata:** YouTube-Music-API

---

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.