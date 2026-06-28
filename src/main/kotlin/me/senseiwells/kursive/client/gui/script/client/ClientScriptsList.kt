package me.senseiwells.kursive.client.gui.script.client

import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.gui.script.ScriptsList
import me.senseiwells.kursive.client.gui.script.widget.ScriptCompileButton
import me.senseiwells.kursive.client.gui.script.widget.ScriptDiagnosticIcon
import me.senseiwells.kursive.client.gui.script.widget.ScriptNameWidget
import me.senseiwells.kursive.client.gui.script.widget.ScriptToggleButton
import me.senseiwells.kursive.client.gui.widget.OpenFileButton
import me.senseiwells.kursive.client.script.executable.LocalScriptHandle
import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.network.chat.Component

class ClientScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int
): ScriptsList<ClientScriptsList.Entry>(minecraft, width, height, y) {
    override fun refresh() {
        this.clearEntries()
        for (script in KursiveClient.scripts.sortedBy { it.definition.name }) {
            this.addEntry(ScriptEntry(this, script))
        }
    }

    override fun dirty(): Boolean {
        return KursiveClient.scripts.dirty
    }

    abstract class Entry: ScriptsList.Entry<Entry>()

    class ScriptEntry(
        override val parent: ClientScriptsList,
        private val script: ScriptInstance<Minecraft>
    ): Entry(), ScriptsList.ScriptEntry {
        private val handle = LocalScriptHandle(this.script, KursiveClient.environment(this.parent.minecraft, listOf()))

        override val diagnosticIcon = ScriptDiagnosticIcon(this.handle)
        override val nameWidget = ScriptNameWidget(this.parent.minecraft.font, this.handle)
        override val compileButton = ScriptCompileButton(this.handle)
        override val toggleButton = ScriptToggleButton(this.handle)
        override val openButton = this.createOpenButton()

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            super.extractContent(graphics, mouseX, mouseY, hovered, a)
        }

        override fun children(): List<GuiEventListener> {
            return super.children()
        }

        override fun narratables(): List<NarratableEntry> {
            return super.narratables()
        }

        private fun createOpenButton(): Button? {
            if (this.script.definition is FileScriptDefinition) {
                return OpenFileButton(Component.literal("Open Script")) {
                    this.script.definition.absolute
                }
            }
            return null
        }
    }
}