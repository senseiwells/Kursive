package me.senseiwells.kursive.common.network.payload

import me.senseiwells.kursive.common.network.payload.clientbound.ListRemoteScriptsPayload
import me.senseiwells.kursive.common.network.payload.clientbound.UpdateRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.CompileRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.RequestRemoteScriptsPayload
import me.senseiwells.kursive.common.network.payload.serverbound.StartRemoteScriptPayload
import me.senseiwells.kursive.common.network.payload.serverbound.StopRemoteScriptPayload
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

internal object KursivePayloads {
    fun register() {
        PayloadTypeRegistry.clientboundPlay().register(ListRemoteScriptsPayload.TYPE, ListRemoteScriptsPayload.STREAM_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(UpdateRemoteScriptPayload.TYPE, UpdateRemoteScriptPayload.STREAM_CODEC)

        PayloadTypeRegistry.serverboundPlay().register(RequestRemoteScriptsPayload.TYPE, RequestRemoteScriptsPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(CompileRemoteScriptPayload.TYPE, CompileRemoteScriptPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(StartRemoteScriptPayload.TYPE, StartRemoteScriptPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(StopRemoteScriptPayload.TYPE, StopRemoteScriptPayload.STREAM_CODEC)
    }
}