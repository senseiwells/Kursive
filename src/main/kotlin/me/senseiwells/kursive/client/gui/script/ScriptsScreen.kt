package me.senseiwells.kursive.client.gui.script

import me.senseiwells.kursive.client.gui.script.server.ServerScriptsScreen
import me.senseiwells.kursive.client.gui.script.server.SavedServerScriptsScreen
import me.senseiwells.kursive.client.gui.widget.DoneButton
import me.senseiwells.kursive.client.gui.widget.OpenDirectoryButton
import me.senseiwells.kursive.client.gui.widget.ScaledStringWidget
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.nio.file.Path

abstract class ScriptsScreen(
    title: Component,
    private val parent: Screen?
): Screen(title) {
    protected val layout = HeaderAndFooterLayout(this)
    protected lateinit var list: ScriptsList

    override fun init() {
        this.layout.addToHeader(ScaledStringWidget(this.title, this.font, 1.5F)) { settings ->
            settings.alignHorizontallyLeft().paddingLeft(15).paddingBottom(6)
        }

        var paddingRight = 10
        this.layout.addToHeader(this.createNewScriptButton()) { settings ->
            settings.alignHorizontallyRight().paddingRight(paddingRight).paddingBottom(2)
        }

        val uploadScriptsButton = this.createUploadScriptsButton()
        if (uploadScriptsButton != null) {
            paddingRight += uploadScriptsButton.width + 5
            this.layout.addToHeader(uploadScriptsButton) { settings ->
                settings.alignHorizontallyRight().paddingRight(paddingRight).paddingBottom(2)
            }
        }

        val openScriptsDirectoryButton = this.createOpenScriptsDirectoryButton()
        if (openScriptsDirectoryButton != null) {
            paddingRight += openScriptsDirectoryButton.width + 5
            this.layout.addToHeader(openScriptsDirectoryButton) { settings ->
                settings.alignHorizontallyRight().paddingRight(paddingRight).paddingBottom(2)
            }
        }

        this.list = this.layout.addToContents(this.createScriptsList())

        this.layout.addToFooter(DoneButton(this)) { settings ->
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

    abstract fun doesScriptExist(name: String): Boolean

    abstract fun createNewScript(name: String)

    protected abstract fun createScriptsList(): ScriptsList

    protected abstract fun getScriptsDirectory(): Path?

    private fun createNewScriptButton(): Button {
        return SpriteIconButton.builder(Component.literal("Create New Script"), {
            this.minecraft.gui.setScreen(NewScriptScreen(this))
        }, true).sprite(kursive("icon/create"), 16, 16).size(20, 20).withTootip().build()
    }

    private fun createOpenScriptsDirectoryButton(): Button? {
        val directory = this.getScriptsDirectory() ?: return null
        return OpenDirectoryButton(Component.literal("Open Scripts Directory")) { directory }
    }

    private fun createUploadScriptsButton(): Button? {
        if (this is ServerScriptsScreen) {
            return SpriteIconButton.builder(Component.literal("Upload Scripts"), {
                this.minecraft.gui.setScreen(SavedServerScriptsScreen(this))
            }, true).sprite(kursive("icon/upload"), 16, 16).size(20, 20).withTootip().build()
        }
        return null
    }
}