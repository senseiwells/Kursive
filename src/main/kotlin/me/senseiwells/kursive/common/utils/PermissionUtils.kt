package me.senseiwells.kursive.common.utils

import me.senseiwells.kursive.common.permissions.KursivePermission
import me.senseiwells.kursive.server.KursiveServer
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner

@Suppress("UnstableApiUsage")
fun PermissionContextOwner.checkPermission(permission: KursivePermission): Boolean {
    if (!KursiveServer.config.requirePermissions) {
        return true
    }

    return this.checkPermission(kursive(permission.id)).orElseGet {
        permission.fallback.invoke(this)
    }
}