package me.senseiwells.kursive.client.gui.script

import me.senseiwells.kursive.client.gui.script.widget.*
import me.senseiwells.kursive.client.gui.widget.OpenFileButton
import me.senseiwells.kursive.client.script.executable.LocalScriptHandle
import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

abstract class ScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int,
    protected val parent: Screen
): ContainerObjectSelectionList<ScriptsList.Entry>(minecraft, width, height, y, 22) {
    abstract fun refresh()

    fun tick() {
        if (this.dirty()) {
            this.refresh()
        }
    }

    override fun getRowWidth(): Int {
        return 300
    }

    protected abstract fun dirty(): Boolean

    protected abstract fun downloader(): DownloadHandler?

    fun interface DownloadHandler {
        fun request(handle: ScriptHandle)
    }

    abstract class Entry: ContainerObjectSelectionList.Entry<Entry>()

    class ScriptEntry(
        private val list: ScriptsList,
        private val handle: ScriptHandle
    ): Entry() {
        private val diagnosticIcon = ScriptDiagnosticIcon(this.handle)
        private val nameWidget = ScriptNameWidget(this.list.minecraft.font, this.handle)
        private val compileButton = ScriptCompileButton(this.handle)
        private val toggleButton = ScriptToggleButton(this.handle)
        private val deleteButton = ScriptDeleteButton(this.list.parent, this.handle)
        private val openButton = this.createOpenButton()
        private val downloadButton = this.createDownloadButton()

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            this.diagnosticIcon.setPosition(this.contentX - 18, this.contentYMiddle - 6)
            this.diagnosticIcon.extractRenderState(graphics, mouseX, mouseY, a)

            this.nameWidget.setPosition(this.contentX, this.contentYMiddle - 9 / 2)
            this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a)

            var buttonX = this.list.scrollBarX() - this.toggleButton.width - 10
            val buttonY = this.contentY - 2

            this.toggleButton.setPosition(buttonX, buttonY)
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, a)

            buttonX -= this.compileButton.width + 5
            this.compileButton.setPosition(buttonX, buttonY)
            this.compileButton.extractRenderState(graphics, mouseX, mouseY, a)

            if (this.downloadButton != null) {
                buttonX -= this.downloadButton.width + 5
                this.downloadButton.setPosition(buttonX, buttonY)
                this.downloadButton.extractRenderState(graphics, mouseX, mouseY, a)
            }

            buttonX -= this.deleteButton.width + 5
            this.deleteButton.setPosition(buttonX, buttonY)
            this.deleteButton.extractRenderState(graphics, mouseX, mouseY, a)

            if (this.openButton != null) {
                buttonX -= this.openButton.width + 5
                this.openButton.setPosition(buttonX, buttonY)
                this.openButton.extractRenderState(graphics, mouseX, mouseY, a)
            }
        }

        override fun children(): List<GuiEventListener> {
            return listOfNotNull(this.nameWidget, this.openButton, this.deleteButton, this.downloadButton, this.compileButton, this.toggleButton)
        }

        override fun narratables(): List<NarratableEntry> {
            return listOfNotNull(this.nameWidget, this.openButton, this.deleteButton, this.downloadButton, this.compileButton, this.toggleButton)
        }

        private fun createOpenButton(): Button? {
            if (this.handle is LocalScriptHandle<*>) {
                val definition = this.handle.instance.definition
                if (definition is FileScriptDefinition) {
                    return OpenFileButton(Component.literal("Open Script")) { definition.absolute }
                }
            }
            return null
        }

        private fun createDownloadButton(): Button? {
            val downloader = this.list.downloader()
            if (downloader != null) {
                return SpriteIconButton.builder(Component.literal("Download Script"), {
                    downloader.request(this.handle)
                }, true).width(20).sprite(kursive("icon/download"), 16, 16).withTootip().build()
            }
            return null
        }
    }
}