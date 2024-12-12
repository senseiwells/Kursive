package me.senseiwells.essential_scripting.utils

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.Version
import kotlin.script.experimental.api.ScriptDiagnostic

object EnvironmentUtils {
    private val minecraft by lazy { FabricLoader.getInstance().getModContainer("minecraft").get() }

    fun getDiagnosticsForTarget(target: Version): List<ScriptDiagnostic> {
        val comparison = target.compareTo(minecraft.metadata.version)
        return when {
            comparison > 0 -> listOf("Script was made for a newer version of Minecraft".asWarningDiagnostics())
            comparison < 0 -> listOf("Script was made for an older version of Minecraft".asWarningDiagnostics())
            else -> emptyList()
        }
    }
}