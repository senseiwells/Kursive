package me.senseiwells.scripting.script.execution

import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer

sealed class EnvironmentContext<M>(val minecraft: M & Any) {
    abstract val isClient: Boolean
}

class ClientContext(client: Minecraft): EnvironmentContext<Minecraft>(client) {
    override val isClient: Boolean
        get() = true
}

class ServerContext(server: MinecraftServer): EnvironmentContext<MinecraftServer>(server) {
    override val isClient: Boolean
        get() = false
}