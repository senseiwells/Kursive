package me.senseiwells.kursive.common.permissions

import net.casual.arcade.utils.component.red
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner
import net.minecraft.network.chat.Component

@Suppress("UnstableApiUsage")
class KursivePermission(
    val id: String,
    val fallback: (PermissionContextOwner) -> Boolean
) {
    fun message(): Component {
        return Component.translatable("kursive.permission.${this.id}.message").red()
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is KursivePermission && this.id == other.id)
    }
}