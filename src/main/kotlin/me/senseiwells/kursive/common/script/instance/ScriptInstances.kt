package me.senseiwells.kursive.common.script.instance

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
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
    private val scripts = LinkedHashMap<ScriptDefinition<M>, ScriptInstance<M>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var dirty: Boolean = false
        private set

    fun add(definition: ScriptDefinition<M>): Boolean {
        return this.scripts.putIfAbsent(definition, ScriptInstance(definition)) == null
    }

    fun find(name: String): ScriptInstance<M>? {
        for ((definition, instance) in this.scripts) {
            if (definition.name == name) {
                return instance
            }
        }
        return null
    }

    fun suggestions(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        return SharedSuggestionProvider.suggest(this.scripts.keys.map { "\"${it.name}\"" }, builder)
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
        for ((definition, instance) in this.scripts.toList()) {
            if (!definition.isValid()) {
                this.scripts.remove(definition, instance)
                this.scope.launch { instance.delete() }
                dirty = true
            }
        }
        return dirty
    }

    override fun iterator(): Iterator<ScriptInstance<M>> {
        return this.scripts.values.iterator()
    }
}