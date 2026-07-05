package me.senseiwells.kursive.common.script.configuration

import me.senseiwells.kursive.annotation.KursiveScript
import me.senseiwells.kursive.api.utils.ScriptType
import me.senseiwells.kursive.common.utils.EnvironmentUtils
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.metadata.version.VersionPredicate
import net.fabricmc.loader.impl.util.version.VersionParser
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.io.Serializable
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.util.PropertiesCollection

class ScriptMetadata private constructor(
    val id: String,
    val type: ScriptType,
    val auto: Boolean,
    private val rawVersion: String,
    private val rawMinecraftVersion: String
): Serializable {
    @Transient
    val version: Version = VersionParser.parse(this.rawVersion, true)

    @Transient
    val minecraftVersion: VersionPredicate = VersionPredicate.parse(this.rawMinecraftVersion)

    override fun toString(): String {
        return "ScriptMetadata(id=${this.id}, version=${this.version})"
    }

    private fun readResolve(): Any {
        return ScriptMetadata(this.id, this.type, this.auto, this.rawVersion, this.rawMinecraftVersion)
    }

    companion object {
        private val SCRIPT_TYPE_STREAM_CODEC = ByteBufCodecs.idMapper({ ScriptType.entries[it] }, ScriptType::ordinal)

        val STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ScriptMetadata::id,
            SCRIPT_TYPE_STREAM_CODEC, ScriptMetadata::type,
            ByteBufCodecs.BOOL, ScriptMetadata::auto,
            ByteBufCodecs.STRING_UTF8, ScriptMetadata::rawVersion,
            ByteBufCodecs.STRING_UTF8, ScriptMetadata::rawMinecraftVersion,
            ::ScriptMetadata
        )

        fun parse(script: KursiveScript): ResultWithDiagnostics<ScriptMetadata> {
            val type = when (script.type.lowercase()) {
                "client" -> ScriptType.Client
                "server" -> ScriptType.Server
                "common" -> ScriptType.Common
                else -> return makeFailureResult("Invalid script type: ${script.type}")
            }

            return ScriptMetadata(script.id, type, script.auto, script.version, script.minecraft).asSuccess()
        }

        fun named(id: String, type: ScriptType = ScriptType.Common): ScriptMetadata {
            return ScriptMetadata(id, type, false, "1.0.0", EnvironmentUtils.getCurrentMinecraftVersion().friendlyString)
        }
    }
}

val ScriptCompilationConfigurationKeys.scriptMetadata by PropertiesCollection.key<ScriptMetadata>()