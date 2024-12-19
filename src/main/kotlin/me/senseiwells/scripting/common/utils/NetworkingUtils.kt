package me.senseiwells.scripting.common.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.senseiwells.scripting.common.EssentialScripting
import java.io.InputStream
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URI

object NetworkingUtils {
    inline fun <T: Any> fetchAsStream(url: String, consumer: (InputStream) -> T): T? {
        try {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.use(consumer)
            }
        } catch (e: Exception) {
            EssentialScripting.logger.error("Failed to fetch url $url")
        }
        return null
    }

    inline fun <T: Any> fetchAsReader(url: String, consumer: (Reader) -> T): T? {
        return fetchAsStream(url) { stream ->
            stream.bufferedReader().use(consumer)
        }
    }

    fun fetchAsJsonObject(url: String): JsonObject? {
        return fetchAsReader(url, JsonParser::parseReader)?.asJsonObject
    }

    fun fetchAsJsonArray(url: String): JsonArray? {
        return fetchAsReader(url, JsonParser::parseReader)?.asJsonArray
    }
}