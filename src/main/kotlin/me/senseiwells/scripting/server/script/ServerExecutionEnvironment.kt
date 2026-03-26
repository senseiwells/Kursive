package me.senseiwells.scripting.server.script

import kotlinx.coroutines.Job
import me.senseiwells.scripting.api.ScriptContext
import me.senseiwells.scripting.api.ServerScriptContext
import me.senseiwells.scripting.common.script.execution.ExecutionEnvironment
import me.senseiwells.scripting.common.script.execution.ScriptEntrypoint
import me.senseiwells.scripting.impl.CommonScriptingApi
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.SimpleListenerRegistry
import net.fabricmc.api.EnvType
import net.minecraft.server.MinecraftServer
import java.util.concurrent.Executor
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
        val job = CommonScriptingApi.launch(this.minecraft) { entrypoint.invoke(this.minecraft, context) }
        job.invokeOnCompletion { this.cleanupContext(context) }
        return job
    }

    override fun executor(): Executor {
        return this.minecraft
    }

    override fun minecraftType(): KClass<MinecraftServer> {
        return MinecraftServer::class
    }

    override fun contextType(): KClass<out ScriptContext> {
        return ServerScriptContext::class
    }

    private fun createContext(): ServerScriptContext {
        val events = SimpleListenerRegistry()
        val commands = ServerScriptCommands(this.minecraft)
        return ServerScriptContext(this.args, events, commands)
    }

    private fun initializeContext(context: ServerScriptContext) {
        GlobalEventHandler.Server.addProvider(context.events)
        (context.commands as ServerScriptCommands).initialize(context.events)
    }

    private fun cleanupContext(context: ServerScriptContext) {
        GlobalEventHandler.Server.removeProvider(context.events)
        (context.commands as ServerScriptCommands).cleanup()
    }
}