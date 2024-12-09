package me.senseiwells.scripting.impl

import kotlinx.coroutines.Job
import me.senseiwells.scripting.ScriptingCoroutines
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
object ClientScriptingApi: ClientModInitializer {
    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ScriptingCoroutines::tick)
        ClientLifecycleEvents.CLIENT_STOPPING.register(ScriptingCoroutines::destroy)
    }

    fun launch(client: Minecraft, block: suspend () -> Unit): Job {
        return ScriptingCoroutines.launch(client, block)
    }
}