package me.senseiwells.kursive.common.utils

import java.nio.file.InvalidPathException
import java.nio.file.Path

fun Path.resolveConfined(path: String): Path {
    val base = this.toAbsolutePath().normalize()
    val name = Path.of(path).fileName?.toString()
        ?: throw InvalidPathException(path, "Invalid filename")
    val resolved = base.resolve(name).normalize()
    if (!resolved.startsWith(base)) {
        throw IllegalArgumentException("Path '$path' is not confined")
    }
    return resolved
}