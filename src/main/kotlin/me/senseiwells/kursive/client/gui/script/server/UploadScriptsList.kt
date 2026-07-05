package me.senseiwells.kursive.client.gui.script.server

import me.senseiwells.kursive.client.gui.script.widget.ScriptUploadButton
import me.senseiwells.kursive.client.gui.widget.OpenFileButton
import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

class UploadScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int,
    private val screen: SavedServerScriptsScreen
): ContainerObjectSelectionList<UploadScriptsList.ScriptEntry>(minecraft, width, height, y, 22) {
    fun refresh(definitions: Collection<ScriptDefinition<MinecraftServer>>) {
        this.clearEntries()

        for (definition in definitions.sortedBy { it.name }) {
            this.addEntry(ScriptEntry(this, definition))
        }
    }

    override fun getRowWidth(): Int {
        return 300
    }

    class ScriptEntry(
        private val parent: UploadScriptsList,
        private val definition: ScriptDefinition<MinecraftServer>
    ): Entry<ScriptEntry>() {
        private val nameWidget = StringWidget(Component.literal("${this.definition.name}.kts"), this.parent.minecraft.font)
        private val openButton = this.createOpenButton()
        private val uploadButton = ScriptUploadButton(this.parent.screen, this.definition)

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            this.nameWidget.setPosition(this.contentX, this.contentYMiddle - 9 / 2)
            this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a)

            var buttonX = this.parent.scrollBarX() - this.uploadButton.width - 10
            val buttonY = this.contentY - 2

            this.uploadButton.setPosition(buttonX, buttonY)
            this.uploadButton.extractRenderState(graphics, mouseX, mouseY, a)

            if (this.openButton != null) {
                buttonX -= this.openButton.width + 5
                this.openButton.setPosition(buttonX, buttonY)
                this.openButton.extractRenderState(graphics, mouseX, mouseY, a)
            }
        }

        override fun children(): List<GuiEventListener> {
            return listOfNotNull(this.nameWidget, this.openButton, this.uploadButton)
        }

        override fun narratables(): List<NarratableEntry> {
            return listOfNotNull(this.nameWidget, this.openButton, this.uploadButton)
        }

        private fun createOpenButton(): Button? {
            if (this.definition is FileScriptDefinition) {
                return OpenFileButton(Component.literal("Open Script")) { this.definition.absolute }
            }
            return null
        }
    }
}