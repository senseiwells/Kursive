package me.senseiwells.scripting.common.script.execution

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import me.senseiwells.scripting.api.ScriptContext
import net.fabricmc.api.EnvType
import kotlin.reflect.KClass

abstract class ExecutionEnvironment<M: Any>(
    val minecraft: M,
    val args: List<String>
) {
    abstract val type: EnvType

    abstract fun invoke(entrypoint: ScriptEntrypoint<M>): Job

    abstract fun launch(block: suspend CoroutineScope.() -> Unit)

    abstract fun minecraftType(): KClass<M>

    abstract fun contextType(): KClass<out ScriptContext>
}