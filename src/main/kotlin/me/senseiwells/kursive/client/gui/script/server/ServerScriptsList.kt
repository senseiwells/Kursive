package me.senseiwells.kursive.client.gui.script.server

import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.config.KursiveClientConfig
import me.senseiwells.kursive.client.gui.script.ScriptOverwriteScreen
import me.senseiwells.kursive.client.gui.script.ScriptsList
import me.senseiwells.kursive.client.script.executable.LocalScriptHandle
import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.client.sync.ClientRemoteScriptsManager
import me.senseiwells.kursive.common.network.payload.serverbound.DownloadRemoteScriptPayload
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.ScriptFileUtils
import me.senseiwells.kursive.server.KursiveServer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft

class ServerScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int,
    screen: ServerScriptsScreen
): ScriptsList(minecraft, width, height, y, screen) {
    private val downloader = ServerDownloadHandler(minecraft, screen)

    override fun refresh() {
        this.clearEntries()

        if (KursiveClientConfig.TREAT_INTEGRATED_AS_LOCAL) {
            val server = this.minecraft.singleplayerServer
            if (server != null) {
                val environment = KursiveServer.environment(server, listOf())
                for (script in KursiveServer.scripts.sortedBy { it.definition.name }) {
                    val handle = LocalScriptHandle(script, environment)
                    this.addEntry(ScriptEntry(this, handle))
                }
                return
            }
        }

        for (script in ClientRemoteScriptsManager.scripts.sortedBy { it.name() }) {
            this.addEntry(ScriptEntry(this, script))
        }
    }

    override fun dirty(): Boolean {
        return ClientRemoteScriptsManager.dirty
    }

    override fun downloader(): DownloadHandler {
        return this.downloader
    }

    private class ServerDownloadHandler(
        private val minecraft: Minecraft,
        private val screen: ServerScriptsScreen
    ): DownloadHandler {
        override fun request(handle: ScriptHandle) {
            if (!KursiveClient.doesRemoteScriptExist(ScriptFileUtils.suffixate(handle.name()))) {
                this.request(handle.id())
                return
            }

            val screen = ScriptOverwriteScreen(this.screen, handle.name(), KursiveClient::doesRemoteScriptExist) { name ->
                this.request(handle.id(), name)
            }
            this.minecraft.gui.setScreen(screen)
        }

        private fun request(id: ScriptInstance.Id, name: String? = null) {
            ClientPlayNetworking.send(DownloadRemoteScriptPayload(id, name))
        }
    }
}