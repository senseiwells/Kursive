package me.senseiwells.scripting.script.execution

fun interface ScriptEntrypoint<M> {
    suspend fun invoke(minecraft: M, args: Array<String>)
}