package me.senseiwells.kursive.api

import me.senseiwells.kursive.api.data.PersistentDataStores
import me.senseiwells.kursive.api.keybind.KeybindRegistry
import net.casual.arcade.commands.manager.CommandRegistry
import net.casual.arcade.events.ListenerRegistry
import net.casual.arcade.events.common.ClientSideEvent
import net.casual.arcade.events.common.ServerSideEvent
import net.minecraft.commands.CommandSourceStack

sealed class ScriptContext(
    val args: List<String>,
    val stores: PersistentDataStores
) {
    abstract val events: ListenerRegistry<*>
}

class ServerScriptContext(
    args: List<String>,
    stores: PersistentDataStores,
    override val events: ListenerRegistry<ServerSideEvent>,
    val commands: CommandRegistry<CommandSourceStack>
): ScriptContext(args, stores)

class ClientScriptContext(
    args: List<String>,
    stores: PersistentDataStores,
    override val events: ListenerRegistry<ClientSideEvent>,
    val keybinds: KeybindRegistry
): ScriptContext(args, stores)