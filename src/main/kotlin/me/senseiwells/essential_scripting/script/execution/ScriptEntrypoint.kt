package me.senseiwells.essential_scripting.script.execution

import me.senseiwells.scripting.ScriptingContext

fun interface ScriptEntrypoint<M> {
    suspend fun invoke(minecraft: M, context: ScriptingContext)
}