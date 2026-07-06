package me.senseiwells.kursive.common.network.payload.serverbound

import me.senseiwells.kursive.common.network.data.RemoteScriptContents
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer

class UploadRemoteScriptPayload(
    val contents: RemoteScriptContents
): CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    companion object {
        const val MAX_SIZE_BYTES = 1024 * 1024 * 1024
        val TYPE = CustomPacketPayload.Type<UploadRemoteScriptPayload>(kursive("upload_remote_script"))
        val STREAM_CODEC = RemoteScriptContents.STREAM_CODEC.map(::UploadRemoteScriptPayload, UploadRemoteScriptPayload::contents)

        fun from(definition: ScriptDefinition<MinecraftServer>, name: String? = null): UploadRemoteScriptPayload {
            return UploadRemoteScriptPayload(RemoteScriptContents.from(definition, name))
        }
    }
}