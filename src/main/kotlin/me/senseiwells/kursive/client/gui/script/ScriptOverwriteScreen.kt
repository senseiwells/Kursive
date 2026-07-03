package me.senseiwells.kursive.client.gui.script

import me.senseiwells.kursive.client.gui.screen.OverlayScreen
import me.senseiwells.kursive.client.gui.widget.CancelButton
import net.casual.arcade.utils.component.lime
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ScriptOverwriteScreen(
    override val parent: Screen,
    private val name: String,
    private val exists: (String) -> Boolean,
    private val action: (String?) -> Unit
): OverlayScreen(Component.literal("Script Overwrite Warning")) {
    override fun init() {
        val layout = GridLayout()
        layout.defaultCellSetting()

        val warning = Component.literal("There already exists a script named ")
            .append(Component.literal("'${this.name}'").lime())
        layout.addChild(StringWidget(warning, this.font), 0, 0) { settings -> settings.paddingTop(55) }

        val options = GridLayout()
        options.defaultCellSetting()

        val helper = options.createRowHelper(3)
        helper.addChild(CancelButton(this, width = 60), layout.newCellSettings().alignHorizontallyLeft())
        helper.addChild(this.createOverwriteButton(), layout.newCellSettings().paddingHorizontal(5))
        helper.addChild(this.createRenameButton(), layout.newCellSettings().alignHorizontallyRight())

        layout.addChild(options, 1, 0) { settings -> settings.alignHorizontallyCenter().paddingTop(10) }

        layout.arrangeElements()
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5F, 0.25F)
        layout.visitWidgets(this::addRenderableWidget)
    }

    private fun createOverwriteButton(): Button {
        return Button.builder(Component.literal("Overwrite")) {
            this.action.invoke(null)
        }.width(60).build()
    }

    private fun createRenameButton(): Button {
        return Button.builder(Component.literal("Rename")) {
            this.minecraft.gui.setScreen(RenameScreen(this.parent, this.exists, this.action))
        }.width(60).build()
    }

    class RenameScreen(
        override val parent: Screen,
        private val exists: (String) -> Boolean,
        private val action: (String) -> Unit
    ): ScriptNamingScreen(Component.literal("Rename Script")) {
        override fun description(): Component? {
            return null
        }

        override fun doesScriptExist(name: String): Boolean {
            return this.exists.invoke(name)
        }

        override fun createConfirmButton(): Button {
            return Button.builder(Component.literal("Confirm")) {
                this.action.invoke(this.getScriptNameWithoutSuffix())
                this.onClose()
            }.width(50).build()
        }
    }
}