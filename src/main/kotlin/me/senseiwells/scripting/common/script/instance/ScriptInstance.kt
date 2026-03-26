package me.senseiwells.scripting.common.script.instance

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import me.senseiwells.scripting.api.ScriptContext
import me.senseiwells.scripting.common.script.configuration.ScriptWithClassloaderEvaluationConfiguration
import me.senseiwells.scripting.common.script.configuration.ScriptWithClasspathCompilationConfiguration
import me.senseiwells.scripting.common.script.configuration.environment
import me.senseiwells.scripting.common.script.definition.ScriptDefinition
import me.senseiwells.scripting.common.script.execution.ExecutionEnvironment
import me.senseiwells.scripting.common.script.execution.ScriptEntrypoint
import me.senseiwells.scripting.common.utils.EnvironmentUtils
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.concurrent.CompletableFuture
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.readAttributes
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvmhost.BasicJvmScriptJarGenerator
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.loadScriptFromJar

abstract class ScriptInstance<M: Any>(
    private val definition: ScriptDefinition<M>
) {
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
        if (this.isRunning()) {
            return makeFailureResult("Cannot re-compile while script is running")
        }
        this.entrypoint = null
        val jar = this.getCompileJarPath().toFile()
        val host = BasicJvmScriptingHost(evaluator = BasicJvmScriptJarGenerator(jar))
        val result = host.eval(
            this.getSource(),
            ScriptWithClasspathCompilationConfiguration,
            ScriptWithClassloaderEvaluationConfiguration
        )
        return result.onSuccess { Unit.asSuccess() }
    }

    fun prepare(context: ExecutionEnvironment<M>): ResultWithDiagnostics<Unit> {
        return this.getOrFindEntrypoint(context).onSuccess { Unit.asSuccess() }
    }

    fun execute(context: ExecutionEnvironment<M>): ResultWithDiagnostics<Unit> {
        if (this.isRunning()) {
            return makeFailureResult("Cannot execute script while it's already running")
        }
        return this.getOrFindEntrypoint(context).onSuccess { entrypoint ->
            val job = context.invoke(entrypoint)
            this.job = job
            Unit.asSuccess()
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

    fun delete(): CompletableFuture<Void?> {
        val future = CompletableFuture<Void?>()
        if (this.isRunning()) {
            this.job!!.invokeOnCompletion {
                future.complete(null)
            }
        } else {
            future.complete(null)
        }
        return future.thenRun {
            this.definition.delete(this)
            this.getCompileJarPath().deleteIfExists()
        }
    }

    private fun getCompileJarPath(): Path {
        return this.getCompileDirectoryPath().resolve("${this.name}.jar")
    }

    private fun getOrFindEntrypoint(context: ExecutionEnvironment<M>): ResultWithDiagnostics<ScriptEntrypoint<M>> {
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
        } catch (e: Exception) {
            return makeFailureResult(e.asDiagnostics(customMessage = "Failed to load script jar"))
        }
    }

    private fun findEntrypoint(
        context: ExecutionEnvironment<M>,
        klass: KClass<*>
    ): ResultWithDiagnostics<ScriptEntrypoint<M>> {
        val mcType = context.minecraftType()
        val ctxType = context.contextType()

        findEntrypoint(klass) { _, _ -> emptyArray() }?.let { return it.asSuccess() }
        findEntrypoint(klass, ctxType) { _, args -> arrayOf(args) }?.let { return it.asSuccess() }
        findEntrypoint(klass, mcType) { mc, _ -> arrayOf(mc) }?.let { return it.asSuccess() }
        findEntrypoint(klass, mcType, ctxType) { mc, args -> arrayOf(mc, args) }?.let { return it.asSuccess() }

        return makeFailureResult("Failed to find script entrypoint")
    }

    private inline fun findEntrypoint(
        klass: KClass<*>,
        vararg params: KClass<*>,
        crossinline remap: (M, ScriptContext) -> Array<Any?>
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

