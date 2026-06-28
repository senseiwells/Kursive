package me.senseiwells.kursive.common.network.payload.clientbound

import me.senseiwells.kursive.common.network.data.RemoteScriptData
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer

class UpdateRemoteScriptPayload(
    val id: ScriptInstance.Id,
    val script: RemoteScriptData
): CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    companion object {
        val TYPE = CustomPacketPayload.Type<UpdateRemoteScriptPayload>(kursive("update_remote_script"))
        val STREAM_CODEC = StreamCodec.composite(
            ScriptInstance.Id.STREAM_CODEC, UpdateRemoteScriptPayload::id,
            RemoteScriptData.STREAM_CODEC, UpdateRemoteScriptPayload::script,
            ::UpdateRemoteScriptPayload
        )

        fun from(instance: ScriptInstance<MinecraftServer>): UpdateRemoteScriptPayload {
            return UpdateRemoteScriptPayload(instance.id, RemoteScriptData.from(instance))
        }
    }
}