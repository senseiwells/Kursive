package me.senseiwells.scripting.impl

import me.senseiwells.scripting.ScriptingCoroutines
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
object CommonScriptingApi: ModInitializer {
    override fun onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(ScriptingCoroutines::tick)
    }
}