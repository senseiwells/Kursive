package me.senseiwells.kursive.client.gui.script

import me.senseiwells.kursive.client.gui.screen.OverlayScreen
import me.senseiwells.kursive.client.gui.widget.CancelButton
import me.senseiwells.kursive.client.script.executable.ScriptHandle
import net.casual.arcade.utils.component.lime
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ScriptDeletionScreen(
    override val parent: Screen,
    private val handle: ScriptHandle
): OverlayScreen(Component.literal("Script Deletion Confirmation")) {
    override fun init() {
        val layout = GridLayout()
        layout.defaultCellSetting()

        val warning = Component.literal("Are you sure you want to delete the script ")
            .append(Component.literal("'${this.handle.name()}'").lime()).append("?")
        layout.addChild(StringWidget(warning, this.font), 0, 0) { settings -> settings.paddingTop(55) }

        val options = GridLayout()
        options.defaultCellSetting()

        options.addChild(CancelButton(this, name = Component.literal("No"), width = 60), 0, 0) { settings ->
            settings.paddingHorizontal(3)
        }
        options.addChild(this.createYesButton(), 0, 1) { settings ->
            settings.paddingHorizontal(3)
        }

        layout.addChild(options, 1, 0) { settings -> settings.alignHorizontallyCenter().paddingTop(10) }

        layout.arrangeElements()
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5F, 0.25F)
        layout.visitWidgets(this::addRenderableWidget)
    }

    private fun createYesButton(): Button {
        return Button.builder(Component.literal("Yes")) {
            this.handle.delete()
            this.onClose()
        }.width(60).build()
    }
}