package net.thunderbird.feature.antispam.update

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface FetchResult {
    data object NotModified : FetchResult
    data class Downloaded(val lines: List<String>, val etag: String?, val lastModified: String?) : FetchResult
    data class Failed(val reason: String) : FetchResult
}

/** Download condizionale (ETag / Last-Modified): se la lista non è cambiata il server risponde 304. */
class AntispamListDownloader(
    private val maxBytes: Long = 10L * 1024 * 1024,
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
) {
    suspend fun fetch(url: String, etag: String?, lastModified: String?): FetchResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val parsed = URL(url)
            if (parsed.protocol != "https") return@withContext FetchResult.Failed("Sono ammessi solo URL https")

            connection = (parsed.openConnection() as HttpURLConnection).apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                etag?.let { setRequestProperty("If-None-Match", it) }
                lastModified?.let { setRequestProperty("If-Modified-Since", it) }
            }

            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> FetchResult.NotModified
                HttpURLConnection.HTTP_OK -> FetchResult.Downloaded(
                    lines = readBody(connection),
                    etag = connection.getHeaderField("ETag"),
                    lastModified = connection.getHeaderField("Last-Modified"),
                )
                else -> FetchResult.Failed("Risposta HTTP $code")
            }
        } catch (e: IOException) {
            FetchResult.Failed(e.message ?: "Errore di rete")
        } catch (e: IllegalArgumentException) {
            FetchResult.Failed("URL non valido")
        } finally {
            connection?.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection): List<String> {
        val lines = ArrayList<String>()
        var total = 0L
        BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                total += line.length + 1
                if (total > maxBytes) throw IOException("Lista troppo grande")
                lines.add(line)
            }
        }
        return lines
    }
}
