package me.senseiwells.kursive.client.gui.widget

import me.senseiwells.kursive.common.utils.kursive
import net.casual.arcade.utils.component.joinToComponent
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import kotlin.script.experimental.api.ScriptDiagnostic

class ScriptDiagnosticIcon(
    private val diagnostics: () -> List<ScriptDiagnostic>
): AbstractWidget(0, 0, 12, 12, Component.literal("Script Diagnostics")) {
    init {
        this.active = false
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        val diagnostics = this.diagnostics.invoke()
        val severity = diagnostics.maxOfOrNull { diagnostic -> diagnostic.severity } ?: ScriptDiagnostic.Severity.INFO
        if (severity < ScriptDiagnostic.Severity.WARNING) {
            this.setTooltip(null)
            return
        }

        val icon = if (severity == ScriptDiagnostic.Severity.WARNING) WARNING else ERROR
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, icon, this.x, this.y, this.width, this.height, this.alpha
        )

        val warnings = diagnostics.filter { diagnostic -> diagnostic.severity >= ScriptDiagnostic.Severity.WARNING }
            .joinToComponent(CommonComponents.NEW_LINE) { diagnostic ->
                Component.literal(diagnostic.render(withSeverity = false)).withStyle(this.color(diagnostic.severity))
            }
        this.setTooltip(Tooltip.create(warnings))
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {

    }

    private fun color(severity: ScriptDiagnostic.Severity): ChatFormatting {
        return when (severity) {
            ScriptDiagnostic.Severity.WARNING -> ChatFormatting.YELLOW
            ScriptDiagnostic.Severity.ERROR -> ChatFormatting.RED
            ScriptDiagnostic.Severity.FATAL -> ChatFormatting.DARK_RED
            else -> ChatFormatting.WHITE
        }
    }

    companion object {
        private val WARNING = kursive("icon/warning")
        private val ERROR = kursive("icon/error")
    }
}