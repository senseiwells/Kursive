package me.senseiwells.essential_scripting.script

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import me.senseiwells.essential_scripting.script.configuration.ScriptWithClassloaderEvaluationConfiguration
import me.senseiwells.essential_scripting.script.configuration.ScriptWithClasspathCompilationConfiguration
import me.senseiwells.essential_scripting.script.configuration.environment
import me.senseiwells.essential_scripting.script.execution.EnvironmentContext
import me.senseiwells.essential_scripting.script.execution.ScriptEntrypoint
import me.senseiwells.essential_scripting.script.remapping.RemappedJvmScriptJarGenerator
import me.senseiwells.essential_scripting.utils.EnvironmentUtils
import me.senseiwells.scripting.ScriptingContext
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.readAttributes
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.loadScriptFromJar

abstract class ScriptInstance<M> {
    private var entrypoint: ScriptEntrypoint<M>? = null
    private var job: Job? = null

    abstract val name: String

    abstract fun isValid(): Boolean

    abstract fun getSource(): SourceCode

    protected abstract fun lastSourceUpdate(): Instant

    protected abstract fun getCompileDirectoryPath(): Path

    fun isRunning(): Boolean {
        val job = this.job
        return job != null && job.isActive
    }

    fun compile(): ResultWithDiagnostics<Unit> {
        this.entrypoint = null
        val jar = this.getCompileJarPath()
        val host = BasicJvmScriptingHost(evaluator = RemappedJvmScriptJarGenerator(jar))
        val result = host.eval(
            this.getSource(),
            ScriptWithClasspathCompilationConfiguration,
            ScriptWithClassloaderEvaluationConfiguration
        )
        return result.onSuccess { Unit.asSuccess() }
    }

    fun execute(context: EnvironmentContext<M>): ResultWithDiagnostics<Boolean> {
        if (this.isRunning()) {
            return false.asSuccess()
        }
        return this.getOrFindEntrypoint(context).onSuccess { entrypoint ->
            val job = context.invoke(entrypoint)
            this.job = job
            true.asSuccess()
        }
    }

    fun cancel(): Boolean {
        if (!this.isRunning()) {
            return false
        }
        this.job!!.cancel()
        this.job = null
        return true
    }

    fun shouldRecompile(): Boolean {
        val jar = this.getCompileJarPath()
        if (jar.isRegularFile()) {
            try {
                val creationTime = jar.readAttributes<BasicFileAttributes>().creationTime()
                return creationTime.toInstant() < this.lastSourceUpdate()
            } catch (_: IOException) {

            }
        }
        return true
    }

    private fun getCompileJarPath(): Path {
        return this.getCompileDirectoryPath().resolve("${this.name}.jar")
    }

    private fun getOrFindEntrypoint(context: EnvironmentContext<M>): ResultWithDiagnostics<ScriptEntrypoint<M>> {
        val existing = this.entrypoint
        if (existing != null) {
            return existing.asSuccess()
        }

        val jar = this.getCompileJarPath()
        if (!jar.isReadable()) {
            return makeFailureResult("Script jar isn't readable: '${jar}'")
        }
        try {
            val script = jar.toFile().loadScriptFromJar(false)
                ?: return makeFailureResult("Failed to find script main class for ${this.name}")

            val result = runBlocking {
                script.getClass(ScriptWithClassloaderEvaluationConfiguration)
            }
            val diagnostics = ArrayList<ScriptDiagnostic>()
            val env = script.compilationConfiguration[ScriptCompilationConfiguration.environment]
            if (env != null) {
                if (context.type != env.type) {
                    return makeFailureResult(
                        "Script marked for ${env.type.name} environment, but running on ${context.type}"
                    )
                }
                diagnostics.addAll(EnvironmentUtils.getDiagnosticsForTarget(env.version))
            }
            return diagnostics + result.onSuccess { klass ->
                findEntrypoint(context, klass).onSuccess { entrypoint ->
                    this.entrypoint = entrypoint
                    entrypoint.asSuccess()
                }
            }
        } catch (e: IOException) {
            return makeFailureResult(e.asDiagnostics(customMessage = "Failed to load script jar"))
        }
    }

    private fun findEntrypoint(
        context: EnvironmentContext<M>,
        klass: KClass<*>
    ): ResultWithDiagnostics<ScriptEntrypoint<M>> {
        val argsType = ScriptingContext::class
        val mcType = context.minecraft::class

        findEntrypoint(klass) { _, _ -> emptyArray() }?.let { return it.asSuccess() }
        findEntrypoint(klass, argsType) { _, args -> arrayOf(args) }?.let { return it.asSuccess() }
        findEntrypoint(klass, mcType) { mc, _ -> arrayOf(mc) }?.let { return it.asSuccess() }
        findEntrypoint(klass, mcType, argsType) { mc, args -> arrayOf(mc, args) }?.let { return it.asSuccess() }

        return makeFailureResult("Failed to find script entrypoint")
    }

    private inline fun findEntrypoint(
        klass: KClass<*>,
        vararg params: KClass<*>,
        crossinline remap: (M, ScriptingContext) -> Array<Any?>
    ): ScriptEntrypoint<M>? {
        val function = klass.declaredMemberFunctions.find { func ->
            func.name == "main" && func.parameters.drop(1).map { it.type.classifier } == params.toList()
        } ?: return null
        val instance = klass.java.getDeclaredConstructor().newInstance()
        return ScriptEntrypoint { mc, args ->
            function.callSuspend(instance, *remap(mc, args))
        }
    }
}

