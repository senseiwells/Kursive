package me.senseiwells.kursive.client.gui.screen

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB

abstract class OverlayScreen(title: Component): Screen(title) {
    protected abstract val parent: Screen

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        this.parent.extractRenderState(graphics, 0, 0, a)
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ARGB.black(120))

        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun onClose() {
        this.minecraft.gui.setScreen(this.parent)
    }

    override fun resize(width: Int, height: Int) {
        this.parent.resize(width, height)
        super.resize(width, height)
    }
}