package me.senseiwells.kursive.api.utils

enum class ScriptType {
    Client, Server, Common;

    fun compatible(other: ScriptType): Boolean {
        return this == Common || this == other
    }
}