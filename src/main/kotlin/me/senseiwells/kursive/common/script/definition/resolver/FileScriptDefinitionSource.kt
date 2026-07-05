package me.senseiwells.kursive.common.script.definition.resolver

import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.utils.ScriptFileUtils
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.name
import kotlin.io.path.walk

open class FileScriptDefinitionSource<M: Any>(
    protected val directory: Path
): ScriptDefinitionSource<M> {
    protected val indexed = ConcurrentHashMap<Path, FileScriptDefinition<M>>()

    override fun initialize() {
        this.indexed.clear()

        this.directory.createDirectories()
        this.reindex()
    }

    override fun get(): Collection<ScriptDefinition<M>> {
        return this.indexed.values
    }

    override fun close() {
        this.indexed.clear()
    }

    fun reindex() {
        this.directory.walk().filter { path -> this.isScriptFile(path) }.forEach { path ->
            this.indexed[path] = this.definition(path)
        }
    }

    protected fun definition(path: Path): FileScriptDefinition<M> {
        return FileScriptDefinition.of(path, this.directory, this.getCompiledDirectory())
    }

    protected fun isScriptFile(path: Path): Boolean {
        return path.name.endsWith(ScriptFileUtils.SUFFIX)
    }

    protected fun getCompiledDirectory(): Path {
        return this.directory.resolve(".compiled")
    }
}