package me.senseiwells.kursive.common.script.instance

import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isReadable
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.toScriptSource

class FileScriptInstance<M: Any>(
    definition: FileScriptDefinition<M>
): ScriptInstance<M>(definition) {
    private val path: Path = definition.absolute
    private val compiled: Path = definition.compiled

    override val name: String = definition.path

    override fun isValid(): Boolean {
        return this.path.isReadable()
    }

    override fun getSource(): SourceCode {
        return this.path.toFile().toScriptSource()
    }

    override fun lastSourceUpdate(): Instant {
        return this.path.getLastModifiedTime().toInstant()
    }

    override fun getCompileDirectoryPath(): Path {
        return this.compiled
    }
}