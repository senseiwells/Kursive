package me.senseiwells.kursive.client.gui.script

import me.senseiwells.kursive.client.gui.widget.ScaledStringWidget
import me.senseiwells.kursive.client.utils.FilenameUtils
import me.senseiwells.kursive.client.utils.setTooltip
import net.casual.arcade.utils.component.red
import net.casual.arcade.utils.component.silver
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB

class NewScriptScreen(
    private val parent: ScriptsScreen
): Screen(Component.literal("Create New Kursive Script")) {
    private lateinit var nameBox: EditBox
    private lateinit var createButton: Button

    override fun init() {
        val layout = GridLayout()
        layout.defaultCellSetting()

        val helper = layout.createRowHelper(3)
        helper.addChild(
            ScaledStringWidget(Component.literal("New Script"), this.font, 1.2F),
            2, layout.newCellSettings().alignHorizontallyLeft().paddingTop(45)
        )
        helper.addChild(
            StringWidget(Component.literal("Name").silver(), this.font),
            2, layout.newCellSettings().alignHorizontallyLeft().paddingTop(8)
        )
        this.nameBox = helper.addChild(
            EditBox(this.font, 160, 16, Component.literal("Script Name")),
            3, layout.newCellSettings().alignHorizontallyLeft().paddingTop(1)
        )
        helper.addChild(
            ScaledStringWidget(
                Component.literal("Creates a new .kts file in the scripts folder").silver(),
                this.font,
                0.62F
            ),
            3, layout.newCellSettings().alignHorizontallyLeft().paddingTop(12)
        )

        layout.addChild(this.createCancelButton(), 4, 1) { settings ->
            settings.paddingTop(5)
        }
        this.createButton = layout.addChild(this.createCreateButton(), 4, 2) { settings ->
            settings.alignHorizontallyRight().paddingTop(5)
        }

        layout.arrangeElements()
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5F, 0.25F)
        layout.visitWidgets(this::addRenderableWidget)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        this.parent.extractRenderState(graphics, 0, 0, a)
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ARGB.black(120))

        this.updateCreateButtonActive()

        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun onClose() {
        this.minecraft.gui.setScreen(this.parent)
    }

    override fun resize(width: Int, height: Int) {
        this.parent.resize(width, height)
        super.resize(width, height)
    }

    private fun updateCreateButtonActive() {
        val raw = this.nameBox.value
        val name = this.getScriptName()
        if (raw.isBlank() || !FilenameUtils.isValidFilename(name)) {
            this.createButton.active = false
            this.createButton.setTooltip(Component.literal("Invalid script name").red())
        } else if (this.parent.doesScriptExist(name)) {
            this.createButton.active = false
            this.createButton.setTooltip(Component.literal("Script already exists").red())
        } else {
            this.createButton.active = true
            this.createButton.setTooltip(null)
        }
    }

    private fun getScriptName(): String {
        val name = this.nameBox.value
        if (name.endsWith(".main.kts")) {
            return name
        }
        return "${name.removeSuffix(".kts")}.main.kts"
    }

    private fun createCancelButton(): Button {
        return Button.builder(Component.literal("Cancel")) {
            this.onClose()
        }.width(50).build()
    }

    private fun createCreateButton(): Button {
        return Button.builder(Component.literal("Create")) {
            this.parent.createNewScript(this.getScriptName())
            this.onClose()
        }.width(50).build()
    }
}