package me.senseiwells.kursive.client.gui

import me.senseiwells.kursive.client.gui.widget.ClientScriptsList
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ClientScriptsScreen(
    private val parent: Screen? = null
): Screen(Component.literal("Kursive Client Scripts")) {
    private val layout = HeaderAndFooterLayout(this)
    private lateinit var list: ClientScriptsList

    override fun init() {
        this.layout.addTitleHeader(this.title, this.font)
        this.list = this.layout.addToContents(
            ClientScriptsList(this.minecraft, this.width, this.layout.contentHeight, this.layout.headerHeight, 20)
        )

        this.layout.visitWidgets(this::addRenderableWidget)
        this.repositionElements()
    }

    override fun repositionElements() {
        this.layout.arrangeElements()
        this.list.updateSize(this.width, this.layout)
    }

    override fun onClose() {
        this.minecraft.gui.setScreen(this.parent)
    }
}