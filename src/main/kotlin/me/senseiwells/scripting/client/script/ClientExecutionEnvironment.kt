package me.senseiwells.scripting.client.script

import kotlinx.coroutines.Job
import me.senseiwells.scripting.api.ClientScriptContext
import me.senseiwells.scripting.api.ScriptContext
import me.senseiwells.scripting.common.script.configuration.MappingType
import me.senseiwells.scripting.common.script.execution.ExecutionEnvironment
import me.senseiwells.scripting.common.script.execution.ScriptEntrypoint
import me.senseiwells.scripting.common.utils.ScriptRemappingUtils
import me.senseiwells.scripting.impl.ClientScriptingApi
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.SimpleListenerRegistry
import net.fabricmc.api.EnvType
import net.minecraft.client.Minecraft
import java.util.concurrent.Executor
import kotlin.reflect.KClass

class ClientExecutionEnvironment(
    client: Minecraft,
    args: List<String>
): ExecutionEnvironment<Minecraft>(client, args) {
    override val type: EnvType
        get() = EnvType.CLIENT

    override fun invoke(entrypoint: ScriptEntrypoint<Minecraft>, mappings: MappingType): Job {
        val context = this.createContext(mappings)
        this.initializeContext(context)
        val job = ClientScriptingApi.launch(this.minecraft) { entrypoint.invoke(this.minecraft, context) }
        job.invokeOnCompletion { this.cleanupContext(context) }
        return job
    }

    override fun executor(): Executor {
        return this.minecraft
    }

    override fun minecraftType(): KClass<Minecraft> {
        return Minecraft::class
    }

    override fun contextType(): KClass<out ScriptContext> {
        return ClientScriptContext::class
    }

    private fun createContext(mappings: MappingType): ClientScriptContext {
        val events = SimpleListenerRegistry()
        val reflection = ScriptRemappingUtils.createScriptReflection(mappings)
        return ClientScriptContext(this.args, events, reflection)
    }

    private fun initializeContext(context: ClientScriptContext) {
        GlobalEventHandler.Client.addProvider(context.events)
    }

    private fun cleanupContext(context: ClientScriptContext) {
        GlobalEventHandler.Client.removeProvider(context.events)
    }
}