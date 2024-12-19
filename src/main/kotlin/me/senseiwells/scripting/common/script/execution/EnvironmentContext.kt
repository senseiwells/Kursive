package me.senseiwells.scripting.common.script.execution

import kotlinx.coroutines.Job
import me.senseiwells.scripting.api.ScriptContext
import me.senseiwells.scripting.common.script.configuration.MappingType
import me.senseiwells.scripting.common.utils.ScriptRemappingUtils
import me.senseiwells.scripting.impl.ClientScriptingApi
import me.senseiwells.scripting.impl.CommonScriptingApi
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.SimpleListenerRegistry
import net.fabricmc.api.EnvType
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
import java.util.concurrent.Executor
import kotlin.reflect.KClass

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

sealed class EnvironmentContext<M: Any>(
    val minecraft: M,
    val args: List<String>
) {
    abstract val type: EnvType

    abstract fun invoke(entrypoint: ScriptEntrypoint<M>, mappings: MappingType): Job

    abstract fun executor(): Executor

    abstract fun klass(): KClass<M>
}

private class ClientContext(client: Minecraft, args: List<String>): EnvironmentContext<Minecraft>(client, args) {
    override val type: EnvType
        get() = EnvType.CLIENT

    override fun invoke(entrypoint: ScriptEntrypoint<Minecraft>, mappings: MappingType): Job {
        val context = ScriptContext(
            this.args,
            SimpleListenerRegistry(),
            null,
            ScriptRemappingUtils.createScriptReflection(mappings),
        )
        GlobalEventHandler.Client.addProvider(context.events)
        val job = ClientScriptingApi.launch(this.minecraft) { entrypoint.invoke(this.minecraft, context) }
        job.invokeOnCompletion { GlobalEventHandler.Client.removeProvider(context.events) }
        return job
    }

    override fun executor(): Executor {
        return this.minecraft
    }

    override fun klass(): KClass<Minecraft> {
        return Minecraft::class
    }
}

private class ServerContext(server: MinecraftServer, args: List<String>): EnvironmentContext<MinecraftServer>(server, args) {
    override val type: EnvType
        get() = EnvType.SERVER

    override fun invoke(entrypoint: ScriptEntrypoint<MinecraftServer>, mappings: MappingType): Job {
        val context = ScriptContext(
            this.args,
            SimpleListenerRegistry(),
            null,
            ScriptRemappingUtils.createScriptReflection(mappings)
        )
        GlobalEventHandler.Server.addProvider(context.events)
        val job = CommonScriptingApi.launch(this.minecraft) { entrypoint.invoke(this.minecraft, context) }
        job.invokeOnCompletion { GlobalEventHandler.Server.removeProvider(context.events) }
        return job
    }

    override fun executor(): Executor {
        return this.minecraft
    }

    override fun klass(): KClass<MinecraftServer> {
        return MinecraftServer::class
    }
}