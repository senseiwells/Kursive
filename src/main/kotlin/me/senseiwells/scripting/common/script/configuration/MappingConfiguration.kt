package me.senseiwells.scripting.common.script.configuration

import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

enum class MappingType(val id: String) {
    Intermediary("intermediary"),
    Mojang("mojang"),
    Yarn("yarn");

    companion object {
        fun named(): List<MappingType> {
            return listOf(Mojang, Yarn)
        }

        fun parse(string: String): MappingType {
            return when (string.lowercase()) {
                "yarn" -> Yarn
                "mojang" -> Mojang
                else -> Intermediary
            }
        }
    }
}

val ScriptCompilationConfigurationKeys.mappings by PropertiesCollection.key(MappingType.Mojang)