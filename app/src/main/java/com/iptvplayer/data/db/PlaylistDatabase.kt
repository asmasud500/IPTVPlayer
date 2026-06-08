package com.iptvplayer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iptvplayer.data.model.Playlist

@Database(entities = [Playlist::class], version = 1, exportSchema = false)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
