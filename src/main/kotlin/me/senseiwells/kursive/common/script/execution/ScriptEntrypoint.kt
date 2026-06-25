package me.senseiwells.kursive.common.script.execution

import me.senseiwells.kursive.api.ScriptContext

fun interface ScriptEntrypoint<M> {
    suspend fun invoke(minecraft: M, context: ScriptContext)
}