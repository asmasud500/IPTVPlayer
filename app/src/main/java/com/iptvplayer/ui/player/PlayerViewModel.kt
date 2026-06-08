package com.iptvplayer.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.iptvplayer.data.model.AspectRatio
import com.iptvplayer.data.model.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Player State ──
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatio.FIT_SCREEN)
    val aspectRatio: StateFlow<AspectRatio> = _aspectRatio.asStateFlow()

    private var reconnectJob: Job? = null
    private var controlsHideJob: Job? = null
    private var reconnectAttempts = 0

    // ── ExoPlayer তৈরি করো (Low Latency Config) ──
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                // IPTV লাইভ স্ট্রিমের জন্য বাফার কমাও
                .setBufferDurationsMs(
                    5_000,   // minBufferMs: ৫ সেকেন্ড
                    15_000,  // maxBufferMs: ১৫ সেকেন্ড
                    1_500,   // bufferForPlaybackMs
                    3_000    // bufferForPlaybackAfterRebufferMs
                )
                .build()
        )
        .build()
        .also { player ->
            player.addListener(object : Player.Listener {

                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    _isBuffering.value = state == Player.STATE_BUFFERING
                    _isError.value = false

                    if (state == Player.STATE_ENDED) {
                        // লাইভ স্ট্রিম শেষ হলে পুনরায় চালু করো
                        scheduleReconnect()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _isError.value = true
                    _errorMessage.value = "সংযোগ ত্রুটি। পুনরায় চেষ্টা করা হচ্ছে..."
                    scheduleReconnect()
                }
            })
        }

    // ── চ্যানেল চালু করো ──
    fun playChannel(channel: Channel) {
        reconnectAttempts = 0
        reconnectJob?.cancel()
        _currentChannel.value = channel
        _isError.value = false
        startStream(channel.url)
        scheduleHideControls()
    }

    private fun startStream(url: String) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        val mediaSource = when {
            url.contains(".m3u8") || url.contains("hls") -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(url))
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(url))
            }
        }

        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()
    }

    // ── Auto Reconnect (৩ বার চেষ্টা) ──
    private fun scheduleReconnect() {
        if (reconnectAttempts >= 3) {
            _errorMessage.value = "সংযোগ ব্যর্থ। স্ক্রিন স্পর্শ করে পুনরায় চেষ্টা করুন।"
            return
        }
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            val delay = (reconnectAttempts + 1) * 3000L
            _errorMessage.value = "সংযোগ বিচ্ছিন্ন। ${delay / 1000}s পরে পুনরায় সংযোগ করা হবে..."
            delay(delay)
            reconnectAttempts++
            _currentChannel.value?.let { startStream(it.url) }
        }
    }

    // ── Controls দেখানো / লুকানো ──
    fun toggleControls() {
        _showControls.value = !_showControls.value
        if (_showControls.value) scheduleHideControls()
    }

    fun showControls() {
        _showControls.value = true
        scheduleHideControls()
    }

    private fun scheduleHideControls() {
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(4000)
            _showControls.value = false
        }
    }

    // ── Aspect Ratio সাইকেল ──
    fun cycleAspectRatio() {
        val ratios = AspectRatio.values()
        val next = (ratios.indexOf(_aspectRatio.value) + 1) % ratios.size
        _aspectRatio.value = ratios[next]
    }

    // ── Play / Pause ──
    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    // ── ম্যানুয়াল Reconnect ──
    fun retryConnection() {
        reconnectAttempts = 0
        _isError.value = false
        _currentChannel.value?.let { startStream(it.url) }
    }

    override fun onCleared() {
        reconnectJob?.cancel()
        controlsHideJob?.cancel()
        player.release()
        super.onCleared()
    }
}
