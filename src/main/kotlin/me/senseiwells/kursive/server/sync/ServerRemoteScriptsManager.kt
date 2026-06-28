package me.senseiwells.kursive.server.sync

import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.network.payload.clientbound.ListRemoteScriptsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.UpdateRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.CompileRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.RequestRemoteScriptsPayload
import me.senseiwells.kursive.common.network.payload.serverbound.StartRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.StopRemoteScriptPayload
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.kursive
import me.senseiwells.kursive.server.KursiveServer
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.utils.coroutine.launch
import net.casual.arcade.utils.player.username
import net.casual.arcade.utils.server.players
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.PermissionLevel

object ServerRemoteScriptsManager {
    internal fun registerEvents() {
        GlobalEventHandler.Server.register<ServerTickEvent>(::onServerTick)

        ServerPlayNetworking.registerGlobalReceiver(RequestRemoteScriptsPayload.TYPE, ::handleRequestRemoteScripts)

        ServerPlayNetworking.registerGlobalReceiver(CompileRemoteScriptPayload.TYPE, ::handleCompileRemoteScript)
        ServerPlayNetworking.registerGlobalReceiver(StartRemoteScriptPayload.TYPE, ::handleStartRemoteScript)
        ServerPlayNetworking.registerGlobalReceiver(StopRemoteScriptPayload.TYPE, ::handleStopRemoteScript)
    }

    @Suppress("UnstableApiUsage")
    fun isPermitted(player: ServerPlayer): Boolean {
        return player.checkPermission(kursive("remote"), PermissionLevel.OWNERS)
    }

    private fun onServerTick(event: ServerTickEvent) {
        if (KursiveServer.scripts.dirty) {
            val payload by lazy { ListRemoteScriptsPayload.from(KursiveServer.scripts) }
            for (player in event.server.players) {
                if (this.isPermitted(player) && ServerPlayNetworking.canSend(player, ListRemoteScriptsPayload.TYPE)) {
                    ServerPlayNetworking.send(player, payload)
                }
            }
        }
    }

    @Suppress("Unused")
    private fun handleRequestRemoteScripts(payload: RequestRemoteScriptsPayload, context: ServerPlayNetworking.Context) {
        if (this.isPermitted(context.player())) {
            val sender = context.responseSender()
            sender.sendPacket(ListRemoteScriptsPayload.from(KursiveServer.scripts))
        }
    }

    private fun handleCompileRemoteScript(payload: CompileRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        if (this.isPermitted(context.player())) {
            this.tryRunScriptActionAndSync(payload.id, context) { script -> script.compile() }
        }
    }

    private fun handleStartRemoteScript(payload: StartRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        if (this.isPermitted(context.player())) {
            this.tryRunScriptActionAndSync(payload.id, context) { script ->
                script.start(KursiveServer.environment(context.server(), listOf()))
            }
        }
    }

    private fun handleStopRemoteScript(payload: StopRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        if (this.isPermitted(context.player())) {
            this.tryRunScriptActionAndSync(payload.id, context) { script -> script.stop() }
        }
    }

    private inline fun tryRunScriptActionAndSync(
        id: ScriptInstance.Id,
        context: ServerPlayNetworking.Context,
        crossinline action: suspend (ScriptInstance<MinecraftServer>) -> Unit
    ) {
        val sender = context.responseSender()
        val script = KursiveServer.scripts.find(id)
        if (script == null) {
            val player = context.player()
            Kursive.logger.error("Player ${player.username} provided invalid script for action: $id, ignoring")
            return
        }

        context.server().launch {
            action.invoke(script)
            sender.sendPacket(UpdateRemoteScriptPayload.from(script))
        }
    }
}