package me.senseiwells.kursive.common.script.instance.sync

import me.senseiwells.kursive.common.script.instance.ScriptInstance

fun interface ScriptSynchronizer<M: Any> {
    fun sync(script: ScriptInstance<M>)
}