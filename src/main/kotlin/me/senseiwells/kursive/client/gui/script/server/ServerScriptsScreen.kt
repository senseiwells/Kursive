package me.senseiwells.kursive.client.gui.script.server

import me.senseiwells.kursive.client.gui.script.ScriptsList
import me.senseiwells.kursive.client.gui.script.ScriptsScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.nio.file.Path

class ServerScriptsScreen(
    parent: Screen? = null
): ScriptsScreen(Component.literal("Kursive Server Scripts"), parent) {
    override fun doesScriptExist(name: String): Boolean {
        return false
    }

    override fun createNewScript(name: String) {

    }

    override fun createScriptsList(): ScriptsList<*> {
        return ServerScriptsList(this.minecraft, this.width, this.layout.contentHeight, this.layout.headerHeight)
    }

    override fun getScriptsDirectory(): Path? {
        return null
    }
}