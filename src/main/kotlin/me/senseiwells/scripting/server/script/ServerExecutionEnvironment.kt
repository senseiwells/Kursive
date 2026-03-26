package me.senseiwells.scripting.server.script

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.senseiwells.scripting.api.ScriptContext
import me.senseiwells.scripting.api.ServerScriptContext
import me.senseiwells.scripting.common.script.execution.ExecutionEnvironment
import me.senseiwells.scripting.common.script.execution.ScriptEntrypoint
import net.casual.arcade.commands.manager.GlobalCommandManager
import net.casual.arcade.commands.manager.ServerCommandManager
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.SimpleListenerRegistry
import net.casual.arcade.utils.coroutine.getCoroutineScope
import net.casual.arcade.utils.coroutine.launch
import net.fabricmc.api.EnvType
import net.minecraft.server.MinecraftServer
import kotlin.reflect.KClass

class ServerExecutionEnvironment(
    server: MinecraftServer,
    args: List<String>
): ExecutionEnvironment<MinecraftServer>(server, args) {
    override val type: EnvType
        get() = EnvType.SERVER

    override fun invoke(entrypoint: ScriptEntrypoint<MinecraftServer>): Job {
        val context = this.createContext()
        this.initializeContext(context)
        val job = this.minecraft.getCoroutineScope().launch { entrypoint.invoke(minecraft, context) }
        job.invokeOnCompletion { this.cleanupContext(context) }
        return job
    }

    override fun launch(block: suspend CoroutineScope.() -> Unit) {
        this.minecraft.launch(block)
    }

    override fun minecraftType(): KClass<MinecraftServer> {
        return MinecraftServer::class
    }

    override fun contextType(): KClass<out ScriptContext> {
        return ServerScriptContext::class
    }

    private fun createContext(): ServerScriptContext {
        val events = SimpleListenerRegistry()
        val commands = ServerCommandManager(this.minecraft)
        return ServerScriptContext(this.args, events, commands)
    }

    private fun initializeContext(context: ServerScriptContext) {
        GlobalEventHandler.Server.addProvider(context.events)
        GlobalCommandManager.addManager(context.commands as ServerCommandManager)
    }

    private fun cleanupContext(context: ServerScriptContext) {
        GlobalEventHandler.Server.removeProvider(context.events)
        GlobalCommandManager.removeManager(context.commands as ServerCommandManager)
    }
}