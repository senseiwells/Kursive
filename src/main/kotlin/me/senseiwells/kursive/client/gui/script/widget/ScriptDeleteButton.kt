package me.senseiwells.kursive.client.gui.script.widget

import me.senseiwells.kursive.client.gui.script.ScriptDeletionScreen
import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.client.sync.ClientRemoteScriptsManager
import me.senseiwells.kursive.client.utils.setActiveAndTooltip
import me.senseiwells.kursive.common.permissions.KursivePermissions
import me.senseiwells.kursive.common.utils.kursive
import net.casual.arcade.utils.component.red
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.network.chat.Component

class ScriptDeleteButton(
    private val screen: Screen,
    private val script: ScriptHandle
): SpriteIconButton.CenteredIcon(20, 20, NAME, 16, 16, 0, 0, WidgetSprites(DELETE), { }, NAME, { NAME }, false) {
    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (this.script.isRunning()) {
            this.setActiveAndTooltip(false, Component.literal("Cannot delete while running").red())
        } else if (!ClientRemoteScriptsManager.hasPermission(this.script, KursivePermissions.DELETE_REMOTE_SCRIPTS)) {
            this.setActiveAndTooltip(false, KursivePermissions.DELETE_REMOTE_SCRIPTS.message())
        } else {
            this.setActiveAndTooltip(true, this.message)
        }

        super.extractContents(graphics, mouseX, mouseY, a)
    }

    override fun onPress(input: InputWithModifiers) {
        Minecraft.getInstance().gui.setScreen(ScriptDeletionScreen(this.screen, this.script))
    }

    companion object {
        private val NAME = Component.literal("Delete Script")

        private val DELETE = kursive("icon/delete")
    }
}