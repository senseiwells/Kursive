package me.senseiwells.kursive.common.script.definition.resolver

import kotlinx.coroutines.*
import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.utils.ScriptFileUtils
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.isReadable
import kotlin.io.path.name
import kotlin.io.path.walk

class FileScriptDefinitionSource<M: Any>(
    private val directory: Path
): ScriptDefinitionSource<M> {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var indexed = ConcurrentHashMap<Path, ScriptDefinition<M>>()

    override fun initialize() {
        val directory = this.directory.createDirectories()
        this.scope.launch {
            indexScriptDefinitions(directory, indexed)
            val service = FileSystems.getDefault().newWatchService()
            directory.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
            service.use { continuallyPollWatchService(directory, it, indexed) }
        }
    }

    override fun get(): Collection<ScriptDefinition<M>> {
        return this.indexed.values
    }

    override fun close() {
        this.indexed.clear()
        this.scope.cancel()
    }

    private fun indexScriptDefinitions(directory: Path, indexed: MutableMap<Path, ScriptDefinition<M>>) {
        val compiled = this.getCompiledDirectory(directory)
        directory.walk().filter { path -> this.isScriptFile(path) }.forEach { path ->
            indexed[path] = FileScriptDefinition.of(path, directory, compiled)
        }
    }

    private fun updateIndexedScriptDefinitions(
        directory: Path,
        indexed: MutableMap<Path, ScriptDefinition<M>>,
        event: WatchEvent<Path>
    ) {
        val path = directory.resolve(event.context())
        if (!this.isScriptFile(path)) {
            return
        }

        when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE -> {
                if (path.isReadable()) {
                    indexed[path] = FileScriptDefinition.of(path, directory, this.getCompiledDirectory(directory))
                }
            }
            StandardWatchEventKinds.ENTRY_DELETE -> {
                indexed.remove(path)
            }
        }
    }

    private fun CoroutineScope.continuallyPollWatchService(
        directory: Path,
        service: WatchService,
        indexed: MutableMap<Path, ScriptDefinition<M>>
    ) {
        while (isActive) {
            val key = service.poll(100, TimeUnit.MILLISECONDS) ?: continue
            for (event in key.pollEvents()) {
                @Suppress("UNCHECKED_CAST")
                updateIndexedScriptDefinitions(directory, indexed, event as WatchEvent<Path>)
            }
            key.reset()
        }
    }

    private fun getCompiledDirectory(directory: Path): Path {
        return directory.resolve(".compiled").createDirectories()
    }

    private fun isScriptFile(path: Path): Boolean {
        return path.name.endsWith(ScriptFileUtils.SUFFIX)
    }
}