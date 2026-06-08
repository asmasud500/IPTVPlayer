package com.iptvplayer.ui.home

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.iptvplayer.data.model.Channel
import com.iptvplayer.data.model.EventCategory
import com.iptvplayer.data.model.LiveEvent
import com.iptvplayer.data.model.UiState
import com.iptvplayer.ui.player.PlayerActivity

// ─────────────────────────────────────────────
// Home Screen Root
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val suggestedChannels by viewModel.suggestedChannels.collectAsStateWithLifecycle()
    val playlistState by viewModel.playlistState.collectAsStateWithLifecycle()

    var showAddPlaylist by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    // Snackbar for playlist load result
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(playlistState) {
        when (val state = playlistState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("✅ ${state.data}টি চ্যানেল লোড হয়েছে!")
                viewModel.clearPlaylistState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar("❌ ${state.message}")
                viewModel.clearPlaylistState()
            }
            else -> {}
        }
    }

    fun openPlayer(channel: Channel) {
        viewModel.onChannelSelected(channel)
        context.startActivity(
            Intent(context, PlayerActivity::class.java).apply {
                putExtra("channel_id", channel.id)
                putExtra("channel_name", channel.name)
                putExtra("channel_url", channel.url)
                putExtra("channel_logo", channel.logo)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeTopBar(
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onSearchToggle = { isSearchActive = it },
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                onAddPlaylist = { showAddPlaylist = true }
            )
        },
        containerColor = Color(0xFF0D0D1A)
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Featured Live Events ──
            if (!isSearchActive && searchQuery.isEmpty()) {
                item {
                    FeaturedEventsSection(
                        events = viewModel.featuredEvents,
                        onEventClick = { /* API চ্যানেলে লিঙ্ক করুন */ }
                    )
                }

                // ── Suggested Channels ──
                if (suggestedChannels.isNotEmpty()) {
                    item {
                        HorizontalChannelRow(
                            title = "⭐ আপনার জন্য সাজেস্টেড",
                            channels = suggestedChannels,
                            onChannelClick = { openPlayer(it) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) }
                        )
                    }
                }
            }

            // ── Category Tabs ──
            item {
                CategoryTabs(
                    groups = groups,
                    selectedGroup = selectedGroup,
                    onGroupSelect = { viewModel.onGroupSelected(it) }
                )
            }

            // ── Channel Grid ──
            if (channels.isEmpty()) {
                item {
                    EmptyState(
                        message = if (searchQuery.isNotEmpty())
                            "\"$searchQuery\" এর জন্য কোনো ফলাফল নেই"
                        else "কোনো চ্যানেল নেই। প্লেলিস্ট যোগ করুন।",
                        onAddPlaylist = { showAddPlaylist = true }
                    )
                }
            } else {
                items(channels.chunked(2)) { rowChannels ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowChannels.forEach { channel ->
                            ChannelCard(
                                channel = channel,
                                onClick = { openPlayer(channel) },
                                onFavoriteToggle = { viewModel.toggleFavorite(channel) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // odd সংখ্যার জন্য খালি জায়গা
                        if (rowChannels.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // ── Add Playlist Dialog ──
    if (showAddPlaylist) {
        AddPlaylistDialog(
            isLoading = playlistState is UiState.Loading,
            onDismiss = { showAddPlaylist = false },
            onAdd = { name, url ->
                viewModel.loadPlaylist(name, url)
                showAddPlaylist = false
            }
        )
    }
}

// ─────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchToggle: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onAddPlaylist: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("চ্যানেল খুঁজুন...", color = Color.White.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6C63FF)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF6C63FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📺", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "IPTV Player",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { onSearchToggle(!isSearchActive) }) {
                Icon(
                    if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "সার্চ",
                    tint = Color.White
                )
            }
            IconButton(onClick = onAddPlaylist) {
                Icon(Icons.Default.Add, contentDescription = "প্লেলিস্ট যোগ", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF13131F)
        )
    )
}

// ─────────────────────────────────────────────
// Featured Events Banner
// ─────────────────────────────────────────────
@Composable
fun FeaturedEventsSection(
    events: List<LiveEvent>,
    onEventClick: (LiveEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "🔴 এখন লাইভ",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events) { event ->
                FeaturedEventCard(event = event, onClick = { onEventClick(event) })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun FeaturedEventCard(event: LiveEvent, onClick: () -> Unit) {
    val categoryColor = when (event.category) {
        EventCategory.CRICKET -> Color(0xFF4CAF50)
        EventCategory.FOOTBALL -> Color(0xFF2196F3)
        EventCategory.NEWS -> Color(0xFFFF5722)
        EventCategory.ENTERTAINMENT -> Color(0xFF9C27B0)
        EventCategory.OTHER -> Color(0xFF607D8B)
    }

    Card(
        modifier = Modifier
            .width(260.dp)
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.3f),
                                Color(0xFF1A1A2E)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (event.isLive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Red)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("● LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(event.category.emoji, fontSize = 24.sp)
                }

                Column {
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = event.subtitle,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = event.viewerCount,
                            color = categoryColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Horizontal Channel Row (Suggested)
// ─────────────────────────────────────────────
@Composable
fun HorizontalChannelRow(
    title: String,
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(channels) { channel ->
                SmallChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SmallChannelCard(channel: Channel, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2E)),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo.isNotEmpty()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("📺", fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = channel.name,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────
// Category Tabs
// ─────────────────────────────────────────────
@Composable
fun CategoryTabs(
    groups: List<String>,
    selectedGroup: String,
    onGroupSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(groups) { group ->
            val isSelected = group == selectedGroup
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) Color(0xFF6C63FF)
                        else Color(0xFF1E1E2E)
                    )
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onGroupSelect(group) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Channel Card (Grid Item)
// ─────────────────────────────────────────────
@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = if (isFocused) BorderStroke(2.dp, Color(0xFF6C63FF)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // লোগো
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF252540)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo.isNotEmpty()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        modifier = Modifier.size(42.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("📺", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = channel.group,
                    color = Color(0xFF6C63FF),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "পছন্দ",
                    tint = if (channel.isFavorite) Color(0xFFFF4081) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────
@Composable
fun EmptyState(message: String, onAddPlaylist: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📡", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddPlaylist,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("M3U প্লেলিস্ট যোগ করুন")
        }
    }
}

// ─────────────────────────────────────────────
// Add Playlist Dialog
// ─────────────────────────────────────────────
@Composable
fun AddPlaylistDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = Color(0xFF1E1E2E),
        title = {
            Text(
                "M3U প্লেলিস্ট যোগ করুন",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("প্লেলিস্টের নাম", color = Color.White.copy(alpha = 0.7f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("M3U URL", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("http://example.com/list.m3u", color = Color.White.copy(alpha = 0.3f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF6C63FF),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text("চ্যানেল লোড হচ্ছে...", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) onAdd(name.trim(), url.trim())
                },
                enabled = !isLoading && name.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("যোগ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isLoading) onDismiss() }) {
                Text("বাতিল", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
