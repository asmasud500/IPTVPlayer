package com.iptvplayer.utils

import com.iptvplayer.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UParser @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── M3U URL থেকে চ্যানেল পার্স করো ──
    suspend fun parseFromUrl(url: String): List<Channel> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        val content = response.body?.string() ?: return@withContext emptyList()
        parseContent(content)
    }

    // ── M3U ফাইল কন্টেন্ট পার্স করো ──
    fun parseContent(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentMeta = ChannelMeta()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF:") -> {
                    currentMeta = parseExtInf(trimmed)
                }
                trimmed.startsWith("http://") ||
                trimmed.startsWith("https://") ||
                trimmed.startsWith("rtmp://") ||
                trimmed.startsWith("rtsp://") -> {
                    if (currentMeta.name.isNotEmpty()) {
                        channels.add(
                            Channel(
                                id = UUID.randomUUID().toString(),
                                name = currentMeta.name,
                                url = trimmed,
                                logo = currentMeta.logo,
                                group = currentMeta.group,
                                language = currentMeta.language,
                                country = currentMeta.country
                            )
                        )
                        currentMeta = ChannelMeta()
                    }
                }
            }
        }
        return channels
    }

    private fun parseExtInf(line: String): ChannelMeta {
        val name = line.substringAfterLast(",").trim()
        val logo = extractAttribute(line, "tvg-logo")
        val group = extractAttribute(line, "group-title").ifEmpty { "Other" }
        val language = extractAttribute(line, "tvg-language")
        val country = extractAttribute(line, "tvg-country")

        return ChannelMeta(
            name = name,
            logo = logo,
            group = group,
            language = language,
            country = country
        )
    }

    private fun extractAttribute(line: String, attr: String): String {
        val pattern = Regex("""$attr="([^"]*)" """.trimEnd())
        return pattern.find(line)?.groupValues?.get(1) ?: ""
    }

    private data class ChannelMeta(
        val name: String = "",
        val logo: String = "",
        val group: String = "Other",
        val language: String = "",
        val country: String = ""
    )
}
