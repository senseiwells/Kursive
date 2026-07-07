package me.senseiwells.kursive.common.network.payload

import me.senseiwells.kursive.common.network.payload.clientbound.DownloadRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.clientbound.KursivePermissionsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.ListRemoteScriptsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.UpdateRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.*
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

internal object KursivePayloads {
    fun register() {
        val client = PayloadTypeRegistry.clientboundPlay()
        val server = PayloadTypeRegistry.serverboundPlay()

        client.register(ListRemoteScriptsPayload.TYPE, ListRemoteScriptsPayload.STREAM_CODEC)
        client.register(KursivePermissionsPayload.TYPE, KursivePermissionsPayload.STREAM_CODEC)
        client.register(UpdateRemoteScriptPayload.TYPE, UpdateRemoteScriptPayload.STREAM_CODEC)
        client.registerLarge(DownloadRemoteScriptPayload.TYPE, DownloadRemoteScriptPayload.STREAM_CODEC, DownloadRemoteScriptPayload.MAX_SIZE_BYTES)

        server.register(CompileRemoteScriptPayload.TYPE, CompileRemoteScriptPayload.STREAM_CODEC)
        server.register(CreateRemoteScriptPayload.TYPE, CreateRemoteScriptPayload.STREAM_CODEC)
        server.register(DeleteRemoteScriptPayload.TYPE, DeleteRemoteScriptPayload.STREAM_CODEC)
        server.register(RequestDownloadRemoteScriptPayload.TYPE, RequestDownloadRemoteScriptPayload.STREAM_CODEC)
        server.register(RequestRemoteScriptsPayload.TYPE, RequestRemoteScriptsPayload.STREAM_CODEC)
        server.register(RestartRemoteScriptsPayload.TYPE, RestartRemoteScriptsPayload.STREAM_CODEC)
        server.register(StartRemoteScriptPayload.TYPE, StartRemoteScriptPayload.STREAM_CODEC)
        server.register(StopRemoteScriptPayload.TYPE, StopRemoteScriptPayload.STREAM_CODEC)
        server.registerLarge(UploadRemoteScriptPayload.TYPE, UploadRemoteScriptPayload.STREAM_CODEC, UploadRemoteScriptPayload.MAX_SIZE_BYTES)
    }
}