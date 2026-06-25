package me.senseiwells.kursive.common.script.definition

import me.senseiwells.kursive.common.script.instance.FileScriptInstance
import me.senseiwells.kursive.common.script.instance.ScriptInstance
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.pathString

class FileScriptDefinition<M: Any> private constructor(
    val absolute: Path,
    val path: String,
    val compiled: Path,
): ScriptDefinition<M> {
    override fun create(): ScriptInstance<M> {
        return FileScriptInstance(this)
    }

    override fun delete(instance: ScriptInstance<M>) {
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
            return FileScriptDefinition(path, relativized.pathString.removeSuffix(".kts"), compiled)
        }
    }
}