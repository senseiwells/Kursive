package me.senseiwells.kursive.server.sync

import me.senseiwells.kursive.api.ServerScriptContext
import me.senseiwells.kursive.api.utils.ScriptType
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.network.payload.clientbound.DownloadRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.clientbound.KursivePermissionsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.ListRemoteScriptsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.UpdateRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.*
import me.senseiwells.kursive.common.permissions.KursivePermission
import me.senseiwells.kursive.common.permissions.KursivePermissions
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.ScriptFileUtils
import me.senseiwells.kursive.common.utils.ScriptTemplates
import me.senseiwells.kursive.common.utils.checkPermission
import me.senseiwells.kursive.common.utils.resolveConfined
import me.senseiwells.kursive.server.KursiveServer
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerTickEvent
import net.casual.arcade.events.server.player.PlayerSendPermissionLevelEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.utils.coroutine.launch
import net.casual.arcade.utils.player.username
import net.casual.arcade.utils.server.players
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer
import kotlin.io.path.writeBytes

object ServerRemoteScriptsManager {
    internal fun registerEvents() {
        GlobalEventHandler.Server.register<ServerTickEvent>(phase = ServerTickEvent.PHASE_POST, listener = ::onServerTick)
        GlobalEventHandler.Server.register<PlayerSendPermissionLevelEvent>(::onPlayerSendPermissionLevel)

        ServerPlayNetworking.registerGlobalReceiver(CompileRemoteScriptPayload.TYPE, ::handleCompileRemoteScript)
        ServerPlayNetworking.registerGlobalReceiver(CreateRemoteScriptPayload.TYPE, ::handleCreateRemoteScript)
        ServerPlayNetworking.registerGlobalReceiver(DeleteRemoteScriptPayload.TYPE, ::handleDeleteRemoteScript)
        ServerPlayNetworking.registerGlobalReceiver(RequestDownloadRemoteScriptPayload.TYPE, ::handleRequestDownloadRemoteScript)
        ServerPlayNetworking.registerGlobalReceiver(RequestRemoteScriptsPayload.TYPE, ::handleRequestRemoteScripts)
        ServerPlayNetworking.registerGlobalReceiver(RestartRemoteScriptsPayload.TYPE, ::handleRestartRemoteScripts)
        ServerPlayNetworking.registerGlobalReceiver(StartRemoteScriptPayload.TYPE, ::handleStartRemoteScript)
        ServerPlayNetworking.registerGlobalReceiver(StopRemoteScriptPayload.TYPE, ::handleStopRemoteScript)
        ServerPlayNetworking.registerGlobalReceiver(UploadRemoteScriptPayload.TYPE, ::handleUploadRemoteScript)
    }

    fun synchronize(server: MinecraftServer, script: ScriptInstance<MinecraftServer>) {
        this.synchronize(server, UpdateRemoteScriptPayload.from(script))
    }

    private fun synchronize(server: MinecraftServer, payload: CustomPacketPayload) {
        for (player in server.players) {
            if (player.checkPermission(KursivePermissions.ACCESS_REMOTE_SCRIPTS) && ServerPlayNetworking.canSend(player, payload.type())) {
                ServerPlayNetworking.send(player, payload)
            }
        }
    }

    private fun onServerTick(event: ServerTickEvent) {
        if (KursiveServer.scripts.dirty) {
            this.synchronize(event.server, ListRemoteScriptsPayload.from(KursiveServer.scripts))
        }
    }

    private fun onPlayerSendPermissionLevel(event: PlayerSendPermissionLevelEvent) {
        if (ServerPlayNetworking.canSend(event.player, KursivePermissionsPayload.TYPE)) {
            ServerPlayNetworking.send(event.player, KursivePermissionsPayload.from(event.player))
        }
    }

    private fun handleCompileRemoteScript(payload: CompileRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        this.handle(context, KursivePermissions.COMPILE_REMOTE_SCRIPTS) {
            this.tryRunScriptAction(payload.id, context) { script -> script.compile() }
        }
    }

    private fun handleCreateRemoteScript(payload: CreateRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        this.handle(context, KursivePermissions.CREATE_REMOTE_SCRIPTS) {
            val directory = KursiveServer.scriptsDirectory(context.server())
            val path = directory.resolveConfined(ScriptFileUtils.suffixate(payload.name))
            ScriptTemplates.write(
                path, MinecraftServer::class.java, ServerScriptContext::class.java, metadata = ScriptMetadata.named(payload.name, ScriptType.Server)
            )
        }
    }

    private fun handleDeleteRemoteScript(payload: DeleteRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        this.handle(context, KursivePermissions.DELETE_REMOTE_SCRIPTS) {
            this.tryRunScriptAction(payload.id, context) { script -> script.delete() }
        }
    }

    private fun handleRequestDownloadRemoteScript(payload: RequestDownloadRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        this.handle(context, KursivePermissions.DOWNLOAD_REMOTE_SCRIPTS) {
            val script = KursiveServer.scripts.find(payload.id) ?: return
            context.responseSender().sendPacket(DownloadRemoteScriptPayload.from(script, payload.name))
        }
    }

    @Suppress("Unused")
    private fun handleRequestRemoteScripts(payload: RequestRemoteScriptsPayload, context: ServerPlayNetworking.Context) {
        this.handle(context, KursivePermissions.ACCESS_REMOTE_SCRIPTS) {
            val sender = context.responseSender()
            sender.sendPacket(ListRemoteScriptsPayload.from(KursiveServer.scripts))
        }
    }

    @Suppress("Unused")
    private fun handleRestartRemoteScripts(payload: RestartRemoteScriptsPayload, context: ServerPlayNetworking.Context) {
        this.handle(context, KursivePermissions.RUN_REMOTE_SCRIPTS) {
            val environment = KursiveServer.environment(context.server())
            Kursive.restartScripts(environment, KursiveServer.scripts)
        }
    }

    private fun handleStartRemoteScript(payload: StartRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        this.handle(context, KursivePermissions.RUN_REMOTE_SCRIPTS) {
            this.tryRunScriptAction(payload.id, context) { script ->
                script.start(KursiveServer.environment(context.server()))
            }
        }
    }

    private fun handleStopRemoteScript(payload: StopRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        this.handle(context, KursivePermissions.RUN_REMOTE_SCRIPTS) {
            this.tryRunScriptAction(payload.id, context) { script -> script.stop() }
        }
    }

    private fun handleUploadRemoteScript(payload: UploadRemoteScriptPayload, context: ServerPlayNetworking.Context) {
        this.handle(context, KursivePermissions.UPLOAD_REMOTE_SCRIPTS) {
            val directory = KursiveServer.scriptsDirectory(context.server())
            val path = directory.resolveConfined(ScriptFileUtils.suffixate(payload.contents.name))
            path.writeBytes(payload.contents.bytes)
        }
    }

    private inline fun handle(context: ServerPlayNetworking.Context, permission: KursivePermission, action: () -> Unit) {
        val player = context.player()
        if (player.checkPermission(permission)) {
            try {
                action.invoke()
            } catch (exception: Exception) {
                Kursive.logger.error("Error while handling Kursive packet", exception)
            }
        } else {
            player.sendSystemMessage(permission.message())
        }
    }

    private inline fun tryRunScriptAction(
        id: ScriptInstance.Id,
        context: ServerPlayNetworking.Context,
        crossinline action: suspend (ScriptInstance<MinecraftServer>) -> Unit
    ) {
        val script = KursiveServer.scripts.find(id)
        if (script == null) {
            val player = context.player()
            Kursive.logger.error("Player ${player.username} provided invalid script for action: $id, ignoring")
            return
        }

        context.server().launch { action.invoke(script) }
    }
}