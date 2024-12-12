package me.senseiwells.essential_scripting

import com.mojang.blaze3d.platform.InputConstants
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import me.senseiwells.essential_scripting.EssentialScripting.id
import me.senseiwells.keybinds.api.InputKeys
import me.senseiwells.keybinds.api.Keybind
import me.senseiwells.keybinds.api.KeybindManager
import me.senseiwells.keybinds.api.yacl.Keybinding
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen
import java.nio.file.Path
import kotlin.io.path.createDirectories

class EssentialScriptingConfig {
    @Keybinding(id = RUN_SCRIPT)
    @AutoGen(category = "keybinds")
    @SerialEntry var runScriptKeys: InputKeys = InputKeys.of(InputConstants.KEY_F8)

    companion object {
        private const val ESSENTIAL_CATEGORY = "key.categories.essential-client"
        private const val RUN_SCRIPT = "${EssentialScripting.MOD_ID}:run_script"

        private val directory: Path = FabricLoader.getInstance().configDir.resolve("essential-scripting")
        private val handler: ConfigClassHandler<EssentialScriptingConfig> = createHandler()

        @JvmStatic
        val instance: EssentialScriptingConfig
            get() = handler.instance()

        val scriptStartKeybind: Keybind
        val scriptStopKeybind: Keybind

        init {
            handler.load()
            scriptStartKeybind = register("script-start", instance.runScriptKeys)
            scriptStopKeybind = register("script-stop", InputKeys.of(InputConstants.KEY_F7))
        }

        fun screen(parent: Screen? = null): Screen {
            return handler.generateGui().generateScreen(parent)
        }

        fun resolve(path: String): Path {
            return directory.resolve(path)
        }

        internal fun load() {
            directory.createDirectories()
        }

        private fun register(id: String, keys: InputKeys): Keybind {
            val keybind = KeybindManager.register(id(id), keys)
            KeybindManager.addToControlsScreen(ESSENTIAL_CATEGORY, keybind)
            return keybind
        }

        private fun createHandler(): ConfigClassHandler<EssentialScriptingConfig> {
            return ConfigClassHandler.createBuilder(EssentialScriptingConfig::class.java)
                .id(id("config"))
                .serializer { config ->
                    GsonConfigSerializerBuilder.create(config)
                        .setPath(directory.resolve("config.json"))
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