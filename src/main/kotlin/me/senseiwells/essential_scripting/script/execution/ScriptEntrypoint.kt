package me.senseiwells.essential_scripting.script.execution

import me.senseiwells.scripting.api.ScriptContext

fun interface ScriptEntrypoint<M> {
    suspend fun invoke(minecraft: M, context: ScriptContext)
}