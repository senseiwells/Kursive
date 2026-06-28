package me.senseiwells.kursive.client.gui.script

import me.senseiwells.kursive.client.gui.script.widget.ScriptCompileButton
import me.senseiwells.kursive.client.gui.script.widget.ScriptDiagnosticIcon
import me.senseiwells.kursive.client.gui.script.widget.ScriptNameWidget
import me.senseiwells.kursive.client.gui.script.widget.ScriptToggleButton
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry

abstract class ScriptsList<E: ScriptsList.Entry<E>>(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int
): ContainerObjectSelectionList<E>(minecraft, width, height, y, 20) {
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

    abstract class Entry<E: Entry<E>>: ContainerObjectSelectionList.Entry<E>()

    interface ScriptEntry {
        val parent: ScriptsList<*>

        val diagnosticIcon: ScriptDiagnosticIcon
        val nameWidget: ScriptNameWidget
        val compileButton: ScriptCompileButton
        val toggleButton: ScriptToggleButton
        val openButton: Button?

        fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            this.diagnosticIcon.setPosition(this.getContentX() - 18, this.getContentYMiddle() - 6)
            this.diagnosticIcon.extractRenderState(graphics, mouseX, mouseY, a)

            this.nameWidget.setPosition(this.getContentX(), this.getContentYMiddle() - 9 / 2)
            this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a)

            val toggleButtonX = this.parent.scrollBarX() - this.toggleButton.width - 10
            val buttonY = this.getContentY() - 2

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

        fun children(): List<GuiEventListener> {
            return listOfNotNull(this.nameWidget, this.openButton, this.compileButton, this.toggleButton)
        }

        fun narratables(): List<NarratableEntry> {
            return listOfNotNull(this.nameWidget, this.openButton, this.compileButton, this.toggleButton)
        }

        fun getContentX(): Int

		fun getContentY(): Int

		fun getContentYMiddle(): Int
    }
}