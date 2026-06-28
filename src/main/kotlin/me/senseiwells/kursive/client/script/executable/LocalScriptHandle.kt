package me.senseiwells.kursive.client.script.executable

import kotlinx.coroutines.Deferred
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import me.senseiwells.kursive.common.script.diagnostics.FormattedDiagnostics
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import net.casual.arcade.utils.coroutine.getNowOrNull
import net.minecraft.client.Minecraft

class LocalScriptHandle(
    private val instance: ScriptInstance<Minecraft>,
    private val environment: ExecutionEnvironment<Minecraft, *>
): ScriptHandle {
    private lateinit var metadata: CachedMetadata

    override fun name(): String {
        return this.instance.definition.name
    }

    override fun isRunning(): Boolean {
        return this.instance.isRunning()
    }

    override fun isCompiled(): Boolean {
        return this.instance.isCompiled()
    }

    override fun getMetadata(): ScriptMetadata? {
        if (!this::metadata.isInitialized || this.metadata.iteration != this.instance.iteration) {
            val deferred = this.environment.async { instance.tryGetOrLoadMetadata() }
            this.metadata = CachedMetadata(this.instance.iteration, deferred)
        }
        return this.metadata.deferred.getNowOrNull()
    }

    override fun getDiagnostics(): FormattedDiagnostics? {
        return FormattedDiagnostics.from(this.instance.diagnostics)
    }

    override fun start() {
        this.environment.launch {
            val result = instance.start(environment)
            Kursive.logDiagnostics(result)
        }
    }

    override fun compile() {
        this.environment.launch {
            instance.compile()
        }
    }

    override fun stop() {
        this.instance.stop()
    }

    private data class CachedMetadata(val iteration: Int, val deferred: Deferred<ScriptMetadata?>)
}