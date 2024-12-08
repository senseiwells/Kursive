package me.senseiwells.scripting.script.configuration

import me.senseiwells.scripting.script.annotation.Environment
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.impl.util.version.VersionParser
import java.io.Serializable
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.api.asErrorDiagnostics
import kotlin.script.experimental.util.PropertiesCollection

class EnvironmentWithVersion private constructor(
    val type: EnvType,
    private val rawVersion: String
): Serializable {
    @Transient
    val version: Version = VersionParser.parse(this.rawVersion, true)

    companion object {
        fun parse(environment: Environment): ResultWithDiagnostics<EnvironmentWithVersion> {
            val env = when (environment.env.lowercase()) {
                "client" -> EnvType.CLIENT
                "server" -> EnvType.SERVER
                else -> return ResultWithDiagnostics.Failure(
                    "Invalid environment '${environment.env}' provided".asErrorDiagnostics()
                )
            }
            return ResultWithDiagnostics.Success(EnvironmentWithVersion(env, environment.version))
        }
    }
}

val ScriptCompilationConfigurationKeys.environment by PropertiesCollection.key<EnvironmentWithVersion>()