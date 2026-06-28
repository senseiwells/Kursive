package me.senseiwells.kursive.client.gui.scripts.client

import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.gui.scripts.ScriptsList
import me.senseiwells.kursive.client.gui.scripts.widget.ScriptCompileButton
import me.senseiwells.kursive.client.gui.scripts.widget.ScriptDiagnosticIcon
import me.senseiwells.kursive.client.gui.scripts.widget.ScriptNameWidget
import me.senseiwells.kursive.client.gui.scripts.widget.ScriptToggleButton
import me.senseiwells.kursive.client.gui.widget.OpenFileButton
import me.senseiwells.kursive.client.script.executable.LocalScriptHandle
import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.network.chat.Component

class ClientScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int,
    itemHeight: Int
): ScriptsList<ClientScriptsList.Entry>(minecraft, width, height, y, itemHeight) {
    override fun refresh() {
        this.clearEntries()
        for (script in KursiveClient.scripts) {
            this.addEntry(ScriptEntry(this, script))
        }
    }

    override fun dirty(): Boolean {
        return KursiveClient.scripts.dirty
    }

    abstract class Entry: ScriptsList.Entry<Entry>()

    class ScriptEntry(
        private val parent: ClientScriptsList,
        private val script: ScriptInstance<Minecraft>
    ): Entry() {
        private val handle = LocalScriptHandle(this.script, KursiveClient.environment(this.parent.minecraft, listOf()))

        private val diagnosticIcon = this.createDiagnosticWidget()
        private val nameWidget = this.createNameWidget()
        private val compileButton = this.createCompileButton()
        private val toggleButton = this.createToggleButton()
        private val openButton = this.createOpenButton()

        override fun extractContent(
            graphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            a: Float
        ) {
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

            if (this.openButton != null) {
                val openButtonX = compileButtonX - this.openButton.width - 5
                this.openButton.setPosition(openButtonX, buttonY)
                this.openButton.extractRenderState(graphics, mouseX, mouseY, a)
            }
        }

        override fun children(): List<GuiEventListener> {
            return listOfNotNull(this.nameWidget, this.openButton, this.compileButton, this.toggleButton)
        }

        override fun narratables(): List<NarratableEntry> {
            return listOfNotNull(this.nameWidget, this.openButton, this.compileButton, this.toggleButton)
        }

        private fun createDiagnosticWidget(): ScriptDiagnosticIcon {
            return ScriptDiagnosticIcon(this.handle)
        }

        private fun createNameWidget(): StringWidget {
            return ScriptNameWidget(this.parent.minecraft.font, this.handle)
        }

        private fun createOpenButton(): Button? {
            if (this.script.definition is FileScriptDefinition) {
                return OpenFileButton(Component.literal("Open Script")) {
                    this.script.definition.absolute
                }
            }
            return null
        }

        private fun createCompileButton(): Button {
            return ScriptCompileButton(this.handle)
        }

        private fun createToggleButton(): Button {
            return ScriptToggleButton(this.handle)
        }
    }
}