package me.senseiwells.kursive.common.script.definition

import me.senseiwells.kursive.common.utils.ScriptFileUtils
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.deleteIfExists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isReadable
import kotlin.io.path.pathString
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.toScriptSource

class FileScriptDefinition<M: Any> private constructor(
    val absolute: Path,
    private val path: String,
    private val compiled: Path
): ScriptDefinition<M> {
    override val name: String = this.path

    override fun isValid(): Boolean {
        return this.absolute.isReadable()
    }

    override fun getSource(): SourceCode {
        return this.absolute.toFile().toScriptSource()
    }

    override fun lastSourceUpdate(): Instant {
        return this.absolute.getLastModifiedTime().toInstant()
    }

    override fun getCompileDirectoryPath(): Path {
        return this.compiled
    }

    override fun delete() {
        this.absolute.deleteIfExists()
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is FileScriptDefinition<*> && this.absolute == other.absolute)
    }

    override fun hashCode(): Int {
        return this.absolute.hashCode()
    }

    companion object {
        fun <M: Any> of(path: Path, origin: Path, compiled: Path): FileScriptDefinition<M> {
            val relativized = origin.relativize(path)
            val name = relativized.pathString.removeSuffix(ScriptFileUtils.SUFFIX)
            return FileScriptDefinition(path, name, compiled)
        }
    }
}