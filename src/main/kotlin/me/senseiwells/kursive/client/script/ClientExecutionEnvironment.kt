package me.senseiwells.kursive.client.script

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.senseiwells.kursive.api.ClientScriptContext
import me.senseiwells.kursive.api.ScriptContext
import me.senseiwells.kursive.client.script.keybinds.ScriptKeybindManager
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.execution.ScriptEntrypoint
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.SimpleListenerRegistry
import net.casual.arcade.utils.coroutine.getCoroutineScope
import net.casual.arcade.utils.coroutine.launch
import net.fabricmc.api.EnvType
import net.minecraft.client.Minecraft
import kotlin.reflect.KClass

class ClientExecutionEnvironment(
    client: Minecraft,
    args: List<String>
): ExecutionEnvironment<Minecraft>(client, args) {
    override val type: EnvType
        get() = EnvType.CLIENT

    override fun invoke(entrypoint: ScriptEntrypoint<Minecraft>): Job {
        val context = this.createContext()
        this.initializeContext(context)
        val job = this.minecraft.getCoroutineScope().launch { entrypoint.invoke(minecraft, context) }
        job.invokeOnCompletion { this.cleanupContext(context) }
        return job
    }

    override fun launch(block: suspend CoroutineScope.() -> Unit) {
        this.minecraft.launch(block)
    }

    override fun minecraftType(): KClass<Minecraft> {
        return Minecraft::class
    }

    override fun contextType(): KClass<out ScriptContext> {
        return ClientScriptContext::class
    }

    private fun createContext(): ClientScriptContext {
        val events = SimpleListenerRegistry()
        return ClientScriptContext(this.args, events, ScriptKeybindManager())
    }

    private fun initializeContext(context: ClientScriptContext) {
        GlobalEventHandler.Client.addProvider(context.events)
    }

    private fun cleanupContext(context: ClientScriptContext) {
        GlobalEventHandler.Client.removeProvider(context.events)
        (context.keybinds as ScriptKeybindManager).unregister()
    }
}