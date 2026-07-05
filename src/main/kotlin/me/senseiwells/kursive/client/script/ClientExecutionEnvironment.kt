package me.senseiwells.kursive.client.script

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import me.senseiwells.kursive.api.ClientScriptContext
import me.senseiwells.kursive.api.utils.ScriptType
import me.senseiwells.kursive.client.KursiveClient
import me.senseiwells.kursive.client.script.keybinds.ScriptKeybindManager
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import me.senseiwells.kursive.common.script.data.FileBasedDataStores
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.SimpleListenerRegistry
import net.casual.arcade.utils.coroutine.async
import net.casual.arcade.utils.coroutine.launch
import net.minecraft.client.Minecraft
import kotlin.reflect.KClass

class ClientExecutionEnvironment(
    client: Minecraft,
    args: List<String>
): ExecutionEnvironment<Minecraft, ClientScriptContext>(client, args) {
    override val type: ScriptType
        get() = ScriptType.Client

    override fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return this.minecraft.launch(block)
    }

    override fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return this.minecraft.async(block)
    }

    override fun minecraftType(): KClass<Minecraft> {
        return Minecraft::class
    }

    override fun contextType(): KClass<ClientScriptContext> {
        return ClientScriptContext::class
    }

    override fun createContext(metadata: ScriptMetadata): ClientScriptContext {
        val events = SimpleListenerRegistry()
        val stores = FileBasedDataStores(KursiveClient.directory().resolve("data"), metadata.id) {
            this.minecraft.level?.registryAccess()
        }
        return ClientScriptContext(this.args, stores, events, ScriptKeybindManager())
    }

    override fun initializeContext(context: ClientScriptContext) {
        GlobalEventHandler.Client.addProvider(context.events)
    }

    override fun cleanupContext(context: ClientScriptContext) {
        GlobalEventHandler.Client.removeProvider(context.events)
        (context.keybinds as ScriptKeybindManager).unregister()
        (context.stores as FileBasedDataStores).write()
    }
}