package com.tk.toolkit.tv.launcher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/** Current weather and today's forecast, shown in the clock header and the screensaver. */
data class Weather(
    val city: String,
    val temperature: Int,
    val code: Int,
    val isDay: Boolean,
    val feelsLike: Int,
    val min: Int,
    val max: Int,
    /** Highest chance of rain today, in percent. */
    val rainChance: Int,
    /** Wind speed, in km/h. */
    val wind: Int,
) {

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
 * Fetches the weather from Open-Meteo (free, no account or API key). The only thing sent is the
 * chosen city's fixed coordinates: nothing about the TV or its user.
 */
object WeatherSource {

    /** Blocking; call off the main thread. Throws on network or parsing errors. */
    fun fetch(city: WeatherCity): Weather {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${city.latitude}&longitude=${city.longitude}" +
            "&current=temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&forecast_days=1&timezone=auto"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val current = json.getJSONObject("current")
            val daily = json.optJSONObject("daily")
            fun today(key: String) = daily?.optJSONArray(key)?.optDouble(0)?.takeIf { !it.isNaN() }
            val temperature = current.getDouble("temperature_2m")
            return Weather(
                city = city.label,
                temperature = temperature.roundToInt(),
                code = current.getInt("weather_code"),
                isDay = current.optInt("is_day", 1) == 1,
                feelsLike = current.optDouble("apparent_temperature", temperature).roundToInt(),
                min = (today("temperature_2m_min") ?: temperature).roundToInt(),
                max = (today("temperature_2m_max") ?: temperature).roundToInt(),
                rainChance = (today("precipitation_probability_max") ?: 0.0).roundToInt(),
                wind = current.optDouble("wind_speed_10m", 0.0).roundToInt(),
            )
        } finally {
            connection.disconnect()
        }
    }
}
