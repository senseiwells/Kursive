package me.senseiwells.kursive.client.gui.script.widget

import me.senseiwells.kursive.client.config.KursiveClientConfig
import me.senseiwells.kursive.client.gui.script.ScriptCreationScreen
import me.senseiwells.kursive.client.gui.script.ScriptsScreen
import me.senseiwells.kursive.client.gui.script.client.ClientScriptsScreen
import me.senseiwells.kursive.client.sync.ClientRemoteScriptsManager
import me.senseiwells.kursive.client.utils.setActiveAndTooltip
import me.senseiwells.kursive.common.permissions.KursivePermissions
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.network.chat.Component

class ScriptCreationButton(
    private val minecraft: Minecraft,
    private val screen: ScriptsScreen
): SpriteIconButton.CenteredIcon(20, 20, NAME, 16, 16, 0, 0, WidgetSprites(CREATE), { }, NAME, { NAME }, false) {
    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (!this.hasPermissionToCreate()) {
            this.setActiveAndTooltip(false, KursivePermissions.CREATE_REMOTE_SCRIPTS.message())
        } else {
            this.setActiveAndTooltip(true, this.message)
        }

        super.extractContents(graphics, mouseX, mouseY, a)
    }

    override fun onPress(input: InputWithModifiers) {
        this.minecraft.gui.setScreen(ScriptCreationScreen(this.screen))
    }

    private fun hasPermissionToCreate(): Boolean {
        if (this.screen is ClientScriptsScreen) {
            return true
        }

        if (KursiveClientConfig.instance.treatIntegratedAsLocal) {
            val minecraft = Minecraft.getInstance()
            if (minecraft.hasSingleplayerServer()) {
                return true
            }
        }

        return ClientRemoteScriptsManager.hasPermission(KursivePermissions.CREATE_REMOTE_SCRIPTS)
    }

    companion object {
        private val NAME = Component.literal("Create New Script")

        private val CREATE = kursive("icon/create")
    }
}