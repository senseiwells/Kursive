package me.senseiwells.essential_scripting.script.execution

import kotlinx.coroutines.Job
import me.senseiwells.scripting.ScriptingContext
import me.senseiwells.scripting.impl.ClientScriptingApi
import me.senseiwells.scripting.impl.CommonScriptingApi
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.SimpleListenerRegistry
import net.fabricmc.api.EnvType
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer

fun EnvironmentContext(
    client: Minecraft,
    args: List<String> = listOf()
): EnvironmentContext<Minecraft> {
    return ClientContext(client, args)
}

fun EnvironmentContext(
    server: MinecraftServer,
    args: List<String> = listOf()
): EnvironmentContext<MinecraftServer> {
    return ServerContext(server, args)
}

sealed class EnvironmentContext<M>(
    val minecraft: M & Any,
    val args: List<String>
) {
    abstract val type: EnvType

    abstract fun invoke(entrypoint: ScriptEntrypoint<M>): Job
}

private class ClientContext(client: Minecraft, args: List<String>): EnvironmentContext<Minecraft>(client, args) {
    override val type: EnvType
        get() = EnvType.CLIENT

    override fun invoke(entrypoint: ScriptEntrypoint<Minecraft>): Job {
        val context = ScriptingContext(this.args, SimpleListenerRegistry())
        GlobalEventHandler.Client.addProvider(context.events)
        val job = ClientScriptingApi.launch(this.minecraft) { entrypoint.invoke(this.minecraft, context) }
        job.invokeOnCompletion { GlobalEventHandler.Client.removeProvider(context.events) }
        return job
    }
}

private class ServerContext(server: MinecraftServer, args: List<String>): EnvironmentContext<MinecraftServer>(server, args) {
    override val type: EnvType
        get() = EnvType.SERVER

    override fun invoke(entrypoint: ScriptEntrypoint<MinecraftServer>): Job {
        val context = ScriptingContext(this.args, SimpleListenerRegistry())
        GlobalEventHandler.Server.addProvider(context.events)
        val job = CommonScriptingApi.launch(this.minecraft) { entrypoint.invoke(this.minecraft, context) }
        job.invokeOnCompletion { GlobalEventHandler.Server.removeProvider(context.events) }
        return job
    }
}