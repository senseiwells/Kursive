package me.senseiwells.scripting.api

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
import java.util.*
import kotlin.coroutines.CoroutineContext

suspend fun tickDelay(ticks: Int) = coroutineScope {
    val context = coroutineContext[MinecraftContext]
        ?: throw IllegalStateException("Cannot run tickDelay on non-minecraft coroutine")
    if (ticks > 0) {
        val minecraft = context.minecraft
        val delays = ScriptingCoroutines.delays.getOrPut(minecraft, ::Int2ObjectOpenHashMap)
        val time = ScriptingCoroutines.ticks.getInt(minecraft) + ticks
        val queue = delays.getOrPut(time) { ArrayDeque(1) }
        val deferred = CompletableDeferred<Unit>()
        queue.add(deferred)
        deferred.await()
    }
}

internal object ScriptingCoroutines {
    val delays = Reference2ObjectOpenHashMap<Any, Int2ObjectOpenHashMap<Queue<CompletableDeferred<Unit>>>>()
    val ticks = Reference2IntOpenHashMap<Any>()
    private val scopes = Reference2ObjectOpenHashMap<Any, CoroutineScope>()

    fun launch(client: Minecraft, block: suspend () -> Unit): Job {
        val scope = scopes.getOrPut(client) {
            CoroutineScope(client.asCoroutineDispatcher() + MinecraftContext(client) + SupervisorJob())
        }
        return scope.launch { block.invoke() }
    }

    fun launch(server: MinecraftServer, block: suspend () -> Unit): Job {
        val scope = scopes.getOrPut(server) {
            CoroutineScope(server.asCoroutineDispatcher() + MinecraftContext(server) + SupervisorJob())
        }
        return scope.launch { block.invoke() }
    }

    fun tick(client: Minecraft) {
        tick(client as Any)
    }

    fun tick(server: MinecraftServer) {
        tick(server as Any)
    }

    fun destroy(client: Minecraft) {
        scopes[client]?.cancel()
    }

    fun destroy(server: MinecraftServer) {
        scopes[server]?.cancel()
    }

    private fun tick(any: Any) {
        val delays = delays[any] ?: return
        val tick = ticks.getInt(any)
        ticks.put(any, tick + 1)
        val queue = delays.remove(tick) ?: return
        for (deferred in queue) {
            deferred.complete(Unit)
        }
    }
}

private class MinecraftContext(val minecraft: Any): CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = Key

    companion object Key: CoroutineContext.Key<MinecraftContext>
}
