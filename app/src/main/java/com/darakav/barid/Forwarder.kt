package com.darakav.barid

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Talks to the paired server with its key; never logs message text. Protocol: docs/SERVERS.md. */
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

    /** Messages per request: a batch for Barid's JSON, one at a time for a template. */
    fun batchSize(pairing: Pairing) = if (pairing.template.isEmpty()) BATCH else 1

    fun send(pairing: Pairing, items: List<Queued>): Result {
        if (pairing.template.isEmpty()) return request(pairing, "POST", Payload.batch(items))
        for (item in items) {
            val result = request(pairing, "POST", Payload.fill(pairing.template, item))
            if (result != Result.Ok) return result
        }
        return Result.Ok
    }

    /**
     * Checks that the server accepts this phone: Darakav answers a GET, other servers get an empty batch,
     * and a template server gets one [sample] message (there is no empty form of a template).
     */
    fun check(pairing: Pairing, sample: Queued): Result = when {
        pairing.darakav -> request(pairing, "GET", null)
        pairing.template.isEmpty() -> request(pairing, "POST", Payload.batch(emptyList()))
        else -> request(pairing, "POST", Payload.fill(pairing.template, sample))
    }

    private fun request(pairing: Pairing, method: String, body: String?): Result {
        val connection = try {
            URL(pairing.url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return Result.Retry(e.javaClass.simpleName)
        } catch (e: IllegalArgumentException) {
            return Result.Retry(e.javaClass.simpleName)
        }
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            pairing.auth?.let { (name, value) -> connection.setRequestProperty(name, value) }
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Barid/" + BuildConfig.VERSION_NAME)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", Payload.contentType(pairing.template))
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            classify(connection.responseCode, pairing.darakav)
        } catch (e: IOException) {
            Result.Retry(e.javaClass.simpleName)
        } finally {
            connection.disconnect()
        }
    }

    /** Darakav answers 401 for a removed key; other servers may also use 403, and 422 for bad content. */
    fun classify(code: Int, darakav: Boolean): Result = when {
        code in 200..299 -> Result.Ok
        code == 401 || (!darakav && code == 403) -> Result.Rejected
        code == 400 || code == 413 || (!darakav && code == 422) -> Result.Invalid
        else -> Result.Retry("HTTP $code")
    }
}
