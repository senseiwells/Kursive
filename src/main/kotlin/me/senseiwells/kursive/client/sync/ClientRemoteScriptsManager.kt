package me.senseiwells.kursive.client.sync

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.config.KursiveClientConfig
import me.senseiwells.kursive.client.script.executable.RemoteScriptHandle
import me.senseiwells.kursive.client.script.executable.ScriptHandle
import me.senseiwells.kursive.common.network.payload.clientbound.DownloadRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.clientbound.KursivePermissionsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.ListRemoteScriptsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.UpdateRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.RequestDownloadRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.RequestRemoteScriptsPayload
import me.senseiwells.kursive.common.permissions.KursivePermission
import me.senseiwells.kursive.common.permissions.KursivePermissions
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.ScriptFileUtils
import me.senseiwells.kursive.common.utils.resolveConfined
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.client.ClientTickEvent
import net.casual.arcade.events.utils.register
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import kotlin.io.path.writeBytes

object ClientRemoteScriptsManager {
    private val handles = Int2ObjectOpenHashMap<RemoteScriptHandle>()
    private val downloads = IntOpenHashSet()

    private var permissions: Set<KursivePermission> = setOf()

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

    fun hasPermission(permission: KursivePermission): Boolean {
        return this.permissions.contains(permission)
    }

    fun hasPermission(handle: ScriptHandle, permission: KursivePermission): Boolean {
        return handle !is RemoteScriptHandle || this.hasPermission(permission)
    }

    internal fun registerEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register(::onPlayerLeave)
        GlobalEventHandler.Client.register<ClientTickEvent>(phase = ClientTickEvent.PHASE_POST, listener = ::onClientTick)

        ClientPlayNetworking.registerGlobalReceiver(DownloadRemoteScriptPayload.TYPE, ::handleRemoteScriptContents)
        ClientPlayNetworking.registerGlobalReceiver(KursivePermissionsPayload.TYPE, ::handleKursivePermissions)
        ClientPlayNetworking.registerGlobalReceiver(ListRemoteScriptsPayload.TYPE, ::handleListRemoteScripts)
        ClientPlayNetworking.registerGlobalReceiver(UpdateRemoteScriptPayload.TYPE, ::handleUpdateRemoteScript)
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

    @Suppress("Unused")
    private fun handleRemoteScriptContents(payload: DownloadRemoteScriptPayload, context: ClientPlayNetworking.Context) {
        if (KursiveClientConfig.instance.allowDownloadingServerScripts && this.downloads.remove(payload.id.value)) {
            val directory = KursiveClient.remoteScriptsDirectory()
            val path = directory.resolveConfined(ScriptFileUtils.suffixate(payload.contents.name))
            path.writeBytes(payload.contents.bytes)
        }
    }

    @Suppress("Unused")
    private fun handleKursivePermissions(payload: KursivePermissionsPayload, context: ClientPlayNetworking.Context) {
        this.permissions = payload.permissions

        if (ClientPlayNetworking.canSend(RequestRemoteScriptsPayload.TYPE)) {
            if (this.hasPermission(KursivePermissions.ACCESS_REMOTE_SCRIPTS)) {
                context.responseSender().sendPacket(RequestRemoteScriptsPayload)
            }
        }
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
}