package me.senseiwells.kursive.common.network.payload.serverbound

import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.kursive
import net.casual.arcade.utils.optional.optional
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import kotlin.jvm.optionals.getOrNull

class DownloadRemoteScriptPayload(
    val id: ScriptInstance.Id,
    val name: String? = null
): CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    companion object {
        val TYPE = CustomPacketPayload.Type<DownloadRemoteScriptPayload>(kursive("download_remote_script"))
        val STREAM_CODEC = StreamCodec.composite(
            ScriptInstance.Id.STREAM_CODEC, DownloadRemoteScriptPayload::id,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), DownloadRemoteScriptPayload::name.optional()
        ) { id, name -> DownloadRemoteScriptPayload(id, name.getOrNull()) }
    }
}