package me.senseiwells.scripting

import me.senseiwells.keybinds.api.KeybindListener
import me.senseiwells.scripting.script.FileScriptInstance
import me.senseiwells.scripting.script.execution.EnvironmentContext
import me.senseiwells.scripting.utils.ScriptRemappingUtils
import net.fabricmc.api.ModInitializer
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic

object EssentialScripting: ModInitializer {
    const val MOD_ID = "essential-scripting"

    val logger: Logger = LoggerFactory.getLogger("EssentialScripting")

    private val script = FileScriptInstance<Minecraft>(
        EssentialScriptingConfig.resolve("scripts").resolve("test.kts")
    )

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
            if (this.script.shouldRecompile()) {
                this.logReports(this.script.compile())
            }
            this.logReports(this.script.execute(EnvironmentContext(Minecraft.getInstance())))
        })
        EssentialScriptingConfig.scriptStopKeybind.addListener(KeybindListener.onPress {
            this.script.cancel()
        })
    }

    private fun logReports(result: ResultWithDiagnostics<*>) {
        for (report in result.reports) {
            when (report.severity) {
                ScriptDiagnostic.Severity.WARNING -> logger.warn(report.render(withSeverity = false))
                ScriptDiagnostic.Severity.ERROR -> logger.error(report.render(withSeverity = false))
                else -> { }
            }
        }
    }
}