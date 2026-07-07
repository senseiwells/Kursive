package me.senseiwells.kursive.client.gui.script.server

import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.gui.script.ScriptOverwriteScreen
import me.senseiwells.kursive.client.gui.widget.DoneButton
import me.senseiwells.kursive.client.gui.widget.OpenDirectoryButton
import me.senseiwells.kursive.client.gui.widget.ScaledStringWidget
import me.senseiwells.kursive.client.sync.ClientRemoteScriptsManager
import me.senseiwells.kursive.common.network.payload.serverbound.UploadRemoteScriptPayload
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.script.definition.resolver.PolledFileScriptDefinitionSource
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

class SavedServerScriptsScreen(
    private val parent: Screen?
): Screen(Component.literal("Local Server Scripts")) {
    private val source = PolledFileScriptDefinitionSource<MinecraftServer>(KursiveClient.remoteScriptsDirectory())
    private var previous: Set<ScriptDefinition<MinecraftServer>> = setOf()

    private val layout = HeaderAndFooterLayout(this)
    private lateinit var list: UploadScriptsList

    fun upload(definition: ScriptDefinition<MinecraftServer>) {
        if (!ClientRemoteScriptsManager.doesScriptExist(definition.name)) {
            ClientPlayNetworking.send(UploadRemoteScriptPayload.from(definition))
            return
        }

        val screen = ScriptOverwriteScreen(this, definition.name, ClientRemoteScriptsManager::doesScriptExist) { name ->
            ClientPlayNetworking.send(UploadRemoteScriptPayload.from(definition, name))
        }
        this.minecraft.gui.setScreen(screen)
    }

    override fun init() {
        this.layout.addToHeader(ScaledStringWidget(this.title, this.font, 1.5F)) { settings ->
            settings.alignHorizontallyLeft().paddingLeft(15).paddingBottom(6)
        }

        val openScriptsDirectoryButton = OpenDirectoryButton(Component.literal("Open Scripts Directory")) {
            KursiveClient.remoteScriptsDirectory()
        }
        this.layout.addToHeader(openScriptsDirectoryButton) { settings ->
            settings.alignHorizontallyRight().paddingRight(10).paddingBottom(2)
        }

        this.list = this.layout.addToContents(
            UploadScriptsList(this.minecraft, this.width, this.layout.contentHeight, this.layout.headerHeight, this)
        )
        this.tick()

        this.layout.addToFooter(DoneButton(this)) { settings ->
            settings.alignHorizontallyRight().paddingRight(10)
        }

        this.layout.visitWidgets(this::addRenderableWidget)
        this.repositionElements()
    }

    override fun tick() {
        val current = this.source.get().toSet()
        if (this.previous != current) {
            this.previous = current
            this.list.refresh(current)
        }
    }

    override fun added() {
        this.source.initialize()
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun removed() {
        this.source.close()
    }

    override fun repositionElements() {
        this.layout.arrangeElements()
        this.list.updateSize(this.width, this.layout)
    }

    override fun onClose() {
        this.minecraft.gui.setScreen(this.parent)
    }
}