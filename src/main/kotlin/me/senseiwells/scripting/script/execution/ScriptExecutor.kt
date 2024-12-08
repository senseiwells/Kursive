package me.senseiwells.scripting.script.execution

import kotlinx.coroutines.runBlocking
import me.senseiwells.scripting.EssentialScripting
import me.senseiwells.scripting.EssentialScriptingConfig
import me.senseiwells.scripting.script.configuration.ScriptWithClassloaderEvaluationConfiguration
import me.senseiwells.scripting.script.configuration.ScriptWithClasspathCompilationConfiguration
import me.senseiwells.scripting.script.configuration.environment
import me.senseiwells.scripting.script.remapping.RemappedJvmScriptJarGenerator
import net.minecraft.Util
import net.minecraft.client.Minecraft
import java.lang.reflect.Modifier
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createParentDirectories
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.loadScriptFromJar

object ScriptExecutor {
    fun <M> runScript(context: EnvironmentContext<M>, source: SourceCode) {
        CompletableFuture.supplyAsync({
            this.compileAndLoad(context, source)
        }, Util.ioPool()).handleAsync({ entrypoint, throwable ->
            // TODO: Clean this up
            if (entrypoint != null) {
                try {
                    entrypoint.invoke(context.minecraft, arrayOf())
                } catch (e: Throwable) {
                    EssentialScripting.logger.error("Exception during execution", e)
                }
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
        println(env)

        val clazz = result.valueOrThrow().java
        // Check the annotation that env and version match!
        val entrypoint = findEntrypoint(context, clazz)
        return entrypoint
    }

    private fun <M> findEntrypoint(context: EnvironmentContext<M>, clazz: Class<*>): ScriptEntrypoint<M>? {
        val argsType = Array<String>::class.java
        val mcType = context.minecraft::class.java

        findEntrypoint<M>(clazz) { _, _ -> emptyArray() }?.let { return it }
        findEntrypoint<M>(clazz, argsType) { _, args -> arrayOf(args) }?.let { return it }
        findEntrypoint<M>(clazz, mcType) { mc, _ -> arrayOf(mc) }?.let { return it }
        findEntrypoint<M>(clazz, mcType, argsType) { mc, args -> arrayOf(mc, args) }?.let { return it }

        return null
    }

    private inline fun <M> findEntrypoint(
        clazz: Class<*>,
        vararg params: Class<*>,
        crossinline remap: (M, Array<String>) -> Array<Any?>
    ): ScriptEntrypoint<M>? {
        try {
            val main = clazz.getDeclaredMethod("main", *params)
            if (Modifier.isStatic(main.modifiers)) {
                return ScriptEntrypoint { mc, args -> main.invoke(null, *remap(mc, args)) }
            }
            val instance = clazz.getDeclaredConstructor().newInstance()
            return ScriptEntrypoint { mc, args -> main.invoke(instance, *remap(mc, args)) }
        } catch (_: NoSuchMethodException) {
            return null
        }
    }
}