package me.senseiwells.scripting.common.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import me.senseiwells.scripting.common.script.execution.ExecutionEnvironment
import me.senseiwells.scripting.common.script.instance.ScriptInstance
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvm.util.isIncomplete

suspend fun <M: Any> ScriptInstance<M>.compileAndExecute(
    context: ExecutionEnvironment<M>
): ResultWithDiagnostics<Unit> = coroutineScope cs@ {
    if (shouldRecompile()) {
        val result = withContext(Dispatchers.Default) {
            compile().onSuccess { prepare(context) }
        }
        if (result.isError() || result.isIncomplete()) {
            return@cs result
        }
        return@cs result.onSuccess { execute(context) }
    }
    return@cs execute(context)
}