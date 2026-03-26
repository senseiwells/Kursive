package me.senseiwells.scripting.api

import net.casual.arcade.commands.manager.CommandRegistry
import net.casual.arcade.events.ListenerRegistry
import net.minecraft.commands.CommandSourceStack

sealed class ScriptContext(
    val args: List<String>,
    val events: ListenerRegistry
)

class ServerScriptContext(
    args: List<String>,
    events: ListenerRegistry,
    val commands: CommandRegistry<CommandSourceStack>
): ScriptContext(args, events)

class ClientScriptContext(
    args: List<String>,
    events: ListenerRegistry
): ScriptContext(args, events)