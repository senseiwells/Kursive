package me.senseiwells.kursive.client.gui.script

import me.senseiwells.kursive.client.gui.script.widget.ScriptCompileButton
import me.senseiwells.kursive.client.gui.script.widget.ScriptDiagnosticIcon
import me.senseiwells.kursive.client.gui.script.widget.ScriptNameWidget
import me.senseiwells.kursive.client.gui.script.widget.ScriptToggleButton
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
import net.minecraft.network.chat.Component

abstract class ScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int
): ContainerObjectSelectionList<ScriptsList.Entry>(minecraft, width, height, y, 20) {
    abstract fun refresh()

    fun tick() {
        if (this.dirty()) {
            this.refresh()
        }
    }

    override fun getRowWidth(): Int {
        return 280
    }

    protected abstract fun dirty(): Boolean

    protected abstract fun downloader(): DownloadHandler?

    fun interface DownloadHandler {
        fun request(handle: ScriptHandle)
    }

    abstract class Entry: ContainerObjectSelectionList.Entry<Entry>()

    class ScriptEntry(
        private val parent: ScriptsList,
        private val handle: ScriptHandle
    ): Entry() {
        private val diagnosticIcon = ScriptDiagnosticIcon(this.handle)
        private val nameWidget = ScriptNameWidget(this.parent.minecraft.font, this.handle)
        private val compileButton = ScriptCompileButton(this.handle)
        private val toggleButton = ScriptToggleButton(this.handle)
        private val openButton = this.createOpenButton()
        private val downloadButton = this.createDownloadButton()

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            this.diagnosticIcon.setPosition(this.contentX - 18, this.contentYMiddle - 6)
            this.diagnosticIcon.extractRenderState(graphics, mouseX, mouseY, a)

            this.nameWidget.setPosition(this.contentX, this.contentYMiddle - 9 / 2)
            this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a)

            var buttonX = this.parent.scrollBarX() - this.toggleButton.width - 10
            val buttonY = this.contentY - 2

            this.toggleButton.setPosition(buttonX, buttonY)
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, a)

            buttonX = buttonX - this.compileButton.width - 5
            this.compileButton.setPosition(buttonX, buttonY)
            this.compileButton.extractRenderState(graphics, mouseX, mouseY, a)

            if (this.downloadButton != null) {
                buttonX = buttonX - this.downloadButton.width - 5
                this.downloadButton.setPosition(buttonX, buttonY)
                this.downloadButton.extractRenderState(graphics, mouseX, mouseY, a)
            }

            if (this.openButton != null) {
                buttonX = buttonX - this.openButton.width - 5
                this.openButton.setPosition(buttonX, buttonY)
                this.openButton.extractRenderState(graphics, mouseX, mouseY, a)
            }
        }

        override fun children(): List<GuiEventListener> {
            return listOfNotNull(this.nameWidget, this.openButton, this.downloadButton, this.compileButton, this.toggleButton)
        }

        override fun narratables(): List<NarratableEntry> {
            return listOfNotNull(this.nameWidget, this.openButton, this.downloadButton, this.compileButton, this.toggleButton)
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
            val downloader = this.parent.downloader()
            if (downloader != null) {
                return SpriteIconButton.builder(Component.literal("Download Script"), {
                    downloader.request(this.handle)
                }, true).width(20).sprite(kursive("icon/download"), 16, 16).withTootip().build()
            }
            return null
        }
    }
}