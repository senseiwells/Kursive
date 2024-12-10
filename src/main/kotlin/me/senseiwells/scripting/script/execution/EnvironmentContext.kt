package me.senseiwells.scripting.script.execution

import kotlinx.coroutines.Job
import me.senseiwells.scripting.impl.ClientScriptingApi
import me.senseiwells.scripting.impl.CommonScriptingApi
import net.fabricmc.api.EnvType
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer

fun EnvironmentContext(
    client: Minecraft,
    args: Array<String> = arrayOf()
): EnvironmentContext<Minecraft> {
    return ClientContext(client, args)
}

fun EnvironmentContext(
    server: MinecraftServer,
    args: Array<String> = arrayOf()
): EnvironmentContext<MinecraftServer> {
    return ServerContext(server, args)
}

sealed class EnvironmentContext<M>(
    val minecraft: M & Any,
    val args: Array<String>
) {
    abstract val type: EnvType

    abstract fun invoke(entrypoint: ScriptEntrypoint<M>): Job
}

private class ClientContext(client: Minecraft, args: Array<String>): EnvironmentContext<Minecraft>(client, args) {
    override val type: EnvType
        get() = EnvType.CLIENT

    override fun invoke(entrypoint: ScriptEntrypoint<Minecraft>): Job {
        return ClientScriptingApi.launch(this.minecraft) {
            entrypoint.invoke(this.minecraft, this.args)
        }
    }
}

private class ServerContext(server: MinecraftServer, args: Array<String>): EnvironmentContext<MinecraftServer>(server, args) {
    override val type: EnvType
        get() = EnvType.SERVER

    override fun invoke(entrypoint: ScriptEntrypoint<MinecraftServer>): Job {
        return CommonScriptingApi.launch(this.minecraft) {
            entrypoint.invoke(this.minecraft, this.args)
        }
    }
}