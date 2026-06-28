package me.senseiwells.kursive.client.gui.script

import me.senseiwells.kursive.client.gui.script.widget.ScriptCompileButton
import me.senseiwells.kursive.client.gui.script.widget.ScriptDiagnosticIcon
import me.senseiwells.kursive.client.gui.script.widget.ScriptNameWidget
import me.senseiwells.kursive.client.gui.script.widget.ScriptToggleButton
import me.senseiwells.kursive.client.gui.widget.OpenFileButton
import me.senseiwells.kursive.client.script.executable.LocalScriptHandle
import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.network.chat.Component

abstract class ScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int
): ContainerObjectSelectionList<ScriptsList.Entry>(minecraft, width, height, y, 20) {
    init {
        this.refresh()
    }

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

    abstract class Entry: ContainerObjectSelectionList.Entry<Entry>()

    class ScriptEntry(
        private val parent: ScriptsList,
        private val handle: ScriptHandle
    ): Entry() {
        private val diagnosticIcon = ScriptDiagnosticIcon(this.handle)
        private val nameWidget = ScriptNameWidget(this.parent.minecraft.font, this.handle)
        private val compileButton = ScriptCompileButton(this.handle)
        private val toggleButton = ScriptToggleButton(this.handle)
        private val openButton = this.createOpenButton(this.handle)

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            this.diagnosticIcon.setPosition(this.contentX - 18, this.contentYMiddle - 6)
            this.diagnosticIcon.extractRenderState(graphics, mouseX, mouseY, a)

            this.nameWidget.setPosition(this.contentX, this.contentYMiddle - 9 / 2)
            this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a)

            val toggleButtonX = this.parent.scrollBarX() - this.toggleButton.width - 10
            val buttonY = this.contentY - 2

            this.toggleButton.setPosition(toggleButtonX, buttonY)
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, a)

            val compileButtonX = toggleButtonX - this.compileButton.width - 5
            this.compileButton.setPosition(compileButtonX, buttonY)
            this.compileButton.extractRenderState(graphics, mouseX, mouseY, a)

            val openButton = this.openButton
            if (openButton != null) {
                val openButtonX = compileButtonX - openButton.width - 5
                openButton.setPosition(openButtonX, buttonY)
                openButton.extractRenderState(graphics, mouseX, mouseY, a)
            }
        }

        override fun children(): List<GuiEventListener> {
            return listOfNotNull(this.nameWidget, this.openButton, this.compileButton, this.toggleButton)
        }

        override fun narratables(): List<NarratableEntry> {
            return listOfNotNull(this.nameWidget, this.openButton, this.compileButton, this.toggleButton)
        }

        private fun createOpenButton(handle: ScriptHandle): Button? {
            if (handle is LocalScriptHandle<*>) {
                val definition = handle.instance.definition
                if (definition is FileScriptDefinition) {
                    return OpenFileButton(Component.literal("Open Script")) { definition.absolute }
                }
            }
            return null
        }
    }
}