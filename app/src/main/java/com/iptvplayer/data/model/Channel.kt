package com.iptvplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @SerializedName("channel_name")
    val name: String,
    @SerializedName("channel_url")
    val url: String,
    @SerializedName("channel_logo")
    val logo: String? = null,
    @SerializedName("channel_category")
    val category: String? = null,
    val playlistId: Long = 0,
    val isFavorite: Boolean = false,
    val lastPlayedTime: Long = 0L
)
