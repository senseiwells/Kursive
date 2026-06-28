package me.senseiwells.kursive.common.network.payload.serverbound

import io.netty.buffer.ByteBuf
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

object RequestRemoteScriptsPayload: CustomPacketPayload {
    val TYPE = CustomPacketPayload.Type<RequestRemoteScriptsPayload>(kursive("request_remote_scripts"))
    val STREAM_CODEC = StreamCodec.unit<ByteBuf, _>(this)

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }
}