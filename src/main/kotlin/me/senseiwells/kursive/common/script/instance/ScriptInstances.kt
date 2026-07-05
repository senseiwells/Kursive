package me.senseiwells.kursive.common.script.instance

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.*
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.script.definition.resolver.ScriptDefinitionSource
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.instance.sync.ScriptSynchronizer
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.onSuccess

class ScriptInstances<M: Any>(
    private val source: ScriptDefinitionSource<M>,
    private val synchronizer: ScriptSynchronizer<M> = { }
): Iterable<ScriptInstance<M>> {
    private val scriptsByDefinition = ConcurrentHashMap<ScriptDefinition<M>, ScriptInstance<M>>()
    private val scriptsById = Int2ObjectOpenHashMap<ScriptInstance<M>>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler(::handleException))

    private val ids = ScriptInstance.Id.Provider()

    private var compiling: Job? = null

    var dirty: Boolean = false
        private set

    fun add(definition: ScriptDefinition<M>): Boolean {
        if (!this.scriptsByDefinition.containsKey(definition)) {
            val id = this.ids.next()
            val instance = ScriptInstance(id, definition, this.synchronizer)
            this.scriptsByDefinition[definition] = instance
            this.scriptsById[id.value] = instance
            return true
        }
        return false
    }

    fun find(name: String): ScriptInstance<M>? {
        for ((definition, instance) in this.scriptsByDefinition) {
            if (definition.name == name) {
                return instance
            }
        }
        return null
    }

    fun find(id: ScriptInstance.Id): ScriptInstance<M>? {
        return this.scriptsById[id.value]
    }

    fun suggestions(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        return SharedSuggestionProvider.suggest(this.scriptsByDefinition.keys.map { "\"${it.name}\"" }, builder)
    }

    fun initialize(compile: Boolean = false) {
        this.source.initialize()

        for (definition in this.source.get()) {
            this.add(definition)
        }

        if (compile) {
            this.compiling = this.scope.launch { compileScripts() }
        }
    }

    suspend fun start(environment: ExecutionEnvironment<M, *>) {
        val compiling = this.compiling
        if (compiling != null) {
            compiling.join()
            this.compiling = null
        }

        for (instance in this) {
            val metadata = instance.tryGetMetadata() ?: continue
            if (metadata.auto) {
                Kursive.startScript(environment, instance)
            }
        }
    }

    fun update() {
        val available = this.source.get()
        var dirty = false
        for (definition in available) {
            dirty = dirty or this.add(definition)
        }
        this.dirty = dirty or this.deleteInvalidScripts()
    }

    fun close() {
        this.scriptsByDefinition.clear()
        this.scriptsById.clear()
        this.source.close()

        this.scope.cancel()
    }

    private suspend fun compileScripts() = coroutineScope {
        val start = System.currentTimeMillis()
        Kursive.logger.info("Compiling scripts...")
        map { instance ->
            launch {
                if (instance.shouldRecompile()) {
                    instance.compile().onSuccess {
                        instance.tryGetOrLoadMetadata().asSuccess()
                    }
                } else {
                    instance.tryGetOrLoadMetadata()
                }
            }
        }.joinAll()
        val time = System.currentTimeMillis() - start
        Kursive.logger.info("Finished compiling scripts (${time}ms)")
    }

    private fun deleteInvalidScripts(): Boolean {
        var dirty = false
        val iterator = this.scriptsByDefinition.iterator()
        for ((definition, instance) in iterator) {
            if (!definition.isValid()) {
                iterator.remove()
                this.scriptsById.remove(instance.id.value, instance as Any)
                this.scope.launch { instance.delete() }
                dirty = true
            }
        }
        return dirty
    }

    private fun handleException(@Suppress("Unused") context: CoroutineContext, throwable: Throwable) {
        Kursive.logger.error("Exception with script:", throwable)
    }

    override fun iterator(): Iterator<ScriptInstance<M>> {
        return this.scriptsByDefinition.values.iterator()
    }
}