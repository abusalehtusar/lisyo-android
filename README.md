# Lisyo - Synchronized Music Listening

Lisyo is a real-time synchronized music listening application for Android, allowing users to listen to music together in public or private rooms.

## Architecture

Lisyo uses a **Hybrid Client-Server Architecture** to ensure low-latency synchronization while leveraging the vast library of YouTube Music.

### 1. Railway Backend (The Conductor)
The backend is a lightweight Node.js/Express server responsible for **State & Synchronization**.
*   **Room Management:** Handles creating, joining, and listing rooms.
*   **Session Management:** Tracks connected users and hosts.
*   **Precise Synchronization:**
    *   **NTP Time Sync:** Calculates precise time offsets between client and server.
    *   **Playback State:** Broadcasts "Play", "Pause", and "Seek" events with server timestamps to ensure all clients play at the exact same moment.
*   **Shared State:** Manages the Song Queue and Chat messages.
*   **Search Proxy:** Bridges the client to YouTube Music's database for metadata.

### 2. Android Client (The Player)
The client is a native Android app built with Jetpack Compose.
*   **Audio Engine:** Uses **ExoPlayer** for high-quality playback.
*   **Stream Extraction:** Directly interacts with **YouTube's InnerTube API** to extract audio stream URLs on the device.
    *   *Benefit:* Reduces server bandwidth usage and avoids IP blocking on the server.
    *   *Benefit:* Audio streams directly from YouTube to the user's device.
*   **Reactive UI:** Updates instantly based on socket events (new songs, messages, seeking).

## Tech Stack
*   **Android:** Kotlin, Jetpack Compose, ExoPlayer, Retrofit, Socket.IO Client.
*   **Backend:** Node.js, Express, Socket.IO, YouTube-Music-API.

## Key Features
*   **Public Rooms:** Join active sessions from the lobby.
*   **Real-time Chat:** Message others in the room.
*   **Live Queue:** Add songs to a shared playlist.
*   **Sync:** Music starts and stops exactly at the same time for everyone.
