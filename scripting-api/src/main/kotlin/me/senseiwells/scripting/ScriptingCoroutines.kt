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

private val scopes = Reference2ObjectOpenHashMap<Any, CoroutineScope>()
private val delays = Reference2ObjectOpenHashMap<Any, Int2ObjectOpenHashMap<Queue<CompletableDeferred<Unit>>>>()
private val ticks = Reference2IntOpenHashMap<Any>()

fun launch(client: Minecraft, block: suspend () -> Unit) {
    val scope = scopes.getOrPut(client) {
        CoroutineScope(client.asCoroutineDispatcher()) + MinecraftContext(client)
    }
    scope.launch { block.invoke() }
}

fun launch(server: MinecraftServer, block: suspend () -> Unit) {
    val scope = scopes.getOrPut(server) {
        CoroutineScope(server.asCoroutineDispatcher() + MinecraftContext(server))
    }
    scope.launch { block.invoke() }
}

suspend fun tickDelay(duration: Int) = coroutineScope {
    val context = coroutineContext[MinecraftContext]
        ?: throw IllegalStateException("Cannot run tickDelay on non-minecraft coroutine")
    if (duration > 0) {
        val minecraft = context.minecraft
        val delays = delays.getOrPut(minecraft, ::Int2ObjectOpenHashMap)
        val queue = delays.getOrPut(ticks.getInt(minecraft) + duration) { ArrayDeque(1) }
        val deferred = CompletableDeferred<Unit>()
        queue.add(deferred)
        deferred.await()
    }
}

internal object ScriptingCoroutines {
    fun tick(client: Minecraft) {
        this.tick(client as Any)
    }

    fun tick(server: MinecraftServer) {
        this.tick(server as Any)
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
