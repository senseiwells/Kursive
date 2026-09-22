package me.senseiwells.kursive.server.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.senseiwells.config.JsonConfigFile
import me.senseiwells.kursive.common.Kursive
import net.fabricmc.loader.api.FabricLoader

@Serializable
data class KursiveServerConfig(
    @SerialName("require_permissions")
    val requirePermissions: Boolean = true
) {
    companion object {
        private val file = JsonConfigFile.create(
            FabricLoader.getInstance().configDir.resolve("kursive-server.json"),
            ::KursiveServerConfig,
            logger = Kursive.logger
        )

        @JvmStatic
        fun read(): KursiveServerConfig {
            return this.file.read()
        }

        @JvmStatic
        fun write(config: KursiveServerConfig) {
            this.file.write(config)
        }
    }
}
