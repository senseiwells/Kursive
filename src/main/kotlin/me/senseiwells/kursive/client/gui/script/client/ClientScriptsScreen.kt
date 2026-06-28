package me.senseiwells.kursive.client.gui.script.client

import me.senseiwells.kursive.api.ClientScriptContext
import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.gui.script.ScriptsList
import me.senseiwells.kursive.client.gui.script.ScriptsScreen
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import me.senseiwells.kursive.common.utils.ScriptTemplates
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.nio.file.Path
import kotlin.io.path.exists

class ClientScriptsScreen(
    parent: Screen? = null
): ScriptsScreen(Component.literal("Kursive Client Scripts"), parent) {
    override fun doesScriptExist(name: String): Boolean {
        return this.getScriptsDirectory().resolve(name).exists()
    }

    override fun createNewScript(name: String) {
        val path = this.getScriptsDirectory().resolve(name)
        ScriptTemplates.write(
            path, Minecraft::class.java, ClientScriptContext::class.java, metadata = ScriptMetadata.named(name)
        )
    }

    override fun createScriptsList(): ScriptsList<*> {
        return ClientScriptsList(this.minecraft, this.width, this.layout.contentHeight, this.layout.headerHeight)
    }

    override fun getScriptsDirectory(): Path {
        return KursiveClient.scriptsDirectory()
    }
}