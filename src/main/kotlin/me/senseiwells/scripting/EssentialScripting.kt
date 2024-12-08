package me.senseiwells.scripting

import me.senseiwells.scripting.script.execution.ScriptExecutor
import me.senseiwells.scripting.utils.ScriptRemappingUtils
import me.senseiwells.keybinds.api.KeybindListener
import me.senseiwells.scripting.script.execution.ClientContext
import net.fabricmc.api.ModInitializer
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.script.experimental.host.toScriptSource

object EssentialScripting: ModInitializer {
    const val MOD_ID = "essential-scripting"

    val logger: Logger = LoggerFactory.getLogger("EssentialScripting")

    override fun onInitialize() {
        ScriptRemappingUtils.load()
        EssentialScriptingConfig.load()

        this.loadKeybinds()
    }

    fun id(path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path.removePrefix("${MOD_ID}:"))
    }

    private fun loadKeybinds() {
        EssentialScriptingConfig.scriptKeybind.addListener(KeybindListener.onPress {
            val script = EssentialScriptingConfig.resolve("scripts").resolve("test.kts")
            if (script.exists()) {
                ScriptExecutor.runScript(ClientContext(Minecraft.getInstance()), script.toFile().toScriptSource())
            }
        })
    }
}