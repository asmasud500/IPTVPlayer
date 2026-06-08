package com.iptvplayer.utils

import com.iptvplayer.data.model.Channel
import timber.log.Timber

object M3UParser {
    fun parseM3U(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("#EXTINF:")) {
                try {
                    val channelName = extractChannelName(line)
                    val logoUrl = extractLogoUrl(line)
                    val groupTitle = extractGroupTitle(line)

                    if (i + 1 < lines.size) {
                        val url = lines[i + 1].trim()
                        if (url.isNotEmpty() && !url.startsWith("#")) {
                            channels.add(
                                Channel(
                                    name = channelName,
                                    url = url,
                                    logo = logoUrl,
                                    category = groupTitle
                                )
                            )
                            i++
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing M3U line")
                }
            }
            i++
        }

        return channels
    }

    private fun extractChannelName(line: String): String {
        val parts = line.split(",")
        return if (parts.size > 1) parts.last().trim() else "Unknown"
    }

    private fun extractLogoUrl(line: String): String? {
        val regex = "tvg-logo=\"([^\"]*)\"".toRegex()
        return regex.find(line)?.groupValues?.get(1)
    }

    private fun extractGroupTitle(line: String): String? {
        val regex = "group-title=\"([^\"]*)\"".toRegex()
        return regex.find(line)?.groupValues?.get(1)
    }
}
