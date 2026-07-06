package me.senseiwells.kursive.common.network.payload.clientbound

import me.senseiwells.kursive.common.network.data.RemoteScriptContents
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer

class DownloadRemoteScriptPayload(
    val id: ScriptInstance.Id,
    val contents: RemoteScriptContents
): CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    companion object {
        const val MAX_SIZE_BYTES = 1024 * 1024 * 1024
        val TYPE = CustomPacketPayload.Type<DownloadRemoteScriptPayload>(kursive("download_remote_script"))
        val STREAM_CODEC = StreamCodec.composite(
            ScriptInstance.Id.STREAM_CODEC, DownloadRemoteScriptPayload::id,
            RemoteScriptContents.STREAM_CODEC, DownloadRemoteScriptPayload::contents,
            ::DownloadRemoteScriptPayload
        )

        fun from(instance: ScriptInstance<MinecraftServer>, name: String? = null): DownloadRemoteScriptPayload {
            return DownloadRemoteScriptPayload(instance.id, RemoteScriptContents.from(instance.definition, name))
        }
    }
}