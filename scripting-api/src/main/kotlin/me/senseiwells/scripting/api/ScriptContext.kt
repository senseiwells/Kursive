package me.senseiwells.scripting.api

import net.casual.arcade.events.ListenerRegistry

data class ScriptContext(
    val args: List<String>,
    val events: ListenerRegistry,
    val commands: ScriptCommands?,
    val reflection: ScriptReflection
)