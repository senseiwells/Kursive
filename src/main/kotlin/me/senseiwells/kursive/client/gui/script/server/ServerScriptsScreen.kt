package me.senseiwells.kursive.client.gui.script.server

import me.senseiwells.kursive.client.gui.script.ScriptsList
import me.senseiwells.kursive.client.gui.script.ScriptsScreen
import me.senseiwells.kursive.client.sync.ClientRemoteScriptsManager
import me.senseiwells.kursive.common.network.payload.serverbound.CreateRemoteScriptPayload
import me.senseiwells.kursive.server.KursiveServer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.nio.file.Path
import kotlin.io.path.exists

class ServerScriptsScreen(
    parent: Screen? = null
): ScriptsScreen(Component.literal("Kursive Server Scripts"), parent) {
    override fun doesScriptExist(name: String): Boolean {
        val directory = this.getScriptsDirectory()
        if (directory != null) {
            return directory.resolve(name).exists()
        }
        return ClientRemoteScriptsManager.scripts.any { handle -> handle.name() == name }
    }

    override fun createNewScript(name: String) {
        ClientPlayNetworking.send(CreateRemoteScriptPayload(name))
    }

    override fun createScriptsList(): ScriptsList {
        return ServerScriptsList(this.minecraft, this.width, this.layout.contentHeight, this.layout.headerHeight)
    }

    override fun getScriptsDirectory(): Path? {
        val server = this.minecraft.singleplayerServer ?: return null
        return KursiveServer.scriptsDirectory(server)
    }

    override fun isPauseScreen(): Boolean {
        return false
    }
}