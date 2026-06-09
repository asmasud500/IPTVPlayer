package com.iptvplayer.data.cache

import androidx.room.*
import com.iptvplayer.data.model.Channel
import com.iptvplayer.data.model.Playlist
import com.iptvplayer.data.model.WatchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE `group` = :group ORDER BY name ASC")
    fun getChannelsByGroup(group: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchChannels(query: String): Flow<List<Channel>>

    // BUG FIX #4: Room DAO-তে default parameter কাজ করে না
    // আলাদা query তৈরি করা হয়েছে
    @Query("SELECT * FROM channels ORDER BY watchCount DESC LIMIT 20")
    fun getMostWatched(): Flow<List<Channel>>

    @Query("SELECT * FROM channels ORDER BY watchCount DESC LIMIT :limit")
    fun getMostWatchedLimit(limit: Int): Flow<List<Channel>>

    @Query("SELECT DISTINCT `group` FROM channels ORDER BY `group` ASC")
    fun getAllGroups(): Flow<List<String>>

    @Upsert
    suspend fun upsertChannels(channels: List<Channel>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun updateFavorite(channelId: String, isFavorite: Boolean)

    // BUG FIX #5: Room DAO-তে default parameter নেই — time parameter mandatory
    @Query("UPDATE channels SET watchCount = watchCount + 1, lastWatched = :time WHERE id = :channelId")
    suspend fun incrementWatchCount(channelId: String, time: Long)

    @Query("DELETE FROM channels")
    suspend fun deleteAll()
}

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY lastUpdated DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlaylist(): Playlist?

    @Upsert
    suspend fun upsertPlaylist(playlist: Playlist)

    @Query("UPDATE playlists SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE playlists SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)
}

@Dao
interface WatchHistoryDao {

    // BUG FIX #6: Room DAO-তে default parameter নেই
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT 30")
    fun getRecentHistory(): Flow<List<WatchHistory>>

    @Upsert
    suspend fun upsertHistory(history: WatchHistory)

    @Query("DELETE FROM watch_history WHERE channelId = :channelId")
    suspend fun deleteHistory(channelId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}

@Database(
    entities = [Channel::class, Playlist::class, WatchHistory::class],
    version = 1,
    exportSchema = false
)
abstract class IPTVDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun watchHistoryDao(): WatchHistoryDao
}
