package me.senseiwells.kursive.client.gui.script

import me.senseiwells.kursive.client.gui.screen.OverlayScreen
import me.senseiwells.kursive.client.gui.widget.CancelButton
import me.senseiwells.kursive.client.gui.widget.ScaledStringWidget
import me.senseiwells.kursive.client.utils.FilenameUtils
import me.senseiwells.kursive.client.utils.setTooltip
import me.senseiwells.kursive.common.utils.ScriptFileUtils
import net.casual.arcade.utils.component.bold
import net.casual.arcade.utils.component.red
import net.casual.arcade.utils.component.silver
import net.casual.arcade.utils.component.wrap
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.network.chat.Component

abstract class ScriptNamingScreen(title: Component): OverlayScreen(title) {
    private lateinit var nameBox: EditBox
    private lateinit var confirmButton: Button

    override fun init() {
        val layout = GridLayout()
        layout.defaultCellSetting()

        val helper = layout.createRowHelper(3)
        helper.addChild(
            StringWidget(this.title.wrap().bold(), this.font),
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
        val description = this.description()
        if (description != null) {
            helper.addChild(
                ScaledStringWidget(description.wrap().silver(), this.font, 0.62F),
                3, layout.newCellSettings().alignHorizontallyLeft().paddingTop(12)
            )
        }

        layout.addChild(this.createCancelButton(), 4, 1) { settings ->
            settings.paddingTop(5)
        }
        this.confirmButton = layout.addChild(this.createConfirmButton(), 4, 2) { settings ->
            settings.alignHorizontallyRight().paddingTop(5)
        }

        layout.arrangeElements()
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5F, 0.25F)
        layout.visitWidgets(this::addRenderableWidget)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        this.updateCreateButtonActive()

        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    protected abstract fun description(): Component?

    protected abstract fun doesScriptExist(name: String): Boolean

    protected abstract fun createConfirmButton(): Button

    protected fun getScriptName(): String {
        val name = this.nameBox.value
        if (name.endsWith(ScriptFileUtils.SUFFIX)) {
            return name
        }
        return "${name.removeSuffix(".kts")}${ScriptFileUtils.SUFFIX}"
    }

    protected fun getScriptNameWithoutSuffix(): String {
        return this.getScriptName().removeSuffix(ScriptFileUtils.SUFFIX)
    }

    private fun createCancelButton(): Button {
        return CancelButton(this, width = 50)
    }

    private fun updateCreateButtonActive() {
        val raw = this.nameBox.value
        val name = this.getScriptName()
        if (raw.isBlank() || !FilenameUtils.isValidFilename(name)) {
            this.confirmButton.active = false
            this.confirmButton.setTooltip(Component.literal("Invalid script name").red())
        } else if (this.doesScriptExist(name)) {
            this.confirmButton.active = false
            this.confirmButton.setTooltip(Component.literal("Script already exists").red())
        } else {
            this.confirmButton.active = true
            this.confirmButton.setTooltip(null)
        }
    }
}