package me.senseiwells.kursive.client.gui.script.widget

import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.client.utils.setTooltip
import me.senseiwells.kursive.common.script.diagnostics.FormattedDiagnostics
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component

class ScriptDiagnosticIcon(
    private val script: ScriptHandle
): AbstractWidget(0, 0, 12, 12, Component.literal("Script Diagnostics")) {
    init {
        this.active = false
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        val diagnostics = this.script.getDiagnostics()
        if (diagnostics == null) {
            val icon = if (this.script.isRunning()) {
                this.setTooltip(Component.literal("Script Running"))
                RUNNING
            } else if (this.script.isCompiled()) {
                this.setTooltip(Component.literal("Script Compiled"))
                COMPILED
            } else {
                this.setTooltip(Component.literal("Script Needs Compiling"))
                UNCOMPILED
            }
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, icon,
                this.x - this.width / 2 + 2,
                this.y - this.height / 2,
                this.width * 2,
                this.height * 2,
                this.alpha
            )
            return
        }

        val icon = if (diagnostics.severity == FormattedDiagnostics.Severity.Warning) WARNING else ERROR
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, icon, this.x, this.y, this.width, this.height, this.alpha
        )

        this.setTooltip(diagnostics.component)
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {

    }

    companion object {
        private val WARNING = kursive("icon/warning")
        private val ERROR = kursive("icon/error")

        private val RUNNING = kursive("icon/running")
        private val COMPILED = kursive("icon/compiled")
        private val UNCOMPILED = kursive("icon/uncompiled")
    }
}