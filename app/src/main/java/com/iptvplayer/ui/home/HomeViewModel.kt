package com.iptvplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.data.model.Channel
import com.iptvplayer.data.model.LiveEvent
import com.iptvplayer.data.model.UiState
import com.iptvplayer.data.model.WatchHistory
import com.iptvplayer.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ChannelRepository
) : ViewModel() {

    // ── সার্চ কোয়েরি ──
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ── সিলেক্টেড গ্রুপ ──
    private val _selectedGroup = MutableStateFlow("সকল")
    val selectedGroup: StateFlow<String> = _selectedGroup.asStateFlow()

    // ── Playlist লোড স্টেট ──
    private val _playlistState = MutableStateFlow<UiState<Int>?>(null)
    val playlistState: StateFlow<UiState<Int>?> = _playlistState.asStateFlow()

    // ── চ্যানেল লিস্ট (ফিল্টার সহ) ──
    val channels: StateFlow<List<Channel>> = combine(
        _searchQuery,
        _selectedGroup
    ) { query, group ->
        Pair(query, group)
    }.flatMapLatest { (query, group) ->
        when {
            query.isNotEmpty() -> repository.searchChannels(query)
            group == "সকল" || group == "All" -> repository.getAllChannels()
            group == "❤️ পছন্দের" -> repository.getFavorites()
            else -> repository.getChannelsByGroup(group)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── গ্রুপ লিস্ট ──
    val groups: StateFlow<List<String>> = repository.getAllGroups()
        .map { groups ->
            listOf("সকল", "❤️ পছন্দের") + groups
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("সকল"))

    // ── Featured Events ──
    val featuredEvents: List<LiveEvent> = repository.getFeaturedEvents()

    // ── সাজেস্টেড চ্যানেল ──
    val suggestedChannels: StateFlow<List<Channel>> = repository.getSuggestedChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Watch History ──
    val watchHistory: StateFlow<List<WatchHistory>> = repository.getWatchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Actions ──
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onGroupSelected(group: String) {
        _selectedGroup.value = group
        _searchQuery.value = ""
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel)
        }
    }

    fun loadPlaylist(name: String, url: String) {
        viewModelScope.launch {
            _playlistState.value = UiState.Loading
            val result = repository.loadPlaylistFromUrl(name, url)
            _playlistState.value = result.fold(
                onSuccess = { count -> UiState.Success(count) },
                onFailure = { e -> UiState.Error(e.message ?: "অজানা ত্রুটি") }
            )
        }
    }

    fun clearPlaylistState() {
        _playlistState.value = null
    }

    fun onChannelSelected(channel: Channel) {
        viewModelScope.launch {
            repository.recordWatch(channel)
        }
    }
}
