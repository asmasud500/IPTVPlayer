# 📺 IPTV Player — Android App

A professional, modern IPTV Player built with **Jetpack Compose**, **ExoPlayer (Media3)**, **Hilt**, and **Room**.

## ✨ Features
- 🎬 **ExoPlayer Media3** — Low latency HLS/M3U8 streaming
- 📋 **M3U/M3U8 Playlist** — Load any playlist via URL
- 🔴 **Featured Live Events** — Live sports & news highlights
- ⭐ **Auto-Suggest** — Personalized channel recommendations
- ❤️ **Favorites** — Save favorite channels
- 📺 **Android TV Ready** — D-pad remote navigation support
- 🔄 **Auto-Reconnect** — Reconnects on network drop
- 🌙 **Dark Theme** — Beautiful dark IPTV UI

## 🛠️ Tech Stack
| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material3 |
| Player | AndroidX Media3 ExoPlayer |
| DI | Hilt |
| Database | Room + Flow |
| Network | OkHttp + Retrofit |
| Images | Coil |

## 🚀 Getting Started
1. Clone this repo
2. Open in Android Studio
3. Build → Build APK(s)
4. Install on device
5. Add M3U playlist URL and enjoy!

## 📡 Test M3U URL
```
https://iptv-org.github.io/iptv/index.m3u
```

## 📁 Project Structure
```
app/src/main/java/com/iptvplayer/
├── IPTVApp.kt
├── ui/
│   ├── MainActivity.kt
│   ├── theme/Theme.kt
│   ├── home/HomeScreen.kt + HomeViewModel.kt
│   └── player/PlayerActivity.kt + PlayerViewModel.kt
├── data/
│   ├── model/Models.kt
│   ├── cache/IPTVDatabase.kt
│   ├── repository/ChannelRepository.kt
│   └── service/PlaybackService.kt
├── di/AppModule.kt
└── utils/M3UParser.kt
```
