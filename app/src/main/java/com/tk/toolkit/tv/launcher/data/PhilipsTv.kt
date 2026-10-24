package com.tk.toolkit.tv.launcher.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Philips' local TV API ("JointSpace", https on port 1926 of the TV itself), used to drive the
 * Ambilight LEDs directly. Pairing shows a code on the TV once; the credentials it returns are
 * kept in [LauncherPrefs]. Protocol as implemented by the open-source ha-philipsjs library.
 * All calls are blocking: run them off the main thread.
 */
class PhilipsTv(private val context: Context, private val prefs: LauncherPrefs) {

    /** What [pairRequest] returns and [pairGrant] needs. */
    class PairState(val deviceId: String, val authKey: String, val timestamp: Long, val device: JSONObject)

    val isPaired: Boolean get() = prefs.philipsAuthKey != null

    private var host: String? = null
    private var challenge: Map<String, String>? = null
    private var nonceCount = 0

    /** Asks the TV for a pairing code, which it then shows on screen. */
    fun pairRequest(): PairState {
        val deviceId = prefs.philipsDeviceId ?: randomHex(16).also { prefs.philipsDeviceId = it }
        val device = JSONObject()
            .put("device_name", "Toolkit")
            .put("device_os", "Android")
            .put("type", "native")
            .put("id", deviceId)
            .put("app_id", "com.tk.toolkit.tv")
            .put("app_name", "Toolkit")
        val body = JSONObject()
            .put("access", JSONObject().put("scope", org.json.JSONArray(listOf("read", "write", "control"))))
            .put("device", device)
        val (code, response) = send("POST", "pair/request", body.toString(), credentials = null)
        val json = JSONObject(response.ifEmpty { "{}" })
        check(code == 200 && json.optString("error_id") == "SUCCESS") { "la TV a refusé ($code ${json.optString("error_id")})" }
        return PairState(deviceId, json.getString("auth_key"), json.getLong("timestamp"), device)
    }

    /** Completes pairing with the [pin] shown on the TV. */
    fun pairGrant(state: PairState, pin: String) {
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(SHARED_KEY, "HmacSHA1")) }
        val signature = Base64.encodeToString(mac.doFinal("${state.timestamp}$pin".toByteArray()), Base64.NO_WRAP)
        val auth = JSONObject()
            .put("auth_appId", "1")
            .put("auth_timestamp", state.timestamp)
            .put("auth_signature", signature)
            .put("pin", pin)
        val body = JSONObject().put("auth", auth).put("device", state.device)
        val (code, response) = send("POST", "pair/grant", body.toString(), state.deviceId to state.authKey)
        val json = JSONObject(response.ifEmpty { "{}" })
        check(code == 200 && json.optString("error_id") == "SUCCESS") { "code refusé ($code ${json.optString("error_id")})" }
        prefs.philipsDeviceId = state.deviceId
        prefs.philipsAuthKey = state.authKey
    }

    fun forget() {
        prefs.philipsAuthKey = null
    }

    /** LEDs per side, e.g. {"layers":1,"left":4,"top":9,"right":4,"bottom":0}. */
    fun topology(): JSONObject = JSONObject(get("ambilight/topology"))

    fun currentConfiguration(): String = get("ambilight/currentconfiguration")

    /** Hands the LEDs over to [setPixels] (or back to the TV with "internal"). */
    fun setMode(mode: String) {
        post("ambilight/mode", JSONObject().put("current", mode).toString())
    }

    fun setPixels(payload: String) {
        post("ambilight/cached", payload)
    }

    fun restoreConfiguration(configuration: String) {
        post("ambilight/currentconfiguration", configuration)
    }

    private fun get(path: String): String = authed("GET", path, null)

    private fun post(path: String, body: String): String = authed("POST", path, body)

    private fun authed(method: String, path: String, body: String?): String {
        val deviceId = prefs.philipsDeviceId
        val authKey = prefs.philipsAuthKey
        check(deviceId != null && authKey != null) { "Toolkit n'est pas associé à la TV" }
        val (code, response) = send(method, path, body, deviceId to authKey)
        check(code in 200..299) { "HTTP $code sur $path" }
        return response
    }

    /** One request, answering a Digest challenge if the TV sends one (and reusing it after). */
    private fun send(method: String, path: String, body: String?, credentials: Pair<String, String>?): Pair<Int, String> {
        var lastError: Exception? = null
        for (candidate in hosts()) {
            try {
                val result = sendTo(candidate, method, path, body, credentials)
                host = candidate
                return result
            } catch (e: java.io.IOException) {
                lastError = e
                challenge = null
            }
        }
        throw lastError ?: IllegalStateException("TV injoignable")
    }

    @SuppressLint("BadHostnameVerifier")
    private fun sendTo(host: String, method: String, path: String, body: String?, credentials: Pair<String, String>?): Pair<Int, String> {
        val uri = "/$API_VERSION/$path"
        repeat(2) {
            val connection = URL("https://$host:$PORT$uri").openConnection() as HttpsURLConnection
            connection.sslSocketFactory = trustingSocketFactory
            connection.setHostnameVerifier { _, _ -> true }
            // The TV can take a while, e.g. to check a pairing code (ha-philipsjs allows 20 s).
            connection.connectTimeout = 5_000
            connection.readTimeout = 20_000
            connection.requestMethod = method
            connection.setRequestProperty("Accept", "application/json")
            val known = challenge
            if (credentials != null && known != null) {
                connection.setRequestProperty("Authorization", digest(known, method, uri, credentials))
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            val header = connection.getHeaderField("WWW-Authenticate")
            val text = (if (code >= 400) connection.errorStream else connection.inputStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED && credentials != null && header != null && it == 0) {
                challenge = parseChallenge(header)
                nonceCount = 0
                return@repeat
            }
            return code to text
        }
        return HttpURLConnection.HTTP_UNAUTHORIZED to ""
    }

    private fun digest(challenge: Map<String, String>, method: String, uri: String, credentials: Pair<String, String>): String {
        val (user, password) = credentials
        val realm = challenge["realm"].orEmpty()
        val nonce = challenge["nonce"].orEmpty()
        val ha1 = md5("$user:$realm:$password")
        val ha2 = md5("$method:$uri")
        val qop = challenge["qop"]?.split(',')?.map { it.trim() }?.firstOrNull { it == "auth" }
        val parts = mutableListOf(
            "username=\"$user\"",
            "realm=\"$realm\"",
            "nonce=\"$nonce\"",
            "uri=\"$uri\"",
            "algorithm=MD5",
        )
        val response = if (qop != null) {
            nonceCount++
            val nc = "%08x".format(nonceCount)
            val cnonce = randomHex(8)
            parts += listOf("qop=$qop", "nc=$nc", "cnonce=\"$cnonce\"")
            md5("$ha1:$nonce:$nc:$cnonce:$qop:$ha2")
        } else {
            md5("$ha1:$nonce:$ha2")
        }
        parts += "response=\"$response\""
        challenge["opaque"]?.let { parts += "opaque=\"$it\"" }
        return "Digest " + parts.joinToString(", ")
    }

    private fun parseChallenge(header: String): Map<String, String> =
        Regex("""(\w+)=(?:"([^"]*)"|([^,\s]+))""").findAll(header.removePrefix("Digest").trim())
            .associate { match -> match.groupValues[1].lowercase() to match.groupValues[2].ifEmpty { match.groupValues[3] } }

    /** The TV itself first, then its address on the local network. */
    private fun hosts(): List<String> = listOfNotNull(host, "127.0.0.1", lanAddress()).distinct()

    private fun lanAddress(): String? = runCatching {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        connectivity.getLinkProperties(connectivity.activeNetwork)?.linkAddresses
            ?.map { it.address }?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
    }.getOrNull()

    companion object {
        private const val PORT = 1926
        private const val API_VERSION = 6

        /** Key shared by Philips' apps for pairing signatures (from ha-philipsjs). */
        private val SHARED_KEY = Base64.decode(
            "ZmVay1EQVFOaZhwQ4Kv81ypLAZNczV9sG4KkseXWn1NEk6cXmPKO/MCa9sryslvLCFMnNe4Z4CPXzToowvhHvA==",
            Base64.DEFAULT,
        )

        private fun md5(text: String) =
            MessageDigest.getInstance("MD5").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

        private fun randomHex(bytes: Int) =
            ByteArray(bytes).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

        /**
         * The TV's API uses a self-signed certificate. This trust-all factory is only ever used
         * for these requests, which go to the TV itself (loopback or its own LAN address).
         */
        private val trustingSocketFactory: SSLSocketFactory by lazy {
            SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(TrustAll()), SecureRandom()) }.socketFactory
        }
    }

    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private class TrustAll : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
