package me.senseiwells.kursive.common.script.instance

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture

class ScriptInstances<M: Any>(
    private val resolver: (M) -> Collection<ScriptDefinition<M>>
) {
    private val scripts = LinkedHashMap<ScriptDefinition<M>, ScriptInstance<M>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun add(definition: ScriptDefinition<M>): Boolean {
        return this.scripts.putIfAbsent(definition, ScriptInstance(definition)) == null
    }

    fun resolve(minecraft: M): Set<ScriptDefinition<M>> {
        val discovered = this.resolver.invoke(minecraft)
        for (definition in discovered) {
            this.add(definition)
        }
        this.deleteInvalidScripts()
        return this.scripts.keys
    }

    fun find(name: String): ScriptInstance<M>? {
        for ((definition, instance) in this.scripts) {
            if (definition.name == name) {
                return instance
            }
        }
        return null
    }

    fun suggestions(minecraft: M, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        return SharedSuggestionProvider.suggest(this.resolve(minecraft).map { "\"${it.name}\"" }, builder)
    }

    private fun deleteInvalidScripts() {
        for ((definition, instance) in this.scripts.toList()) {
            if (!definition.isValid()) {
                this.scripts.remove(definition, instance)
                this.scope.launch { instance.delete() }
            }
        }
    }
}