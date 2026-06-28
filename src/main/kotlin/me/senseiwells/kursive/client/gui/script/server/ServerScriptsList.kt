package me.senseiwells.kursive.client.gui.script.server

import me.senseiwells.kursive.client.gui.script.ScriptsList
import me.senseiwells.kursive.client.gui.script.widget.ScriptCompileButton
import me.senseiwells.kursive.client.gui.script.widget.ScriptDiagnosticIcon
import me.senseiwells.kursive.client.gui.script.widget.ScriptNameWidget
import me.senseiwells.kursive.client.gui.script.widget.ScriptToggleButton
import me.senseiwells.kursive.client.script.executable.RemoteScriptHandle
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry

class ServerScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int
): ScriptsList<ServerScriptsList.Entry>(minecraft, width, height, y) {
    override fun refresh() {
        this.clearEntries()
    }

    override fun dirty(): Boolean {
        return false
    }

    abstract class Entry: ScriptsList.Entry<Entry>()

    class ScriptEntry(
        override val parent: ServerScriptsList,
        private val handle: RemoteScriptHandle,
    ): Entry(), ScriptsList.ScriptEntry {
        override val diagnosticIcon = ScriptDiagnosticIcon(this.handle)
        override val nameWidget = ScriptNameWidget(this.parent.minecraft.font, this.handle)
        override val compileButton = ScriptCompileButton(this.handle)
        override val toggleButton = ScriptToggleButton(this.handle)
        override val openButton = null

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            super.extractContent(graphics, mouseX, mouseY, hovered, a)
        }

        override fun children(): List<GuiEventListener> {
            return super.children()
        }

        override fun narratables(): List<NarratableEntry> {
            return super.narratables()
        }
    }
}