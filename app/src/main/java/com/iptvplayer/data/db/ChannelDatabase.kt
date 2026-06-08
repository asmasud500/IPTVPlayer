package com.iptvplayer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iptvplayer.data.model.Channel

@Database(entities = [Channel::class], version = 1, exportSchema = false)
abstract class ChannelDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
}
