package com.iptvplayer.data.repository

import com.iptvplayer.data.cache.ChannelDao
import com.iptvplayer.data.cache.PlaylistDao
import com.iptvplayer.data.cache.WatchHistoryDao
import com.iptvplayer.data.model.Channel
import com.iptvplayer.data.model.EventCategory
import com.iptvplayer.data.model.LiveEvent
import com.iptvplayer.data.model.Playlist
import com.iptvplayer.data.model.WatchHistory
import com.iptvplayer.utils.M3UParser
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val channelDao: ChannelDao,
    private val playlistDao: PlaylistDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val m3uParser: M3UParser
) {
    fun getAllChannels(): Flow<List<Channel>> = channelDao.getAllChannels()
    fun getFavorites(): Flow<List<Channel>> = channelDao.getFavoriteChannels()
    fun searchChannels(query: String): Flow<List<Channel>> = channelDao.searchChannels(query)
    // BUG FIX #7: getMostWatched() এখন fixed — no default param
    fun getMostWatched(): Flow<List<Channel>> = channelDao.getMostWatched()
    fun getChannelsByGroup(group: String): Flow<List<Channel>> = channelDao.getChannelsByGroup(group)
    fun getAllGroups(): Flow<List<String>> = channelDao.getAllGroups()

    suspend fun loadPlaylistFromUrl(name: String, url: String): Result<Int> {
        return try {
            val channels = m3uParser.parseFromUrl(url)
            if (channels.isEmpty()) return Result.failure(Exception("কোনো চ্যানেল পাওয়া যায়নি"))

            val playlist = Playlist(
                id = UUID.randomUUID().toString(),
                name = name,
                url = url,
                isActive = true,
                channelCount = channels.size,
                lastUpdated = System.currentTimeMillis()
            )

            playlistDao.deactivateAll()
            playlistDao.upsertPlaylist(playlist)
            channelDao.deleteAll()
            channelDao.upsertChannels(channels)

            Result.success(channels.size)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "অজানা ত্রুটি"))
        }
    }

    suspend fun toggleFavorite(channel: Channel) {
        channelDao.updateFavorite(channel.id, !channel.isFavorite)
    }

    // BUG FIX #8: incrementWatchCount এ time explicit পাঠানো হচ্ছে
    suspend fun recordWatch(channel: Channel) {
        channelDao.incrementWatchCount(channel.id, System.currentTimeMillis())
        watchHistoryDao.upsertHistory(
            WatchHistory(
                channelId = channel.id,
                channelName = channel.name,
                channelLogo = channel.logo
            )
        )
    }

    fun getWatchHistory(): Flow<List<WatchHistory>> = watchHistoryDao.getRecentHistory()

    fun getFeaturedEvents(): List<LiveEvent> = listOf(
        LiveEvent(
            id = "1",
            title = "🏏 বাংলাদেশ vs ভারত",
            subtitle = "T20 বিশ্বকাপ • LIVE",
            category = EventCategory.CRICKET,
            isLive = true,
            viewerCount = "১২ লক্ষ দর্শক"
        ),
        LiveEvent(
            id = "2",
            title = "⚽ ম্যানচেস্টার সিটি vs আর্সেনাল",
            subtitle = "প্রিমিয়ার লিগ • LIVE",
            category = EventCategory.FOOTBALL,
            isLive = true,
            viewerCount = "৮ লক্ষ দর্শক"
        ),
        LiveEvent(
            id = "3",
            title = "📺 ব্রেকিং নিউজ",
            subtitle = "আজকের সর্বশেষ সংবাদ",
            category = EventCategory.NEWS,
            isLive = true,
            viewerCount = "৫ লক্ষ দর্শক"
        ),
        LiveEvent(
            id = "4",
            title = "🎬 বিশেষ চলচ্চিত্র",
            subtitle = "সন্ধ্যার বিশেষ আয়োজন",
            category = EventCategory.ENTERTAINMENT,
            isLive = false,
            viewerCount = "৩ লক্ষ দর্শক"
        )
    )

    // BUG FIX #9: getMostWatched(10) এর পরিবর্তে getMostWatchedLimit(10) ব্যবহার
    fun getSuggestedChannels(): Flow<List<Channel>> = channelDao.getMostWatchedLimit(10)
}
