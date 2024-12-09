package me.senseiwells.scripting.impl

import kotlinx.coroutines.Job
import me.senseiwells.scripting.ScriptingCoroutines
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
object CommonScriptingApi: ModInitializer {
    override fun onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(ScriptingCoroutines::tick)
        ServerLifecycleEvents.SERVER_STOPPING.register(ScriptingCoroutines::destroy)
    }

    fun launch(server: MinecraftServer, block: suspend () -> Unit): Job {
        return ScriptingCoroutines.launch(server, block)
    }
}