package me.senseiwells.kursive.client.sync

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.config.KursiveClientConfig
import me.senseiwells.kursive.client.script.executable.RemoteScriptHandle
import me.senseiwells.kursive.common.network.payload.clientbound.DownloadRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.clientbound.ListRemoteScriptsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.UpdateRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.RequestDownloadRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.RequestRemoteScriptsPayload
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.ScriptFileUtils
import me.senseiwells.kursive.common.utils.resolveConfined
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.client.ClientTickEvent
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import kotlin.io.path.writeBytes

object ClientRemoteScriptsManager {
    private val handles = Int2ObjectOpenHashMap<RemoteScriptHandle>()
    private val downloads = IntOpenHashSet()

    val scripts: Collection<RemoteScriptHandle>
        get() = this.handles.values

    var dirty = false
        private set

    fun doesScriptExist(name: String): Boolean {
        return this.scripts.any { handle -> handle.name() == name }
    }

    fun requestDownloadFor(id: ScriptInstance.Id, name: String? = null) {
        if (KursiveClientConfig.instance.allowDownloadingServerScripts && !this.downloads.contains(id.value)) {
            ClientPlayNetworking.send(RequestDownloadRemoteScriptPayload(id, name))
        }
    }

    internal fun registerEvents() {
        ClientPlayConnectionEvents.JOIN.register(::onPlayerJoin)
        ClientPlayConnectionEvents.DISCONNECT.register(::onPlayerLeave)
        GlobalEventHandler.Client.register<ClientTickEvent>(phase = ClientTickEvent.PHASE_POST, listener = ::onClientTick)

        ClientPlayNetworking.registerGlobalReceiver(ListRemoteScriptsPayload.TYPE, ::handleListRemoteScripts)
        ClientPlayNetworking.registerGlobalReceiver(UpdateRemoteScriptPayload.TYPE, ::handleUpdateRemoteScript)
        ClientPlayNetworking.registerGlobalReceiver(DownloadRemoteScriptPayload.TYPE, ::handleRemoteScriptContents)
    }

    @Suppress("Unused")
    private fun onPlayerJoin(listener: ClientPacketListener, sender: PacketSender, client: Minecraft) {
        sender.sendPacket(RequestRemoteScriptsPayload)
    }

    @Suppress("Unused")
    private fun onPlayerLeave(listener: ClientPacketListener, client: Minecraft) {
        this.handles.clear()
        this.downloads.clear()
    }

    @Suppress("Unused")
    private fun onClientTick(event: ClientTickEvent) {
        this.dirty = false
    }

    private fun handleListRemoteScripts(payload: ListRemoteScriptsPayload, context: ClientPlayNetworking.Context) {
        this.handles.clear()

        val sender = context.responseSender()
        for ((id, script) in payload.scripts) {
            this.handles[id.value] = RemoteScriptHandle(id, script, sender::sendPacket)
        }
        this.dirty = true
    }

    @Suppress("Unused")
    private fun handleUpdateRemoteScript(payload: UpdateRemoteScriptPayload, context: ClientPlayNetworking.Context) {
        val handle = this.handles.get(payload.id.value) ?: return
        handle.update(payload.script)
    }

    @Suppress("Unused")
    private fun handleRemoteScriptContents(payload: DownloadRemoteScriptPayload, context: ClientPlayNetworking.Context) {
        if (KursiveClientConfig.instance.allowDownloadingServerScripts && this.downloads.remove(payload.id.value)) {
            val directory = KursiveClient.remoteScriptsDirectory()
            val path = directory.resolveConfined(ScriptFileUtils.suffixate(payload.contents.name))
            path.writeBytes(payload.contents.bytes)
        }
    }
}