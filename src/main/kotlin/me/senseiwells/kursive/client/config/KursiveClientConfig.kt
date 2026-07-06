package me.senseiwells.kursive.client.config

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import me.senseiwells.keybinds.api.InputKeys
import me.senseiwells.keybinds.api.yacl.Keybinding
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.client.gui.screens.Screen
import dev.isxander.yacl3.config.v2.api.autogen.Boolean as Bool

class KursiveClientConfig {
    @Bool(colored = true)
    @AutoGen(category = GENERAL_CATEGORY)
    @SerialEntry var allowDownloadingServerScripts = false

    @Keybinding(id = KursiveKeybinds.MENU_ID)
    @AutoGen(category = KEYBIND_CATEGORY)
    @SerialEntry var menuKeys: InputKeys = InputKeys.EMPTY

    @Keybinding(id = KursiveKeybinds.RESTART_CLIENT_SCRIPTS_ID)
    @AutoGen(category = KEYBIND_CATEGORY)
    @SerialEntry var restartClientScriptsKeys: InputKeys = InputKeys.EMPTY

    @Keybinding(id = KursiveKeybinds.RESTART_SERVER_SCRIPTS_ID)
    @AutoGen(category = KEYBIND_CATEGORY)
    @SerialEntry var restartServerScriptsKeys: InputKeys = InputKeys.EMPTY

    @Bool(colored = true)
    @AutoGen(category = DEBUG_CATEGORY)
    @SerialEntry var treatIntegratedAsLocal: Boolean = true

    companion object {
        private const val GENERAL_CATEGORY = "general"
        private const val DEBUG_CATEGORY = "debug"
        private const val KEYBIND_CATEGORY = "keybinds"

        private val handler: ConfigClassHandler<KursiveClientConfig> = create()

        val instance: KursiveClientConfig
            get() = this.handler.instance()

        init {
            this.handler.load()
        }

        fun screen(parent: Screen? = null): Screen {
            return this.handler.generateGui().generateScreen(parent)
        }

        internal fun load() {

        }

        private fun create(): ConfigClassHandler<KursiveClientConfig> {
            return ConfigClassHandler.createBuilder(KursiveClientConfig::class.java)
                .id(kursive("config"))
                .serializer { config ->
                    GsonConfigSerializerBuilder.create(config)
                        .setPath(Kursive.config())
                        .appendGsonBuilder { obj ->
                            obj.setPrettyPrinting()
                            obj.registerTypeAdapter(InputKeys::class.java, InputKeys.Serializer.INSTANCE)
                        }
                        .build()
                }
                .build()
        }
    }
}