package me.senseiwells.kursive.client.gui.script.widget

import me.senseiwells.kursive.client.config.KursiveClientConfig
import me.senseiwells.kursive.client.gui.script.server.SavedServerScriptsScreen
import me.senseiwells.kursive.client.sync.ClientRemoteScriptsManager
import me.senseiwells.kursive.client.utils.setActiveAndTooltip
import me.senseiwells.kursive.common.network.payload.serverbound.UploadRemoteScriptPayload
import me.senseiwells.kursive.common.permissions.KursivePermissions
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.utils.kursive
import net.casual.arcade.utils.component.red
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

class ScriptUploadButton(
    private val screen: SavedServerScriptsScreen,
    private val definition: ScriptDefinition<MinecraftServer>,
): SpriteIconButton.CenteredIcon(20, 20, NAME, 16, 16, 0, 0, WidgetSprites(UPLOAD), { }, NAME, { NAME }, false) {
    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (!ClientPlayNetworking.canSend(UploadRemoteScriptPayload.TYPE)) {
            this.setActiveAndTooltip(false, Component.literal("Cannot upload script").red())
        } else if (!this.hasPermissionToUpload()) {
            this.setActiveAndTooltip(false, KursivePermissions.UPLOAD_REMOTE_SCRIPTS.message())
        } else {
            this.setActiveAndTooltip(true, this.message)
        }

        super.extractContents(graphics, mouseX, mouseY, a)
    }

    override fun onPress(input: InputWithModifiers) {
        this.screen.upload(this.definition)
    }

    private fun hasPermissionToUpload(): Boolean {
        if (KursiveClientConfig.instance.treatIntegratedAsLocal) {
            val minecraft = Minecraft.getInstance()
            if (minecraft.hasSingleplayerServer()) {
                return true
            }
        }

        return ClientRemoteScriptsManager.hasPermission(KursivePermissions.UPLOAD_REMOTE_SCRIPTS)
    }

    companion object {
        private val NAME = Component.literal("Upload Script")

        private val UPLOAD = kursive("icon/upload")
    }
}