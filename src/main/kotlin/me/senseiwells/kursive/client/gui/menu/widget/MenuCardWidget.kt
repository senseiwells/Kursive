package me.senseiwells.kursive.client.gui.menu.widget

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

class MenuCardWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
    private val description: Component,
    private val sprite: Identifier,
    private val action: () -> Unit
): AbstractButton(x, y, width, height, message) {
    override fun extractContents(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        a: Float
    ) {
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, ARGB.black(0.25F))
        if (this.isHoveredOrFocused) {
            graphics.outline(this.x, this.y, this.width, this.height, ARGB.white(0.25F))
        }

        val iconSize = this.height - 8
        val iconX = this.x + 4
        val iconY = this.y + 4
        val iconAlpha = if (this.active) this.alpha else this.alpha * 0.5f
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, this.sprite, iconX, iconY, iconSize, iconSize, iconAlpha
        )

        val textLeft = iconX + iconSize + 6
        val textRight = this.x + this.width - 4
        val titleCenterY = this.y + this.height / 2 - 10
        val descCenterY = this.y + this.height / 2 + 2

        val titleOutput = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE)
        titleOutput.acceptScrollingWithDefaultCenter(this.message, textLeft, textRight, titleCenterY, titleCenterY + 10)

        val descOutput = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE)
        descOutput.acceptScrollingWithDefaultCenter(this.description, textLeft, textRight, descCenterY, descCenterY + 10)
    }

    override fun onPress(input: InputWithModifiers) {
        this.action.invoke()
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        this.defaultButtonNarrationText(output)
    }

    @Suppress("Unused")
    class Builder(private val message: Component, private val sprite: Identifier) {
        private var x: Int = 0
        private var y: Int = 0
        private var width: Int = 200
        private var height: Int = 50
        private var description: Component = Component.empty()
        private var action: () -> Unit = {}

        fun pos(x: Int, y: Int): Builder {
            this.x = x
            this.y = y
            return this
        }

        fun size(width: Int, height: Int): Builder {
            this.width = width
            this.height = height
            return this
        }

        fun bounds(x: Int, y: Int, width: Int, height: Int): Builder {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
            return this
        }

        fun description(description: Component): Builder {
            this.description = description
            return this
        }

        fun action(action: () -> Unit): Builder {
            this.action = action
            return this
        }

        fun build(): MenuCardWidget {
            return MenuCardWidget(
                this.x, this.y, this.width, this.height, this.message, this.description, this.sprite, this.action
            )
        }
    }

    companion object {
        fun builder(message: Component, sprite: Identifier): Builder {
            return Builder(message, sprite)
        }
    }
}