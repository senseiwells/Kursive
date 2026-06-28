package me.senseiwells.kursive.client.gui.widget

import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.utils.TogglableScript
import me.senseiwells.kursive.client.utils.setTooltip
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

    fun tick() {
        if (KursiveClient.scripts.dirty) {
            this.refresh()
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

            this.toggleButton.message = if (this.script.isRunning()) Component.literal("Stop Script") else Component.literal("Start Script")
            this.toggleButton.setTooltip(this.toggleButton.message)
            this.toggleButton.setPosition(toggleButtonX, buttonY)
            this.toggleButton.extractRenderState(graphics, mouseX, mouseY, a)

            val compileButtonX = toggleButtonX - this.compileButton.width - 5
            this.compileButton.setPosition(compileButtonX, buttonY)
            this.compileButton.extractRenderState(graphics, mouseX, mouseY, a)
            if (this.script.isRunning()) {
                this.compileButton.active = false
                this.compileButton.setTooltip(Component.literal("Cannot recompile while running"))
            } else {
                this.compileButton.active = true
                this.compileButton.setTooltip(this.compileButton.message)
            }

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
            return ScriptDiagnosticIcon(
                this.script::isRunning,
                this.script::isCompiled,
                this.script::getLatestScriptDiagnostics
            )
        }

        private fun createNameWidget(): StringWidget {
            val widget = StringWidget(
                Component.literal("${this.script.definition.name}.kts"), this.parent.minecraft.font
            )
            this.parent.minecraft.launch {
                val metadata = script.tryGetOrLoadMetadata() ?: return@launch
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
                return SpriteIconButton.builder(Component.literal("Open Script"), {
                    Util.getPlatform().openPath(path)
                }, true).width(20).sprite(kursive("icon/open_file"), 16, 16).withTootip().build()
            }
            return null
        }

        private fun createCompileButton(): Button {
            return SpriteIconButton.builder(Component.literal("Compile Script"), {
                this.parent.minecraft.launch { script.compile() }
            }, true).width(20).sprite(kursive("icon/compile"), 20, 16).withTootip().build()
        }

        private fun createToggleButton(): Button {
            return ScriptToggleButton(
                TogglableScript.from(this.script, KursiveClient.environment(this.parent.minecraft, listOf()))
            )
        }
    }
}