package me.senseiwells.kursive.client.gui.script.widget

import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.client.utils.setTooltip
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.network.chat.Component

class ScriptCompileButton(
    private val script: ScriptHandle
): SpriteIconButton.CenteredIcon(20, 20, NAME, 20, 16, 0, 0, WidgetSprites(COMPILE), { }, NAME, { NAME }, false) {
    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (this.script.isRunning()) {
            this.active = false
            this.setTooltip(Component.literal("Cannot recompile while running"))
        } else {
            this.active = true
            this.setTooltip(this.message)
        }

        super.extractContents(graphics, mouseX, mouseY, a)
    }

    override fun onPress(input: InputWithModifiers) {
        this.script.compile()
    }

    companion object {
        private val NAME = Component.literal("Compile Script")

        private val COMPILE = kursive("icon/compile")
    }
}