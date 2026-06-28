package me.senseiwells.kursive.common.script.configuration

import me.senseiwells.kursive.annotation.KursiveScript
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.impl.util.version.VersionParser
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.io.Serializable
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

class ScriptMetadata private constructor(
    val id: String,
    private val rawVersion: String
): Serializable {
    @Transient
    val version: Version = VersionParser.parse(this.rawVersion, true)

    override fun toString(): String {
        return "ScriptIdWithVersion(id=${this.id}, version=${this.version})"
    }

    private fun readResolve(): Any {
        return ScriptMetadata(this.id, this.rawVersion)
    }

    companion object {
        val STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ScriptMetadata::id,
            ByteBufCodecs.STRING_UTF8, ScriptMetadata::rawVersion,
            ::ScriptMetadata
        )

        fun parse(script: KursiveScript): ResultWithDiagnostics<ScriptMetadata> {
            return ResultWithDiagnostics.Success(ScriptMetadata(script.id, script.version))
        }

        fun named(id: String): ScriptMetadata {
            return ScriptMetadata(id, "1.0.0")
        }
    }
}

val ScriptCompilationConfigurationKeys.scriptMetadata by PropertiesCollection.key<ScriptMetadata>()