package com.iptvplayer.di

import android.content.Context
import androidx.room.Room
import com.iptvplayer.data.cache.ChannelDao
import com.iptvplayer.data.cache.IPTVDatabase
import com.iptvplayer.data.cache.PlaylistDao
import com.iptvplayer.data.cache.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IPTVDatabase {
        return Room.databaseBuilder(
            context,
            IPTVDatabase::class.java,
            "iptv_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideChannelDao(db: IPTVDatabase): ChannelDao = db.channelDao()

    @Provides
    fun providePlaylistDao(db: IPTVDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideWatchHistoryDao(db: IPTVDatabase): WatchHistoryDao = db.watchHistoryDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }
}
