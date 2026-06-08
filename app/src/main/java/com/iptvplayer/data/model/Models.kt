package com.iptvplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────
// Channel Model
// ─────────────────────────────────────────────
@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val logo: String = "",
    val group: String = "Other",
    val language: String = "",
    val country: String = "",
    val isFavorite: Boolean = false,
    val watchCount: Int = 0,
    val lastWatched: Long = 0L
)

// ─────────────────────────────────────────────
// Playlist Model
// ─────────────────────────────────────────────
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val isActive: Boolean = false,
    val channelCount: Int = 0,
    val lastUpdated: Long = 0L
)

// ─────────────────────────────────────────────
// Live Event Model (Featured Section)
// ─────────────────────────────────────────────
data class LiveEvent(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: EventCategory,
    val thumbnailUrl: String = "",
    val isLive: Boolean = true,
    val viewerCount: String = "",
    val channelId: String = ""
)

enum class EventCategory(val label: String, val emoji: String) {
    CRICKET("ক্রিকেট", "🏏"),
    FOOTBALL("ফুটবল", "⚽"),
    NEWS("সংবাদ", "📺"),
    ENTERTAINMENT("বিনোদন", "🎬"),
    OTHER("অন্যান্য", "📡")
}

// ─────────────────────────────────────────────
// Watch History Model
// ─────────────────────────────────────────────
@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val channelLogo: String,
    val watchedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L
)

// ─────────────────────────────────────────────
// UI State wrappers
// ─────────────────────────────────────────────
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// Player aspect ratio options
enum class AspectRatio(val label: String) {
    FIT_SCREEN("স্বাভাবিক"),
    FILL_SCREEN("পূর্ণ স্ক্রিন"),
    ZOOM("জুম"),
    RATIO_4_3("4:3"),
    RATIO_16_9("16:9")
}
