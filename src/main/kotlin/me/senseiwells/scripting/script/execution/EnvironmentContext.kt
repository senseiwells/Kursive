package me.senseiwells.scripting.script.execution

import kotlinx.coroutines.Job
import me.senseiwells.scripting.launch
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
    abstract val isClient: Boolean

    abstract fun invoke(entrypoint: ScriptEntrypoint<M>): Job
}

private class ClientContext(client: Minecraft, args: Array<String>): EnvironmentContext<Minecraft>(client, args) {
    override val isClient: Boolean
        get() = true

    override fun invoke(entrypoint: ScriptEntrypoint<Minecraft>): Job {
        return launch(this.minecraft) {
            entrypoint.invoke(this.minecraft, this.args)
        }
    }
}

private class ServerContext(server: MinecraftServer, args: Array<String>): EnvironmentContext<MinecraftServer>(server, args) {
    override val isClient: Boolean
        get() = false

    override fun invoke(entrypoint: ScriptEntrypoint<MinecraftServer>): Job {
        return launch(this.minecraft) {
            entrypoint.invoke(this.minecraft, this.args)
        }
    }
}