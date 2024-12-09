package me.senseiwells.scripting

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import me.senseiwells.keybinds.api.KeybindListener
import me.senseiwells.scripting.script.execution.EnvironmentContext
import me.senseiwells.scripting.script.execution.ScriptExecutor
import me.senseiwells.scripting.utils.ScriptRemappingUtils
import net.fabricmc.api.ModInitializer
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.io.path.exists
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
        EssentialScriptingConfig.scriptStartKeybind.addListener(KeybindListener.onPress {
            val script = EssentialScriptingConfig.resolve("scripts").resolve("test.kts")
            if (script.exists()) {
                ScriptExecutor.runScript(EnvironmentContext(Minecraft.getInstance()), script.toFile().toScriptSource())
            }
        })
        EssentialScriptingConfig.scriptStopKeybind.addListener(KeybindListener.onPress {
            ScriptExecutor.cancelAllScripts()
        })
    }

    private suspend fun main(client: Minecraft) = coroutineScope {
        val deferred = async {
            println("Starting async task")
            delay(3000)
            println("Finished async task")
        }
        println("Waiting for async task")
        deferred.await()
        println("Finished waiting for async task")
    }
}