package me.senseiwells.scripting

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
import kotlin.coroutines.CoroutineContext

private val scopes = Reference2ObjectOpenHashMap<Any, CoroutineScope>()
private val channels = Reference2ObjectOpenHashMap<Any, Channel<Unit>>()

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

suspend fun tickDelay(ticks: Int) = coroutineScope {
    val context = coroutineContext[MinecraftContext]
        ?: throw IllegalStateException("Cannot run tickDelay on non-minecraft coroutine")
    if (ticks > 0) {
        val channel = channels.getOrPut(context.minecraft, ::Channel)
        repeat(ticks) { channel.receive() }
    }
}

internal fun tick(client: Minecraft) {
    val channel = channels.getOrPut(client, ::Channel)
    launch(client) { channel.send(Unit) }
}

internal fun tick(server: MinecraftServer) {
    val channel = channels.getOrPut(server, ::Channel)
    launch(server) { channel.send(Unit) }
}

private class MinecraftContext(val minecraft: Any): CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = Key

    companion object Key: CoroutineContext.Key<MinecraftContext>
}
