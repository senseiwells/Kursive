package me.senseiwells.kursive.client.gui.scripts.widget

import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.client.utils.setTooltip
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component

class ScriptToggleButton(
    private val script: ScriptHandle
): SpriteIconButton.CenteredIcon(20, 20, NAME, 14, 14, 0, 0, WidgetSprites(START), { }, NAME, { NAME }, false) {
    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        this.message = if (this.script.isRunning()) Component.literal("Stop Script") else Component.literal("Start Script")
        this.setTooltip(this.message)
        super.extractContents(graphics, mouseX, mouseY, a)
    }

    override fun extractSprite(graphics: GuiGraphicsExtractor, x: Int, y: Int) {
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            if (this.script.isRunning()) STOP else START,
            x, y, this.spriteWidth, this.spriteHeight, this.alpha
        )
    }

    override fun onPress(input: InputWithModifiers) {
        this.script.toggle()
    }

    companion object {
        private val NAME = Component.literal("Toggle Script")

        private val START = kursive("icon/start")
        private val STOP = kursive("icon/stop")
    }
}