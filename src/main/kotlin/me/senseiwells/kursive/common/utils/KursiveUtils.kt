package me.senseiwells.kursive.common.utils

import me.senseiwells.kursive.common.Kursive
import net.casual.arcade.utils.Identifier
import net.minecraft.resources.Identifier

fun kursive(path: String): Identifier {
    return Identifier(Kursive.MOD_ID, path)
}