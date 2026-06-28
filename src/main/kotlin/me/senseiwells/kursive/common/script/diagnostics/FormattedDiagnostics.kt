package me.senseiwells.kursive.common.script.diagnostics

import net.casual.arcade.utils.component.joinToComponent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import kotlin.script.experimental.api.ScriptDiagnostic

data class FormattedDiagnostics(
    val severity: Severity,
    val component: Component
) {
    enum class Severity {
        Warning, Error;

        companion object {
            val STREAM_CODEC = ByteBufCodecs.idMapper({ Severity.entries[it] }, Severity::ordinal)
        }
    }

    companion object {
        val STREAM_CODEC = StreamCodec.composite(
            Severity.STREAM_CODEC, FormattedDiagnostics::severity,
            ComponentSerialization.STREAM_CODEC, FormattedDiagnostics::component,
            ::FormattedDiagnostics
        )

        fun from(diagnostics: List<ScriptDiagnostic>): FormattedDiagnostics? {
            val severity = diagnostics.maxOfOrNull { diagnostic -> diagnostic.severity } ?: return null
            if (severity < ScriptDiagnostic.Severity.WARNING) {
                return null
            }

            val formatted = diagnostics.filter { diagnostic -> diagnostic.severity >= ScriptDiagnostic.Severity.WARNING }
                .joinToComponent(CommonComponents.NEW_LINE) { diagnostic ->
                    Component.literal(diagnostic.render(withSeverity = false)).withStyle(this.color(diagnostic.severity))
                }
            return FormattedDiagnostics(
                if (severity == ScriptDiagnostic.Severity.WARNING) Severity.Warning else Severity.Error, formatted
            )
        }

        private fun color(severity: ScriptDiagnostic.Severity): ChatFormatting {
            return when (severity) {
                ScriptDiagnostic.Severity.WARNING -> ChatFormatting.YELLOW
                ScriptDiagnostic.Severity.ERROR -> ChatFormatting.RED
                ScriptDiagnostic.Severity.FATAL -> ChatFormatting.DARK_RED
                else -> ChatFormatting.WHITE
            }
        }
    }
}