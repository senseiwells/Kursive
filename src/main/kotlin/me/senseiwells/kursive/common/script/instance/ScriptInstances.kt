package me.senseiwells.kursive.common.script.instance

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.script.definition.resolver.ScriptDefinitionSource
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture

class ScriptInstances<M: Any>(
    private val source: ScriptDefinitionSource<M>
): Iterable<ScriptInstance<M>> {
    private val scriptsByDefinition = LinkedHashMap<ScriptDefinition<M>, ScriptInstance<M>>()
    private val scriptsById = Int2ObjectOpenHashMap<ScriptInstance<M>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val ids = ScriptInstance.Id.Provider()

    var dirty: Boolean = false
        private set

    fun add(definition: ScriptDefinition<M>): Boolean {
        if (!this.scriptsByDefinition.containsKey(definition)) {
            val id = this.ids.next()
            val instance = ScriptInstance(id, definition)
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

    fun initialize(minecraft: M) {
        this.source.initialize(minecraft)
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
        this.source.close()
    }

    private fun deleteInvalidScripts(): Boolean {
        var dirty = false
        for ((definition, instance) in this.scriptsByDefinition.toList()) {
            if (!definition.isValid()) {
                this.scriptsByDefinition.remove(definition, instance)
                this.scriptsById.remove(instance.id.value, instance as Any)
                this.scope.launch { instance.delete() }
                dirty = true
            }
        }
        return dirty
    }

    override fun iterator(): Iterator<ScriptInstance<M>> {
        return this.scriptsByDefinition.values.iterator()
    }
}