package me.senseiwells.kursive.common.network.payload.clientbound

import me.senseiwells.kursive.common.network.data.RemoteScriptData
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer

private typealias Scripts = Map<ScriptInstance.Id, RemoteScriptData>

class ListRemoteScriptsPayload(
    val scripts: Scripts
): CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    companion object {
        val TYPE = CustomPacketPayload.Type<ListRemoteScriptsPayload>(kursive("list_remote_scripts"))
        val STREAM_CODEC = ByteBufCodecs.map<_, _, _, Scripts>(::HashMap, ScriptInstance.Id.STREAM_CODEC, RemoteScriptData.STREAM_CODEC)
            .map(::ListRemoteScriptsPayload, ListRemoteScriptsPayload::scripts)

        fun from(instances: Iterable<ScriptInstance<MinecraftServer>>): ListRemoteScriptsPayload {
            return ListRemoteScriptsPayload(instances.associate { instance -> instance.id to RemoteScriptData.from(instance) })
        }
    }
}