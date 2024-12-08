package me.senseiwells.scripting.impl

import me.senseiwells.scripting.ScriptingCoroutines
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
object ClientScriptingApi: ClientModInitializer {
    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ScriptingCoroutines::tick)
    }
}