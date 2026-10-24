package com.tk.toolkit.tv.launcher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Whether a Kick channel is live, and the address of its stream (HLS), from the API that
 * kick.com's own pages use. It isn't documented and Kick may refuse it, hence [Unreachable].
 */
sealed interface KickLive {
    data class Live(val streamUrl: String) : KickLive
    data object Offline : KickLive
    data class Unreachable(val reason: String) : KickLive

    companion object {
        /** Blocking: call it off the main thread. */
        fun check(channel: String): KickLive {
            var failure: KickLive = Unreachable("pas de réponse")
            for (endpoint in ENDPOINTS) {
                val result = runCatching { fetch(endpoint + channel) }
                    .getOrElse { Unreachable(it.message ?: it.javaClass.simpleName) }
                if (result !is Unreachable) return result
                failure = result
            }
            return failure
        }

        private fun fetch(url: String): KickLive {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", BROWSER_AGENT)
            try {
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_NOT_FOUND) return Unreachable("chaîne inconnue")
                if (code != HttpURLConnection.HTTP_OK) return Unreachable("HTTP $code")
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val stream = json.optString("playback_url").takeIf { it.startsWith("http") }
                val live = !json.isNull("livestream")
                return if (live && stream != null) Live(stream) else Offline
            } finally {
                connection.disconnect()
            }
        }

        private val ENDPOINTS = listOf("https://kick.com/api/v2/channels/", "https://kick.com/api/v1/channels/")
        private const val TIMEOUT_MS = 4_000
        private const val BROWSER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"
    }
}
