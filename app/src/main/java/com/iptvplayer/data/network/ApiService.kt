package com.iptvplayer.data.network

import com.iptvplayer.data.model.Channel
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("channels")
    suspend fun getChannels(
        @Query("playlistId") playlistId: String
    ): List<Channel>

    @GET("channel")
    suspend fun getChannel(
        @Query("id") channelId: String
    ): Channel
}
