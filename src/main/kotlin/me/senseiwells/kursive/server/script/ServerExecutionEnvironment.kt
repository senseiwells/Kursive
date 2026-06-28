package me.senseiwells.kursive.server.script

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import me.senseiwells.kursive.api.ServerScriptContext
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import me.senseiwells.kursive.common.script.data.FileBasedDataStores
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.server.KursiveServer
import net.casual.arcade.commands.manager.GlobalCommandManager
import net.casual.arcade.commands.manager.ServerCommandManager
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.SimpleListenerRegistry
import net.casual.arcade.events.server.ServerSaveEvent
import net.casual.arcade.utils.coroutine.async
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

    override fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return this.minecraft.async(block)
    }

    override fun minecraftType(): KClass<MinecraftServer> {
        return MinecraftServer::class
    }

    override fun contextType(): KClass<ServerScriptContext> {
        return ServerScriptContext::class
    }

    override fun createContext(metadata: ScriptMetadata): ServerScriptContext {
        val events = SimpleListenerRegistry()
        val commands = ServerCommandManager(this.minecraft)
        val stores = FileBasedDataStores(
            KursiveServer.directory(this.minecraft).resolve("data"), metadata.id, this.minecraft::registryAccess
        )
        return ServerScriptContext(this.args, stores, events, commands)
    }

    override fun initializeContext(context: ServerScriptContext) {
        GlobalEventHandler.Server.addProvider(context.events)
        GlobalCommandManager.addManager(context.commands as ServerCommandManager)

        context.events.register<ServerSaveEvent> { event ->
            if (event.isRoutine) {
                (context.stores as FileBasedDataStores).write()
            }
        }
    }

    override fun cleanupContext(context: ServerScriptContext) {
        GlobalEventHandler.Server.removeProvider(context.events)
        GlobalCommandManager.removeManager(context.commands as ServerCommandManager)
        (context.stores as FileBasedDataStores).write()
    }
}