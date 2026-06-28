package me.senseiwells.kursive.client.script.executable

import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import kotlin.script.experimental.api.ScriptDiagnostic

interface ScriptHandle {
    fun name(): String

    fun isRunning(): Boolean

    fun isCompiled(): Boolean

    fun getMetadata(): ScriptMetadata?

    fun getDiagnostics(): List<ScriptDiagnostic>

    fun start()

    fun compile()

    fun stop()

    fun toggle() {
        if (this.isRunning()) {
            this.stop()
        } else {
            this.start()
        }
    }
}