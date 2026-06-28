package me.senseiwells.kursive.common.script.definition.resolver

import me.senseiwells.kursive.common.script.definition.ScriptDefinition

interface ScriptDefinitionSource<M: Any> {
    fun initialize()

    fun get(): Collection<ScriptDefinition<M>>

    fun close()
}