package me.senseiwells.kursive.client.gui

import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.gui.widget.ClientScriptsList
import me.senseiwells.kursive.client.gui.widget.ScaledStringWidget
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.Util

class ClientScriptsScreen(
    private val parent: Screen? = null
): Screen(Component.literal("Kursive Client Scripts")) {
    private val layout = HeaderAndFooterLayout(this)
    private lateinit var list: ClientScriptsList

    override fun init() {
        this.layout.addToHeader(ScaledStringWidget(this.title, this.font, 1.5F)) { settings ->
            settings.alignHorizontallyLeft().paddingLeft(15).paddingBottom(6)
        }
        this.layout.addToHeader(this.createNewScriptButton()) { settings ->
            settings.alignHorizontallyRight().paddingRight(10).paddingBottom(2)
        }
        this.layout.addToHeader(this.createOpenScriptsDirectoryButton()) { settings ->
            settings.alignHorizontallyRight().paddingRight(35).paddingBottom(2)
        }

        this.list = this.layout.addToContents(
            ClientScriptsList(this.minecraft, this.width, this.layout.contentHeight, this.layout.headerHeight, 20)
        )

        this.layout.addToFooter(this.createDoneButton()) { settings ->
            settings.alignHorizontallyRight().paddingRight(10)
        }

        this.layout.visitWidgets(this::addRenderableWidget)
        this.repositionElements()
    }

    override fun tick() {
        this.list.tick()
    }

    override fun repositionElements() {
        this.layout.arrangeElements()
        this.list.updateSize(this.width, this.layout)
    }

    override fun onClose() {
        this.minecraft.gui.setScreen(this.parent)
    }

    private fun createNewScriptButton(): Button {
        return SpriteIconButton.builder(Component.literal("Create New Script"), {
            this.minecraft.gui.setScreen(CreateNewClientScriptScreen(this))
        }, true).sprite(kursive("icon/create"), 16, 16).size(20, 20).withTootip().build()
    }

    private fun createOpenScriptsDirectoryButton(): Button {
        return SpriteIconButton.builder(Component.literal("Open Scripts Directory"), {
            Util.getPlatform().openPath(KursiveClient.scriptsDirectory())
        }, true).sprite(kursive("icon/open_directory"), 16, 16).size(20, 20).withTootip().build()
    }

    private fun createDoneButton(): Button {
        return Button.builder(Component.literal("Done")) {
            this.onClose()
        }.size(80, 20).build()
    }
}