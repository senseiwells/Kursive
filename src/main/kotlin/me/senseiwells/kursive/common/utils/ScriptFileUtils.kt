package me.senseiwells.kursive.common.utils

object ScriptFileUtils {
    val SUFFIX = ".main.kts"

    fun suffixate(name: String): String {
        return "$name$SUFFIX"
    }
}