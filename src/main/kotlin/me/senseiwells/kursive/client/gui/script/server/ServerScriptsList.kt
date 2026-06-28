package me.senseiwells.kursive.client.gui.script.server

import me.senseiwells.kursive.client.config.KursiveClientConfig
import me.senseiwells.kursive.client.gui.script.ScriptsList
import me.senseiwells.kursive.client.script.executable.LocalScriptHandle
import me.senseiwells.kursive.client.sync.ClientRemoteScriptsManager
import me.senseiwells.kursive.server.KursiveServer
import net.minecraft.client.Minecraft

class ServerScriptsList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int
): ScriptsList(minecraft, width, height, y) {
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
}