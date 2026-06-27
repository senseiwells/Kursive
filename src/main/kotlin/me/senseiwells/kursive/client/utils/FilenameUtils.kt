package me.senseiwells.kursive.client.utils

object FilenameUtils {
    private val RESERVED_NAMES = setOf(
        "con", "prn", "aux", "nul",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
        "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    )

    fun isValidFilename(name: String): Boolean {
        return when {
            name.isBlank() -> false
            name.length > 255 -> false
            name.trimEnd('.', ' ') != name -> false
            name.lowercase() in RESERVED_NAMES -> false
            else -> name.none { it < ' ' || it in """\/:*?"<>|""" }
        }
    }
}