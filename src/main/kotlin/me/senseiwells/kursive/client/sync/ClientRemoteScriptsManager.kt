package me.senseiwells.kursive.client.sync

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.script.executable.RemoteScriptHandle
import me.senseiwells.kursive.common.network.payload.clientbound.ListRemoteScriptsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.UpdateRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.common.RemoteScriptContentsPayload
import me.senseiwells.kursive.common.network.payload.serverbound.RequestRemoteScriptsPayload
import me.senseiwells.kursive.common.utils.ScriptFileUtils
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

    val scripts: Collection<RemoteScriptHandle>
        get() = this.handles.values

    var dirty = false
        private set

    internal fun registerEvents() {
        ClientPlayConnectionEvents.JOIN.register(::onPlayerJoin)
        ClientPlayConnectionEvents.DISCONNECT.register(::onPlayerLeave)
        GlobalEventHandler.Client.register<ClientTickEvent>(phase = ClientTickEvent.PHASE_POST, listener = ::onClientTick)

        ClientPlayNetworking.registerGlobalReceiver(ListRemoteScriptsPayload.TYPE, ::handleListRemoteScripts)
        ClientPlayNetworking.registerGlobalReceiver(UpdateRemoteScriptPayload.TYPE, ::handleUpdateRemoteScript)
        ClientPlayNetworking.registerGlobalReceiver(RemoteScriptContentsPayload.TYPE, ::handleRemoteScriptContents)
    }

    @Suppress("Unused")
    private fun onPlayerJoin(listener: ClientPacketListener, sender: PacketSender, client: Minecraft) {
        sender.sendPacket(RequestRemoteScriptsPayload)
    }

    @Suppress("Unused")
    private fun onPlayerLeave(listener: ClientPacketListener, client: Minecraft) {
        this.handles.clear()
    }

    @Suppress("Unused")
    private fun onClientTick(event: ClientTickEvent) {
        this.dirty = false
    }

    private fun handleListRemoteScripts(payload: ListRemoteScriptsPayload, context: ClientPlayNetworking.Context) {
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
    private fun handleRemoteScriptContents(payload: RemoteScriptContentsPayload, context: ClientPlayNetworking.Context) {
        val path = KursiveClient.remoteScriptsDirectory().resolve(ScriptFileUtils.suffixate(payload.name))
        path.writeBytes(payload.contents)
    }
}