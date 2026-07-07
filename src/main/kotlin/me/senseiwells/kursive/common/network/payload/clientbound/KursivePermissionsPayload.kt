package me.senseiwells.kursive.common.network.payload.clientbound

import me.senseiwells.kursive.common.permissions.KursivePermission
import me.senseiwells.kursive.common.permissions.KursivePermissions
import me.senseiwells.kursive.common.utils.kursive
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

class KursivePermissionsPayload(
    val permissions: Set<KursivePermission>
): CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
        return TYPE
    }

    companion object {
        val TYPE = CustomPacketPayload.Type<KursivePermissionsPayload>(kursive("permissions"))
        val STREAM_CODEC = KursivePermissions.STREAM_CODEC.map(::KursivePermissionsPayload, KursivePermissionsPayload::permissions)

        fun from(player: ServerPlayer): KursivePermissionsPayload {
            return KursivePermissionsPayload(KursivePermissions.from(player))
        }
    }
}