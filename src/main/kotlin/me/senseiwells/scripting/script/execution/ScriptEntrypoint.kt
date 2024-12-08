package me.senseiwells.scripting.script.execution

fun interface ScriptEntrypoint<M> {
    fun invoke(minecraft: M, args: Array<String>)
}