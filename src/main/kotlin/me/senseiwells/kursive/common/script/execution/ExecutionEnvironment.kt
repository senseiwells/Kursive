package me.senseiwells.kursive.common.script.execution

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import me.senseiwells.kursive.api.ScriptContext
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import net.fabricmc.api.EnvType
import kotlin.reflect.KClass

abstract class ExecutionEnvironment<M: Any, C: ScriptContext>(
    val minecraft: M,
    val args: List<String>
) {
    abstract val type: EnvType

    fun invoke(entrypoint: ScriptEntrypoint<M>, metadata: ScriptMetadata): Job {
        val context = this.createContext(metadata)
        this.initializeContext(context)
        return this.launch {
            try {
                entrypoint.invoke(minecraft, context)
            } finally {
                cleanupContext(context)
            }
        }
    }

    abstract fun launch(block: suspend CoroutineScope.() -> Unit): Job

    abstract fun minecraftType(): KClass<M>

    abstract fun contextType(): KClass<C>

    protected abstract fun createContext(metadata: ScriptMetadata): C

    protected abstract fun initializeContext(context: C)

    protected abstract fun cleanupContext(context: C)
}