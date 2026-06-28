package me.senseiwells.kursive.common.script.instance

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import me.senseiwells.kursive.api.ScriptContext
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.script.configuration.*
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.execution.ScriptEntrypoint
import me.senseiwells.kursive.common.script.instance.sync.ScriptSynchronizer
import me.senseiwells.kursive.common.utils.EnvironmentUtils
import me.senseiwells.kursive.common.utils.ScriptConfigurationUtils
import net.minecraft.network.codec.ByteBufCodecs
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvm.util.isIncomplete
import kotlin.script.experimental.jvmhost.BasicJvmScriptJarGenerator
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.loadScriptFromJar

class ScriptInstance<M: Any>(
    val id: Id,
    val definition: ScriptDefinition<M>,
    private val synchronizer: ScriptSynchronizer<M>
) {
    private val mutex = Mutex()

    private var entrypoint: ScriptEntrypoint<M>? = null
    private var loaded: LoadedScript? = null
    private var job: Job? = null

    var diagnostics: List<ScriptDiagnostic> = listOf()
        private set
    var iteration: Int = 0
        private set

    fun isRunning(): Boolean {
        val job = this.job
        return job != null && job.isActive
    }

    suspend fun start(
        environment: ExecutionEnvironment<M, *>
    ): ResultWithDiagnostics<Unit> {
        val result = this.mutex.withLock { this.internalStart(environment) }
        this.diagnostics = result.reports
        this.synchronizer.sync(this)
        return result
    }

    suspend fun compile(): ResultWithDiagnostics<Unit> = coroutineScope {
        withContext(Dispatchers.Default) {
            val result = mutex.withLock { internalCompile() }
            diagnostics = result.reports
            synchronizer.sync(this@ScriptInstance)
            result
        }
    }

    fun stop(): Boolean {
        if (!this.isRunning()) {
            return false
        }
        this.job?.cancel()
        this.job = null
        this.synchronizer.sync(this)
        return true
    }

    fun tryGetMetadata(): ScriptMetadata? {
        return this.loaded?.metadata
    }

    suspend fun tryGetOrLoadMetadata(): ScriptMetadata? {
        if (this.loaded == null && !this.shouldRecompile()) {
            this.getOrLoadScript()
        }
        return this.tryGetMetadata()
    }

    fun isCompiled(): Boolean {
        return !this.shouldRecompile()
    }

    suspend fun delete() {
        this.mutex.withLock {
            this.job?.join()
            this.definition.delete()
            this.closeLoadedScript()
            this.getCompileJarPath().deleteIfExists()
        }
    }

    private suspend fun internalStart(
        environment: ExecutionEnvironment<M, *>
    ): ResultWithDiagnostics<Unit> = coroutineScope cs@ {
        if (shouldRecompile()) {
            val result = withContext(Dispatchers.Default) {
                internalCompile()
            }
            if (result.isError() || result.isIncomplete()) {
                return@cs result
            }
            return@cs result.onSuccess { execute(environment) }
        }
        return@cs execute(environment)
    }

    private fun internalCompile(): ResultWithDiagnostics<Unit> {
        if (this.isRunning()) {
            return makeFailureResult("Cannot re-compile while script is running")
        }
        this.closeLoadedScript()

        val start = System.currentTimeMillis()
        this.log("Starting to compile ${this.definition.name}")
        val jar = this.getCompileJarPath()
        val tmp = jar.resolveSibling("${jar.name}.tmp")
        val host = BasicJvmScriptingHost(
            ScriptConfigurationUtils.HOST_CONFIGURATION, evaluator = BasicJvmScriptJarGenerator(tmp.toFile())
        )
        val result = host.eval(
            this.definition.getSource(),
            ScriptWithClasspathCompilationConfiguration,
            ScriptWithClassloaderEvaluationConfiguration
        )
        if (!result.isError()) {
            Files.move(tmp, jar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            this.iteration += 1
        }
        val time = System.currentTimeMillis() - start
        this.log("Finished compiling ${this.definition.name}, took $time ms")
        return result.onSuccess { Unit.asSuccess() }
    }

    private suspend fun execute(environment: ExecutionEnvironment<M, *>): ResultWithDiagnostics<Unit> {
        if (this.isRunning()) {
            return makeFailureResult("Cannot execute script while it's already running")
        }
        return this.getOrFindEntrypoint(environment).onSuccess { (entrypoint, metadata) ->
            val job = environment.invoke(entrypoint, metadata)
            this.job = job
            Unit.asSuccess()
        }
    }

    private fun shouldRecompile(): Boolean {
        val jar = this.getCompileJarPath()
        if (jar.isRegularFile()) {
            try {
                val lastModifiedTime = jar.readAttributes<BasicFileAttributes>().lastModifiedTime()
                return lastModifiedTime.toInstant() < this.definition.lastSourceUpdate()
            } catch (_: IOException) {

            }
        }
        return true
    }

    private fun getCompileJarPath(): Path {
        return this.definition.getCompileDirectoryPath().resolve("${this.definition.name}.jar")
    }

    private suspend fun getOrFindEntrypoint(
        environment: ExecutionEnvironment<M, *>
    ): ResultWithDiagnostics<EntrypointWithMetadata<M>> {
        val (script, klass, metadata) = this.getOrLoadScript()
            .onFailure { return ResultWithDiagnostics.Failure(it.reports) }
            .valueOrThrow()

        val existing = this.entrypoint
        if (existing != null) {
            return EntrypointWithMetadata(existing, metadata).asSuccess()
        }

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
        return diagnostics + findEntrypoint(environment, klass).onSuccess { entrypoint ->
            this.entrypoint = entrypoint
            EntrypointWithMetadata(entrypoint, metadata).asSuccess()
        }
    }

    private suspend fun getOrLoadScript(): ResultWithDiagnostics<LoadedScript> {
        val existing = this.loaded
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

            this.log("Loading script class for ${this.definition.name}")
            val result = script.getClass(ScriptWithClassloaderEvaluationConfiguration)
            val metadata = script.compilationConfiguration[ScriptCompilationConfiguration.scriptMetadata]
                ?: ScriptMetadata.named(this.definition.name)
            return result.onSuccess { klass ->
                val loaded = LoadedScript(script, klass, metadata)
                this.loaded = loaded
                loaded.asSuccess()
            }
        } catch (e: IOException) {
            return makeFailureResult(e.asDiagnostics(customMessage = "Failed to load script jar"))
        }
    }

    private fun closeLoadedScript() {
        this.entrypoint = null

        val loaded = this.loaded ?: return
        val classLoader = loaded.klass.java.classLoader
        if (classLoader is Closeable) {
            classLoader.close()
        }
        this.loaded = null
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
        return ScriptEntrypoint { mc, args ->
            val instance = klass.java.getDeclaredConstructor().newInstance()
            function.callSuspend(instance, *remap(mc, args))
        }
    }

    private fun log(message: String) {
        if (DEBUG) {
            Kursive.logger.info(message)
        }
    }

    private data class LoadedScript(val script: CompiledScript, val klass: KClass<*>, val metadata: ScriptMetadata)

    private data class EntrypointWithMetadata<M>(val entrypoint: ScriptEntrypoint<M>, val metadata: ScriptMetadata)

    @JvmInline
    value class Id private constructor(internal val value: Int) {
        class Provider internal constructor() {
            private val current = atomic(0)

            fun next(): Id {
                return Id(this.current.getAndIncrement())
            }
        }

        companion object {
            val STREAM_CODEC = ByteBufCodecs.INT.map(::Id, Id::value)
        }
    }

    companion object {
        private const val DEBUG = false
    }
}

