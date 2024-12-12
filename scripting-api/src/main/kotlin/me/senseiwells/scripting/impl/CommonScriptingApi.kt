package me.senseiwells.scripting.impl

import kotlinx.coroutines.Job
import me.senseiwells.scripting.ScriptingCoroutines
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.client.ClientTickEvent
import net.casual.arcade.events.server.ServerStoppingEvent
import net.casual.arcade.events.server.ServerTickEvent
import net.fabricmc.api.ModInitializer
import net.minecraft.server.MinecraftServer
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
object CommonScriptingApi: ModInitializer {
    const val MOD_ID = "essential-scripting-api"

    override fun onInitialize() {
        GlobalEventHandler.Client.register<ServerTickEvent>(
            phase = ClientTickEvent.PHASE_POST
        ) { ScriptingCoroutines.tick(it.server) }
        GlobalEventHandler.Client.register<ServerStoppingEvent>(
            phase = ClientTickEvent.PHASE_POST
        ) { ScriptingCoroutines.destroy(it.server) }
    }

    fun launch(server: MinecraftServer, block: suspend () -> Unit): Job {
        return ScriptingCoroutines.launch(server, block)
    }
}