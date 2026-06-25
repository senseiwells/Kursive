package me.senseiwells.kursive.server.script

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import me.senseiwells.kursive.api.ServerScriptContext
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import net.casual.arcade.commands.manager.GlobalCommandManager
import net.casual.arcade.commands.manager.ServerCommandManager
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.SimpleListenerRegistry
import net.casual.arcade.utils.coroutine.launch
import net.fabricmc.api.EnvType
import net.minecraft.server.MinecraftServer
import kotlin.reflect.KClass

class ServerExecutionEnvironment(
    server: MinecraftServer,
    args: List<String>
): ExecutionEnvironment<MinecraftServer, ServerScriptContext>(server, args) {
    override val type: EnvType
        get() = EnvType.SERVER

    override fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return this.minecraft.launch(block)
    }

    override fun minecraftType(): KClass<MinecraftServer> {
        return MinecraftServer::class
    }

    override fun contextType(): KClass<ServerScriptContext> {
        return ServerScriptContext::class
    }

    override fun createContext(): ServerScriptContext {
        val events = SimpleListenerRegistry()
        val commands = ServerCommandManager(this.minecraft)
        return ServerScriptContext(this.args, events, commands)
    }

    override fun initializeContext(context: ServerScriptContext) {
        GlobalEventHandler.Server.addProvider(context.events)
        GlobalCommandManager.addManager(context.commands as ServerCommandManager)
    }

    override fun cleanupContext(context: ServerScriptContext) {
        GlobalEventHandler.Server.removeProvider(context.events)
        GlobalCommandManager.removeManager(context.commands as ServerCommandManager)
    }
}