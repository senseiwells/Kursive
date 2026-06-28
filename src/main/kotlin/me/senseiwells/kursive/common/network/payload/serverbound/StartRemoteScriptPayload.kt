package me.senseiwells.kursive.common.network.payload.serverbound

import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class StartRemoteScriptPayload(
    val id: ScriptInstance.Id
): CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    companion object {
        val TYPE = CustomPacketPayload.Type<StartRemoteScriptPayload>(kursive("start_remote_script"))
        val STREAM_CODEC = ScriptInstance.Id.STREAM_CODEC.map(::StartRemoteScriptPayload, StartRemoteScriptPayload::id)
    }
}