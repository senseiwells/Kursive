package me.senseiwells.scripting.common.script.execution

import kotlinx.coroutines.Job
import me.senseiwells.scripting.api.ScriptContext
import me.senseiwells.scripting.common.script.configuration.MappingType
import net.fabricmc.api.EnvType
import java.util.concurrent.Executor
import kotlin.reflect.KClass

abstract class ExecutionEnvironment<M: Any>(
    val minecraft: M,
    val args: List<String>
) {
    abstract val type: EnvType

    abstract fun invoke(entrypoint: ScriptEntrypoint<M>, mappings: MappingType): Job

    abstract fun executor(): Executor

    abstract fun minecraftType(): KClass<M>

    abstract fun contextType(): KClass<out ScriptContext>
}