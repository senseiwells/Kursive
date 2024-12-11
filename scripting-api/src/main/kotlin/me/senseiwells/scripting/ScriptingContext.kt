package me.senseiwells.scripting

import net.casual.arcade.events.ListenerRegistry

data class ScriptingContext(
    val args: List<String>,
    val events: ListenerRegistry
)