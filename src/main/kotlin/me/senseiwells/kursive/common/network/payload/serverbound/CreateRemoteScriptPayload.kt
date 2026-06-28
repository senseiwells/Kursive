package me.senseiwells.kursive.common.network.payload.serverbound

import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class CreateRemoteScriptPayload(
    val name: String
): CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    companion object {
        val TYPE = CustomPacketPayload.Type<CreateRemoteScriptPayload>(kursive("create_remote_script"))
        val STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(::CreateRemoteScriptPayload, CreateRemoteScriptPayload::name)
    }
}