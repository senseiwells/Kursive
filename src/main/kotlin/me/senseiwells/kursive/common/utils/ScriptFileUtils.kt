package me.senseiwells.kursive.common.utils

object ScriptFileUtils {
    const val SCRIPTS_DIRECTORY = "scripts"
    const val SUFFIX = ".main.kts"

    fun suffixate(name: String): String {
        return "$name$SUFFIX"
    }
}