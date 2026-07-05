package me.senseiwells.kursive.client.script.executable

import me.senseiwells.kursive.common.script.configuration.ScriptMetadata
import me.senseiwells.kursive.common.script.diagnostics.FormattedDiagnostics
import me.senseiwells.kursive.common.script.instance.ScriptInstance

interface ScriptHandle {
    fun id(): ScriptInstance.Id

    fun name(): String

    fun isRunning(): Boolean

    fun isCompiled(): Boolean

    fun getMetadata(): ScriptMetadata?

    fun getDiagnostics(): FormattedDiagnostics?

    fun start()

    fun compile()

    fun stop()

    fun delete()

    fun toggle() {
        if (this.isRunning()) {
            this.stop()
        } else {
            this.start()
        }
    }
}