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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

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

    // BUG FIX #12: ExoPlayer lazy তৈরি — ViewModel init এ নয়
    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        5_000,
                        15_000,
                        1_500,
                        3_000
                    )
                    .build()
            )
            .build()
            .also { exoPlayer ->
                exoPlayer.addListener(object : Player.Listener {

                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        _isBuffering.value = state == Player.STATE_BUFFERING
                        // BUG FIX #13: error reset শুধু READY state এ করো
                        if (state == Player.STATE_READY) {
                            _isError.value = false
                        }
                        if (state == Player.STATE_ENDED) {
                            scheduleReconnect()
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        _isBuffering.value = false
                        _isError.value = true
                        _errorMessage.value = "সংযোগ ত্রুটি। পুনরায় চেষ্টা করা হচ্ছে..."
                        scheduleReconnect()
                    }
                })
            }
    }

    fun playChannel(channel: Channel) {
        reconnectAttempts = 0
        reconnectJob?.cancel()
        _currentChannel.value = channel
        _isError.value = false
        _isBuffering.value = true
        startStream(channel.url)
        scheduleHideControls()
    }

    private fun startStream(url: String) {
        try {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(20_000)
                // BUG FIX #14: User-Agent যোগ করা হয়েছে — কিছু server ছাড়া block করে
                .setUserAgent("IPTVPlayer/1.0 (Android)")

            val mediaSource = when {
                url.contains(".m3u8", ignoreCase = true) ||
                url.contains("hls", ignoreCase = true) ||
                url.contains("m3u8", ignoreCase = true) -> {
                    HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(url))
                }
                else -> {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(url))
                }
            }

            player.stop()
            player.clearMediaItems()
            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            _isError.value = true
            _errorMessage.value = "স্ট্রিম লোড করা যায়নি: ${e.message}"
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= 3) {
            _errorMessage.value = "সংযোগ ব্যর্থ। রিফ্রেশ বাটন চাপুন।"
            return
        }
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            val delayMs = (reconnectAttempts + 1) * 3000L
            _errorMessage.value = "${delayMs / 1000} সেকেন্ড পরে পুনরায় সংযোগ..."
            delay(delayMs)
            reconnectAttempts++
            _currentChannel.value?.let { startStream(it.url) }
        }
    }

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

    fun cycleAspectRatio() {
        val ratios = AspectRatio.values()
        val next = (ratios.indexOf(_aspectRatio.value) + 1) % ratios.size
        _aspectRatio.value = ratios[next]
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun retryConnection() {
        reconnectAttempts = 0
        reconnectJob?.cancel()
        _isError.value = false
        _currentChannel.value?.let { startStream(it.url) }
    }

    override fun onCleared() {
        reconnectJob?.cancel()
        controlsHideJob?.cancel()
        // BUG FIX #15: lazy player — initialized হয়েছে কিনা চেক করে release
        if (::player.isInitialized) {
            player.stop()
            player.release()
        }
        super.onCleared()
    }
}
