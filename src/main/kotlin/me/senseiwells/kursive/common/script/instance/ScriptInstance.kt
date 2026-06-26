package me.senseiwells.kursive.common.script.instance

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import me.senseiwells.kursive.api.ScriptContext
import me.senseiwells.kursive.common.script.configuration.*
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.execution.ScriptEntrypoint
import me.senseiwells.kursive.common.utils.EnvironmentUtils
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
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

class ScriptInstance<M: Any>(
    private val definition: ScriptDefinition<M>
) {
    private var entrypoint: ScriptEntrypoint<M>? = null
    private var metadata: ScriptMetadata? = null
    private var job: Job? = null

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
            this.definition.getSource(),
            ScriptWithClasspathCompilationConfiguration,
            ScriptWithClassloaderEvaluationConfiguration
        )
        return result.onSuccess { Unit.asSuccess() }
    }

    fun prepare(environment: ExecutionEnvironment<M, *>): ResultWithDiagnostics<Unit> {
        return this.getOrFindEntrypoint(environment).onSuccess { Unit.asSuccess() }
    }

    fun execute(environment: ExecutionEnvironment<M, *>): ResultWithDiagnostics<Unit> {
        if (this.isRunning()) {
            return makeFailureResult("Cannot execute script while it's already running")
        }
        return this.getOrFindEntrypoint(environment).onSuccess { entrypoint ->
            val job = environment.invoke(entrypoint, this.metadata ?: ScriptMetadata.named(this.definition.name))
            this.job = job
            Unit.asSuccess()
        }
    }

    fun cancel(): Boolean {
        if (!this.isRunning()) {
            return false
        }
        this.job?.cancel()
        this.job = null
        return true
    }

    fun shouldRecompile(): Boolean {
        val jar = this.getCompileJarPath()
        if (jar.isRegularFile()) {
            try {
                val creationTime = jar.readAttributes<BasicFileAttributes>().creationTime()
                return creationTime.toInstant() < this.definition.lastSourceUpdate()
            } catch (_: IOException) {

            }
        }
        return true
    }

    suspend fun delete() {
        this.job?.join()
        this.definition.delete()
        this.getCompileJarPath().deleteIfExists()
    }

    private fun getCompileJarPath(): Path {
        return this.definition.getCompileDirectoryPath().resolve("${this.definition.name}.jar")
    }

    private fun getOrFindEntrypoint(environment: ExecutionEnvironment<M, *>): ResultWithDiagnostics<ScriptEntrypoint<M>> {
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
                ?: return makeFailureResult("Failed to find script main class for ${this.definition.name}")

            val result = runBlocking {
                script.getClass(ScriptWithClassloaderEvaluationConfiguration)
            }
            this.metadata = script.compilationConfiguration[ScriptCompilationConfiguration.scriptMetadata]
            val diagnostics = ArrayList<ScriptDiagnostic>()
            val env = script.compilationConfiguration[ScriptCompilationConfiguration.environment]
            if (env != null) {
                if (environment.type != env.type) {
                    return makeFailureResult(
                        "Script marked for ${env.type.name} environment, but running on ${environment.type}"
                    )
                }
                diagnostics.addAll(EnvironmentUtils.getDiagnosticsForTarget(env.version))
            }
            return diagnostics + result.onSuccess { klass ->
                findEntrypoint(environment, klass).onSuccess { entrypoint ->
                    this.entrypoint = entrypoint
                    entrypoint.asSuccess()
                }
            }
        } catch (e: Exception) {
            return makeFailureResult(e.asDiagnostics(customMessage = "Failed to load script jar"))
        }
    }

    private fun findEntrypoint(
        environment: ExecutionEnvironment<M, *>,
        klass: KClass<*>
    ): ResultWithDiagnostics<ScriptEntrypoint<M>> {
        val mcType = environment.minecraftType()
        val ctxType = environment.contextType()

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

