package me.senseiwells.kursive.common.utils

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.metadata.version.VersionPredicate
import kotlin.script.experimental.api.ScriptDiagnostic

object EnvironmentUtils {
    private val minecraft by lazy { FabricLoader.getInstance().getModContainer("minecraft").get() }

    fun getCurrentMinecraftVersion(): Version {
        return this.minecraft.metadata.version
    }

    fun getDiagnosticsForTarget(target: VersionPredicate): List<ScriptDiagnostic> {
        if (!target.test(this.getCurrentMinecraftVersion())) {
            return listOf("Script was made for a different version of Minecraft".asWarningDiagnostics())
        }
        return listOf()
    }
}