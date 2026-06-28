package me.senseiwells.kursive.common.network.data

import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import me.senseiwells.kursive.common.script.diagnostics.FormattedDiagnostics
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import net.casual.arcade.utils.optional.optional
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import kotlin.jvm.optionals.getOrNull

data class RemoteScriptData(
    val name: String,
    val running: Boolean,
    val compiled: Boolean,
    val metadata: ScriptMetadata?,
    val diagnostics: FormattedDiagnostics?
) {
    companion object {
        val STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RemoteScriptData::name,
            ByteBufCodecs.BOOL, RemoteScriptData::running,
            ByteBufCodecs.BOOL, RemoteScriptData::compiled,
            ByteBufCodecs.optional(ScriptMetadata.STREAM_CODEC), RemoteScriptData::metadata.optional(),
            ByteBufCodecs.optional(FormattedDiagnostics.STREAM_CODEC), RemoteScriptData::diagnostics.optional()
        ) { name, running, compiled, metadata, diagnostics ->
            RemoteScriptData(name, running, compiled, metadata.getOrNull(), diagnostics.getOrNull())
        }

        fun from(instance: ScriptInstance<*>): RemoteScriptData {
            return RemoteScriptData(
                instance.definition.name,
                instance.isRunning(),
                instance.isCompiled(),
                instance.tryGetMetadata(),
                FormattedDiagnostics.from(instance.diagnostics)
            )
        }
    }
}