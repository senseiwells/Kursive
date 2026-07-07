package me.senseiwells.kursive.server.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import me.senseiwells.kursive.common.Kursive
import net.fabricmc.loader.api.FabricLoader
import org.apache.commons.lang3.SerializationException
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class KursiveServerConfig(
    @SerialName("require_permissions")
    val requirePermissions: Boolean = true
) {
    companion object {
        private val json = Json {
            encodeDefaults = true
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
        }

        private fun path(): Path {
            return FabricLoader.getInstance().configDir.resolve("kursive-server.json")
        }

        @JvmStatic
        fun read(): KursiveServerConfig {
            val path = path()
            if (!path.exists()) {
                Kursive.logger.info("Generating default Kursive config")
                val config = KursiveServerConfig()
                this.write(config)
                return config
            }
            try {
                return path.inputStream().use {
                    json.decodeFromStream(it)
                }
            } catch (e: Exception) {
                Kursive.logger.error("Failed to read Kursive config, generating default", e)
                val config = KursiveServerConfig()
                this.write(config)
                return config
            }
        }

        @JvmStatic
        fun write(config: KursiveServerConfig) {
            try {
                path().createParentDirectories().outputStream().use {
                    json.encodeToStream(config, it)
                }
            } catch (e: IOException) {
                Kursive.logger.error("Failed to write Kursive config", e)
            } catch (e: SerializationException) {
                Kursive.logger.error("Failed to serialize Kursive config", e)
            }
        }
    }
}
