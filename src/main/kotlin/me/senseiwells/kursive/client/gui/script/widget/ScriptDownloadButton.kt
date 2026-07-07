package me.senseiwells.kursive.client.gui.script.widget

import me.senseiwells.kursive.client.config.KursiveClientConfig
import me.senseiwells.kursive.client.gui.script.ScriptsList
import me.senseiwells.kursive.client.script.executable.RemoteScriptHandle
import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.client.sync.ClientRemoteScriptsManager
import me.senseiwells.kursive.client.utils.setActiveAndTooltip
import me.senseiwells.kursive.common.permissions.KursivePermissions
import me.senseiwells.kursive.common.utils.kursive
import net.casual.arcade.utils.component.red
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.network.chat.Component

class ScriptDownloadButton(
    private val script: ScriptHandle,
    private val downloader: ScriptsList.DownloadHandler
): SpriteIconButton.CenteredIcon(20, 20, NAME, 16, 16, 0, 0, WidgetSprites(DOWNLOAD), { }, NAME, { NAME }, false) {
    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (this.script is RemoteScriptHandle && !KursiveClientConfig.instance.allowDownloadingServerScripts) {
            this.setActiveAndTooltip(false, Component.literal("Downloading server scripts must be enabled in your config").red())
        } else if (!ClientRemoteScriptsManager.hasPermission(this.script, KursivePermissions.DOWNLOAD_REMOTE_SCRIPTS)) {
            this.setActiveAndTooltip(false, KursivePermissions.DOWNLOAD_REMOTE_SCRIPTS.message())
        } else {
            this.setActiveAndTooltip(true, this.message)
        }

        super.extractContents(graphics, mouseX, mouseY, a)
    }

    override fun onPress(input: InputWithModifiers) {
        this.downloader.request(this.script)
    }

    companion object {
        private val NAME = Component.literal("Download Script")

        private val DOWNLOAD = kursive("icon/download")
    }
}