package me.senseiwells.kursive.client.gui.script.widget

import me.senseiwells.kursive.client.gui.script.server.SavedServerScriptsScreen
import me.senseiwells.kursive.client.utils.setTooltip
import me.senseiwells.kursive.common.network.payload.common.RemoteScriptContentsPayload
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.utils.kursive
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
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
        if (ClientPlayNetworking.canSend(RemoteScriptContentsPayload.TYPE)) {
            this.active = true
            this.setTooltip(this.message)
        } else {
            this.active = false
            this.setTooltip(Component.literal("Cannot upload script"))
        }

        super.extractContents(graphics, mouseX, mouseY, a)
    }

    override fun onPress(input: InputWithModifiers) {
        this.screen.upload(this.definition)
    }

    companion object {
        private val NAME = Component.literal("Upload Script")

        private val UPLOAD = kursive("icon/upload")
    }
}