package com.tk.toolkit.tv.launcher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/** Current weather shown in the clock header. */
data class Weather(val city: String, val temperature: Int, val code: Int, val isDay: Boolean) {

    /** Emoji for the WMO weather [code]. */
    val emoji: String
        get() = when (code) {
            0 -> if (isDay) "☀️" else "🌙"
            1, 2 -> if (isDay) "⛅" else "☁️"
            3 -> "☁️"
            45, 48 -> "🌫️"
            in 51..57 -> "🌦️"
            in 61..67, in 80..82 -> "🌧️"
            in 71..77, 85, 86 -> "🌨️"
            in 95..99 -> "⛈️"
            else -> "🌡️"
        }

    /** French description of the WMO weather [code]. */
    val description: String
        get() = when (code) {
            0 -> if (isDay) "Ensoleillé" else "Dégagé"
            1, 2 -> "Peu nuageux"
            3 -> "Couvert"
            45, 48 -> "Brouillard"
            in 51..57 -> "Bruine"
            in 61..67 -> "Pluie"
            in 71..77 -> "Neige"
            in 80..82 -> "Averses"
            85, 86 -> "Averses de neige"
            in 95..99 -> "Orage"
            else -> ""
        }
}

/**
 * Fetches the current weather from Open-Meteo (free, no account or API key). The only thing sent
 * is the fixed coordinates below: nothing about the TV or its user. Edit them for another city.
 */
object WeatherSource {
    const val CITY = "Paris"
    private const val LATITUDE = "48.8566"
    private const val LONGITUDE = "2.3522"

    private const val URL_TEMPLATE = "https://api.open-meteo.com/v1/forecast" +
        "?latitude=$LATITUDE&longitude=$LONGITUDE&current=temperature_2m,weather_code,is_day&timezone=auto"

    /** Blocking; call off the main thread. Throws on network or parsing errors. */
    fun fetch(): Weather {
        val connection = URL(URL_TEMPLATE).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            return Weather(
                city = CITY,
                temperature = current.getDouble("temperature_2m").roundToInt(),
                code = current.getInt("weather_code"),
                isDay = current.optInt("is_day", 1) == 1,
            )
        } finally {
            connection.disconnect()
        }
    }
}
