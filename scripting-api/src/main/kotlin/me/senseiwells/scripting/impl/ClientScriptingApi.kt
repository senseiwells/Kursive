package me.senseiwells.scripting.impl

import kotlinx.coroutines.Job
import me.senseiwells.scripting.api.ScriptingCoroutines
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.client.ClientStoppingEvent
import net.casual.arcade.events.client.ClientTickEvent
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.Minecraft
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
object ClientScriptingApi: ClientModInitializer {
    override fun onInitializeClient() {
        GlobalEventHandler.Client.register<ClientTickEvent>(
            phase = ClientTickEvent.PHASE_POST
        ) { ScriptingCoroutines.tick(it.client) }
        GlobalEventHandler.Client.register<ClientStoppingEvent>(
            phase = ClientTickEvent.PHASE_POST
        ) { ScriptingCoroutines.destroy(it.client) }
    }

    fun launch(client: Minecraft, block: suspend () -> Unit): Job {
        return ScriptingCoroutines.launch(client, block)
    }
}