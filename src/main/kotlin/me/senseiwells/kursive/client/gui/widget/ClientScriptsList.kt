package me.senseiwells.kursive.client.gui.widget

import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.utils.TogglableScript
import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.kursive
import net.casual.arcade.utils.component.Component
import net.casual.arcade.utils.component.plus
import net.casual.arcade.utils.component.teal
import net.casual.arcade.utils.component.yellow
import net.casual.arcade.utils.coroutine.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.network.chat.Component
import net.minecraft.util.Util

class ClientScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int,
    itemHeight: Int
): ContainerObjectSelectionList<ClientScriptsList.Entry>(minecraft, width, height, y, itemHeight) {
    init {
        this.refresh()
    }

    fun refresh() {
        this.clearEntries()
        for (script in KursiveClient.scripts) {
            this.addEntry(ScriptEntry(this, script))
        }
    }

    override fun getRowWidth(): Int {
        return 280
    }

    abstract class Entry: ContainerObjectSelectionList.Entry<Entry>()

    class ScriptEntry(
        private val parent: ClientScriptsList,
        private val script: ScriptInstance<Minecraft>
    ): Entry() {
        private val nameWidget = this.createNameWidget()
        private val toggleButton = this.createToggleButton()
        private val openButton = this.createOpenButton()

        override fun extractContent(
            graphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            a: Float
        ) {
            val toggleButtonX = this.parent.scrollBarX() - this.toggleButton.width - 10
            val buttonY = this.contentY - 2

            this.nameWidget.setPosition(this.contentX, this.contentYMiddle - 9 / 2)
            this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a)

            this.toggleButton.message = if (this.script.isRunning()) Component.literal("Stop") else Component.literal("Start")
            this.toggleButton.setPosition(toggleButtonX, buttonY)
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, a)

            if (this.openButton != null) {
                val openButtonX = toggleButtonX - this.openButton.width - 5
                this.openButton.setPosition(openButtonX, buttonY)
                this.openButton.extractRenderState(graphics, mouseX, mouseY, a)
            }
        }

        override fun children(): List<GuiEventListener> {
            return listOfNotNull(this.nameWidget, this.openButton, this.toggleButton)
        }

        override fun narratables(): List<NarratableEntry> {
            return listOfNotNull(this.nameWidget, this.openButton, this.toggleButton)
        }

        private fun createNameWidget(): StringWidget {
            val widget = StringWidget(Component.literal(this.script.definition.name), this.parent.minecraft.font)
            this.parent.minecraft.launch {
                val metadata = script.getOrLoadMetadata()
                widget.setTooltip(Tooltip.create(Component {
                    empty() + literal("Script Id: ") + literal(metadata.id).yellow() + nl +
                        literal("Version: ") + literal("${metadata.version}").teal()
                }))
            }
            return widget
        }

        private fun createOpenButton(): Button? {
            if (this.script.definition is FileScriptDefinition) {
                val path = this.script.definition.absolute
                return SpriteIconButton.builder(Component.literal("Open"), {
                    Util.getPlatform().openPath(path)
                }, true).width(20).sprite(kursive("icon/open"), 16, 16).tooltip(Component.literal("Open File")).build()
            }
            return null
        }

        private fun createToggleButton(): Button {
            return ScriptToggleButton(
                TogglableScript.from(this.script, KursiveClient.environment(this.parent.minecraft, listOf()))
            )
        }
    }
}