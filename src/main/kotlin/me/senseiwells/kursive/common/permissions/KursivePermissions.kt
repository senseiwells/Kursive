package me.senseiwells.kursive.common.permissions

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.kursive.common.utils.checkPermission
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.PermissionLevel

@Suppress("UnstableApiUsage", "SameParameterValue")
object KursivePermissions {
    private val registered = Object2ObjectOpenHashMap<String, KursivePermission>()

    val ACCESS_REMOTE_SCRIPTS = register("remote.access", PermissionLevel.GAMEMASTERS)
    val RUN_REMOTE_SCRIPTS = register("remote.run", ACCESS_REMOTE_SCRIPTS)
    val COMPILE_REMOTE_SCRIPTS = register("remote.compile", RUN_REMOTE_SCRIPTS)
    val DOWNLOAD_REMOTE_SCRIPTS = register("remote.download", ACCESS_REMOTE_SCRIPTS)

    val UPLOAD_REMOTE_SCRIPTS = register("remote.upload")
    val CREATE_REMOTE_SCRIPTS = register("remote.create", UPLOAD_REMOTE_SCRIPTS)
    val DELETE_REMOTE_SCRIPTS = register("remote.delete", CREATE_REMOTE_SCRIPTS)

    val STREAM_CODEC = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())
        .map({ ids -> ids.mapNotNull { registered[it] }.toSet() }, { perms -> perms.map { it.id } })

    fun from(player: ServerPlayer): Set<KursivePermission> {
        return registered.values.filterTo(HashSet()) { permission -> player.checkPermission(permission) }
    }

    private fun register(id: String): KursivePermission {
        return this.register(KursivePermission(id) { false })
    }

    private fun register(id: String, level: PermissionLevel): KursivePermission {
        return this.register(KursivePermission(id) { owner ->
            owner.permissionContext.permissionLevel().isEqualOrHigherThan(level)
        })
    }

    private fun register(id: String, fallback: KursivePermission): KursivePermission {
        return this.register(KursivePermission(id) { owner -> owner.checkPermission(fallback) })
    }

    private fun register(permission: KursivePermission): KursivePermission {
        this.registered[permission.id] = permission
        return permission
    }
}