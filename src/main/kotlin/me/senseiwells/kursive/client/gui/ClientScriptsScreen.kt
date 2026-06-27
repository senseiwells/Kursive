package me.senseiwells.kursive.client.gui

import me.senseiwells.kursive.client.gui.widget.ClientScriptsList
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ClientScriptsScreen(
    private val parent: Screen? = null
): Screen(Component.literal("Kursive Client Scripts")) {
    private val layout = HeaderAndFooterLayout(this)
    private lateinit var list: ClientScriptsList

    override fun init() {
        this.layout.addToHeader(StringWidget(this.title, this.font)) { settings ->
            settings.alignHorizontallyLeft().paddingLeft(15)
        }
        this.layout.addToHeader(this.createNewScriptButton()) { settings ->
            settings.alignHorizontallyRight().paddingRight(10).paddingBottom(2)
        }

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

    private fun createNewScriptButton(): Button {
        return Button.builder(Component.literal("+")) {
            this.minecraft.gui.setScreen(CreateNewClientScriptScreen(this))
        }.size(20, 20).build()
    }
}