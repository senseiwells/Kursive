package me.senseiwells.kursive.common.script.instance

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import net.minecraft.commands.SharedSuggestionProvider
import java.util.concurrent.CompletableFuture

class ScriptInstances<M: Any>(
    private val resolver: (M) -> Collection<ScriptDefinition<M>>
) {
    private val scripts = LinkedHashMap<ScriptDefinition<M>, ScriptInstance<M>>()

    fun add(definition: ScriptDefinition<M>): Boolean {
        return this.scripts.putIfAbsent(definition, definition.create()) == null
    }

    fun resolve(minecraft: M): Collection<ScriptInstance<M>> {
        val discovered = this.resolver.invoke(minecraft)
        for (definition in discovered) {
            this.add(definition)
        }
        this.deleteInvalidScripts()
        return this.scripts.values
    }

    fun find(name: String): ScriptInstance<M>? {
        for (instance in this.scripts.values) {
            if (instance.name == name) {
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
            if (!instance.isValid()) {
                instance.delete().thenRun {
                    this.scripts.remove(definition, instance)
                }
            }
        }
    }
}