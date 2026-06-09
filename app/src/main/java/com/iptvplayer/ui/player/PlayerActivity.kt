package com.iptvplayer.ui.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.iptvplayer.data.model.AspectRatio
import com.iptvplayer.data.model.Channel
import com.iptvplayer.ui.theme.IPTVPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val channel = Channel(
            id = intent.getStringExtra("channel_id") ?: "",
            name = intent.getStringExtra("channel_name") ?: "চ্যানেল",
            url = intent.getStringExtra("channel_url") ?: "",
            logo = intent.getStringExtra("channel_logo") ?: ""
        )
        viewModel.playChannel(channel)

        setContent {
            IPTVPlayerTheme {
                PlayerScreen(viewModel = viewModel, onBack = { finish() })
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                viewModel.togglePlayPause(); viewModel.showControls(); true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> { viewModel.showControls(); true }
            KeyEvent.KEYCODE_BACK -> { finish(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@UnstableApi
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val showControls by viewModel.showControls.collectAsStateWithLifecycle()
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val aspectRatio by viewModel.aspectRatio.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { viewModel.toggleControls() }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                }
            },
            update = { pv ->
                pv.resizeMode = when (aspectRatio) {
                    AspectRatio.FIT_SCREEN -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatio.FILL_SCREEN -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatio.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering
        if (isBuffering && !isError) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("লোড হচ্ছে...", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // Error
        if (isError) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.WifiOff, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(errorMessage, color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.retryConnection() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("পুনরায় চেষ্টা করুন")
                        }
                    }
                }
            }
        }

        // Controls overlay
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            PlayerControls(
                channelName = currentChannel?.name ?: "",
                isPlaying = isPlaying,
                aspectRatioLabel = aspectRatio.label,
                onBack = onBack,
                onPlayPause = { viewModel.togglePlayPause() },
                onAspectRatio = { viewModel.cycleAspectRatio() },
                onRefresh = { viewModel.retryConnection() }
            )
        }
    }
}

@Composable
fun PlayerControls(
    channelName: String,
    isPlaying: Boolean,
    aspectRatioLabel: String,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onAspectRatio: () -> Unit,
    onRefresh: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Top bar
        Box(
            Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.8f), Color.Transparent)))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(channelName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                        Spacer(Modifier.width(4.dp))
                        Text("LIVE", color = Color.White.copy(0.8f), fontSize = 11.sp)
                    }
                }
            }
        }

        // Bottom controls
        Box(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))
                .padding(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlBtn(Icons.Default.AspectRatio, aspectRatioLabel, onAspectRatio)
                Box(
                    Modifier.size(60.dp).clip(CircleShape)
                        .background(Color(0xFF6C63FF))
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = Color.White, modifier = Modifier.size(32.dp)
                    )
                }
                ControlBtn(Icons.Default.Refresh, "রিলোড", onRefresh)
            }
        }
    }
}

@Composable
fun ControlBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(0.15f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}
