package me.senseiwells.kursive.client.gui.scripts.client

import kotlinx.io.IOException
import me.senseiwells.kursive.api.ClientScriptContext
import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.gui.scripts.ScriptsList
import me.senseiwells.kursive.client.gui.scripts.ScriptsScreen
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import me.senseiwells.kursive.common.utils.ScriptTemplates
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText

class ClientScriptsScreen(
    parent: Screen? = null
): ScriptsScreen(Component.literal("Kursive Client Scripts"), parent) {
    override fun doesScriptExist(name: String): Boolean {
        return this.getScriptsDirectory().resolve(name).exists()
    }

    override fun createNewScript(name: String) {
        val path = this.getScriptsDirectory().resolve(name)
        val template = ScriptTemplates.create(
            Minecraft::class.java, ClientScriptContext::class.java, metadata = ScriptMetadata.named(name)
        )
        try {
            path.writeText(template)
        } catch (e: IOException) {
            Kursive.logger.error("Failed to create script", e)
        }
    }

    override fun createScriptsList(): ScriptsList<*> {
        return ClientScriptsList(this.minecraft, this.width, this.layout.contentHeight, this.layout.headerHeight, 20)
    }

    override fun getScriptsDirectory(): Path {
        return KursiveClient.scriptsDirectory()
    }
}