package me.senseiwells.scripting.common.script.definition

import me.senseiwells.scripting.common.script.instance.ScriptInstance

interface ScriptDefinition<M: Any> {
    fun create(): ScriptInstance<M>

    fun delete(instance: ScriptInstance<M>)

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}