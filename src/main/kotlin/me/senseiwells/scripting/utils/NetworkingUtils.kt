package me.senseiwells.scripting.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.senseiwells.scripting.EssentialScripting
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URI

object NetworkingUtils {
    fun <T: Any> fetch(url: String, consumer: (Reader) -> T): T? {
        try {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use(consumer)
            }
        } catch (e: Exception) {
            EssentialScripting.logger.error("Failed to fetch url $url")
        }
        return null
    }

    fun fetchAsString(url: String): String? {
        return this.fetch(url, Reader::readText)
    }

    fun fetchAsJsonObject(url: String): JsonObject? {
        return this.fetch(url, JsonParser::parseReader)?.asJsonObject
    }
}