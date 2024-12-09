package me.senseiwells.scripting

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
import java.util.ArrayDeque
import java.util.Queue
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
        val scope = this.scopes.getOrPut(client) {
            CoroutineScope(client.asCoroutineDispatcher() + MinecraftContext(client) + SupervisorJob())
        }
        return scope.launch { block.invoke() }
    }

    fun launch(server: MinecraftServer, block: suspend () -> Unit): Job {
        val scope = this.scopes.getOrPut(server) {
            CoroutineScope(server.asCoroutineDispatcher() + MinecraftContext(server) + SupervisorJob())
        }
        return scope.launch { block.invoke() }
    }

    fun tick(client: Minecraft) {
        this.tick(client as Any)
    }

    fun tick(server: MinecraftServer) {
        this.tick(server as Any)
    }

    fun destroy(client: Minecraft) {
        this.scopes[client]?.cancel()
    }

    fun destroy(server: MinecraftServer) {
        this.scopes[server]?.cancel()
    }

    private fun tick(any: Any) {
        val delays = this.delays[any] ?: return
        val tick = this.ticks.getInt(any)
        this.ticks.put(any, tick + 1)
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
