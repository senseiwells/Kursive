package me.senseiwells.scripting.common.utils

import me.senseiwells.scripting.common.script.execution.EnvironmentContext
import me.senseiwells.scripting.common.script.instance.ScriptInstance
import net.minecraft.Util
import java.util.concurrent.CompletableFuture
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.plus
import kotlin.script.experimental.jvm.util.isError

fun <M: Any> ScriptInstance<M>.compileAsyncThenExecute(
    context: EnvironmentContext<M>
): CompletableFuture<ResultWithDiagnostics<Unit>> {
    if (this.shouldRecompile()) {
        return CompletableFuture.supplyAsync({
            this.compile().reports + this.prepare(context)
        }, Util.ioPool()).thenApplyAsync({ result ->
            if (!result.isError()) result.reports + this.execute(context) else result
        }, context.executor())
    }
    return CompletableFuture.completedFuture(this.execute(context))
}