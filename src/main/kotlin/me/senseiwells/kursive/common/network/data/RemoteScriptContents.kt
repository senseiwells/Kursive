package me.senseiwells.kursive.common.network.data

import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.MinecraftServer

class RemoteScriptContents(
    val name: String,
    val bytes: ByteArray
) {
    companion object {
        val STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RemoteScriptContents::name,
            ByteBufCodecs.BYTE_ARRAY, RemoteScriptContents::bytes,
            ::RemoteScriptContents
        )

        fun from(definition: ScriptDefinition<MinecraftServer>, name: String? = null): RemoteScriptContents {
            return RemoteScriptContents(name ?: definition.name, definition.getSource().text.encodeToByteArray())
        }
    }
}