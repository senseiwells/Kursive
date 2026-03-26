package me.senseiwells.scripting.api

import net.casual.arcade.events.ListenerRegistry
import net.minecraft.commands.CommandSourceStack

sealed class ScriptContext(
    val args: List<String>,
    val events: ListenerRegistry
)

class ServerScriptContext(
    args: List<String>,
    events: ListenerRegistry,
    val commands: ScriptCommands<CommandSourceStack>
): ScriptContext(args, events)

class ClientScriptContext(
    args: List<String>,
    events: ListenerRegistry
): ScriptContext(args, events)