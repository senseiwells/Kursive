package me.senseiwells.scripting.api

import net.casual.arcade.events.ListenerRegistry
import net.minecraft.commands.CommandSourceStack

sealed class ScriptContext(
    val args: List<String>,
    val events: ListenerRegistry,
    val reflection: ScriptReflection
)

class ServerScriptContext(
    args: List<String>,
    events: ListenerRegistry,
    reflection: ScriptReflection,
    val commands: ScriptCommands<CommandSourceStack>
): ScriptContext(args, events, reflection)

class ClientScriptContext(
    args: List<String>,
    events: ListenerRegistry,
    reflection: ScriptReflection
): ScriptContext(args, events, reflection)