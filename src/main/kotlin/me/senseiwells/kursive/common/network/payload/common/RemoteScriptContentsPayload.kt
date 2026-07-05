package me.senseiwells.kursive.common.network.payload.common

import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer

class RemoteScriptContentsPayload(
    val name: String,
    val contents: ByteArray
): CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    companion object {
        const val MAX_SIZE_BYTES = 1024 * 1024 * 1024
        val TYPE = CustomPacketPayload.Type<RemoteScriptContentsPayload>(kursive("remote_script_contents"))
        val STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RemoteScriptContentsPayload::name,
            ByteBufCodecs.BYTE_ARRAY, RemoteScriptContentsPayload::contents,
            ::RemoteScriptContentsPayload
        )

        fun from(definition: ScriptDefinition<MinecraftServer>, name: String? = null): RemoteScriptContentsPayload {
            val contents = definition.getSource().text.encodeToByteArray()
            return RemoteScriptContentsPayload(name ?: definition.name, contents)
        }
    }
}