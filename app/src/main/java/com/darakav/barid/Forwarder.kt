package com.darakav.barid

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Talks to the paired server (Darakav's `/api/sms/ingest`) with the phone key; never logs message text. */
object Forwarder {
    const val BATCH = 5

    sealed interface Result {
        data object Ok : Result
        /** The key was removed on the server or never valid. */
        data object Rejected : Result
        /** The server refused this batch's content (e.g. a message too long). */
        data object Invalid : Result
        /** Try again later (offline, server busy, module off). */
        data class Retry(val reason: String) : Result
    }

    fun send(pairing: Pairing, items: List<Queued>): Result {
        val body = JSONObject().put(
            "messages",
            JSONArray().apply {
                items.forEach { put(JSONObject().put("from", it.from).put("text", it.text).put("receivedStamp", it.receivedStamp)) }
            },
        ).toString()
        return request(pairing, "POST", body)
    }

    fun check(pairing: Pairing): Result = request(pairing, "GET", null)

    private fun request(pairing: Pairing, method: String, body: String?): Result {
        val connection = try {
            URL(pairing.url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return Result.Retry(e.javaClass.simpleName)
        }
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Authorization", "Bearer " + pairing.key)
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Barid/" + BuildConfig.VERSION_NAME)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            when (val code = connection.responseCode) {
                in 200..299 -> Result.Ok
                401 -> Result.Rejected
                400, 413 -> Result.Invalid
                else -> Result.Retry("HTTP $code")
            }
        } catch (e: IOException) {
            Result.Retry(e.javaClass.simpleName)
        } finally {
            connection.disconnect()
        }
    }
}
