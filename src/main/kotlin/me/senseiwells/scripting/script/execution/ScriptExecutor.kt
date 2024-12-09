package me.senseiwells.scripting.script.execution

import kotlinx.coroutines.*
import me.senseiwells.scripting.EssentialScripting
import me.senseiwells.scripting.EssentialScriptingConfig
import me.senseiwells.scripting.script.configuration.ScriptWithClassloaderEvaluationConfiguration
import me.senseiwells.scripting.script.configuration.ScriptWithClasspathCompilationConfiguration
import me.senseiwells.scripting.script.configuration.environment
import me.senseiwells.scripting.script.remapping.RemappedJvmScriptJarGenerator
import net.minecraft.Util
import net.minecraft.client.Minecraft
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createParentDirectories
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.loadScriptFromJar

object ScriptExecutor {
    private val scripts = ArrayList<Job>()

    fun cancelAllScripts() {
        scripts.forEach { it.cancel() }
    }

    fun <M> runScript(context: EnvironmentContext<M>, source: SourceCode) {
        CompletableFuture.supplyAsync({
            this.compileAndLoad(context, source)
        }, Util.ioPool()).handleAsync({ entrypoint, throwable ->
            // TODO: Clean this up
            if (entrypoint != null) {
                val job = context.invoke(entrypoint)
                scripts.add(job)
            } else {
                EssentialScripting.logger.error("Failed to run script", throwable)
            }
        }, Minecraft.getInstance())
    }

    private fun <M> compileAndLoad(
        context: EnvironmentContext<M>,
        source: SourceCode
    ): ScriptEntrypoint<M>? {
        // TODO:
        val output = if (source is FileBasedScriptSource) {
            EssentialScriptingConfig.resolve("compiled")
                .resolve("${source.file.nameWithoutExtension}.jar").createParentDirectories()
        } else {
            EssentialScriptingConfig.resolve("compiled-tmp")
                .resolve("${source.name}.jar").createParentDirectories()
        }


        val host = BasicJvmScriptingHost(evaluator = RemappedJvmScriptJarGenerator(output))


        val report = host.eval(
            source,
            ScriptWithClasspathCompilationConfiguration,
            ScriptWithClassloaderEvaluationConfiguration
        )
        // TODO: propagate errors properly
        if (report.isError()) {
            for (diag in report.reports) {
                if (diag.severity > ScriptDiagnostic.Severity.INFO) {
                    EssentialScripting.logger.error(diag.render())
                }
            }
            return null
        }
        for (diag in report.reports) {
            if (diag.severity > ScriptDiagnostic.Severity.INFO) {
                EssentialScripting.logger.warn(diag.render())
            }
        }

        // TODO: Split
        val script = output.toFile().loadScriptFromJar(false)
            ?: throw IllegalStateException()
        val result = runBlocking {
            script.getClass(ScriptWithClassloaderEvaluationConfiguration)
        }
        if (result.isError()) {
            for (diag in report.reports) {
                EssentialScripting.logger.info(diag.render())
            }
            return null
        }
        val env = script.compilationConfiguration[ScriptCompilationConfiguration.environment]
        // Check the annotation that env and version match!
        println(env)

        val entrypoint = findEntrypoint(context, result.valueOrThrow())
        return entrypoint
    }

    private fun <M> findEntrypoint(context: EnvironmentContext<M>, klass: KClass<*>): ScriptEntrypoint<M>? {
        val argsType = Array<String>::class
        val mcType = context.minecraft::class

        findEntrypoint<M>(klass) { _, _ -> emptyArray() }?.let { return it }
        findEntrypoint<M>(klass, argsType) { _, args -> arrayOf(args) }?.let { return it }
        findEntrypoint<M>(klass, mcType) { mc, _ -> arrayOf(mc) }?.let { return it }
        findEntrypoint<M>(klass, mcType, argsType) { mc, args -> arrayOf(mc, args) }?.let { return it }

        return null
    }

    private inline fun <M> findEntrypoint(
        klass: KClass<*>,
        vararg params: KClass<*>,
        crossinline remap: (M, Array<String>) -> Array<Any?>
    ): ScriptEntrypoint<M>? {
        val function = klass.declaredMemberFunctions.find { func ->
            func.name == "main" && func.parameters.drop(1).map { it.type.classifier } == params.toList()
        } ?: return null
        val instance = klass.java.getDeclaredConstructor().newInstance()
        return ScriptEntrypoint { mc, args -> function.callSuspend(instance, *remap(mc, args)) }
    }
}