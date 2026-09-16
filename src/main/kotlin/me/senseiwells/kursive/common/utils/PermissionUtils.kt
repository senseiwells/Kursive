package me.senseiwells.kursive.common.utils

import me.senseiwells.kursive.common.permissions.KursivePermission
import me.senseiwells.kursive.server.KursiveServer
import net.casual.arcade.utils.player.hasPermission
import net.casual.arcade.utils.player.server
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.PermissionLevel

@Suppress("UnstableApiUsage")
fun PermissionContextOwner.checkPermission(permission: KursivePermission): Boolean {
    if (!KursiveServer.config.requirePermissions) {
        return true
    }

    if (this is ServerPlayer && this.server.isSingleplayer && this.hasPermission(PermissionLevel.GAMEMASTERS)) {
        return true
    }

    return this.checkPermission(kursive(permission.id)).orElseGet {
        permission.fallback.invoke(this)
    }
}