package me.senseiwells.kursive.common.script.definition.resolver

import kotlinx.coroutines.*
import java.nio.file.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.isReadable

class PolledFileScriptDefinitionSource<M: Any>(
    directory: Path
): FileScriptDefinitionSource<M>(directory) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun initialize() {
        this.scope.coroutineContext.cancelChildren()

        super.initialize()
        this.scope.launch {
            val service = FileSystems.getDefault().newWatchService()
            directory.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
            service.use { continuallyPollWatchService(it) }
        }
    }

    override fun close() {
        super.close()
        this.scope.coroutineContext.cancelChildren()
    }

    private fun updateIndexedScriptDefinitions(event: WatchEvent<Path>) {
        val path = this.directory.resolve(event.context())
        if (!this.isScriptFile(path)) {
            return
        }

        when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE -> {
                if (path.isReadable()) {
                    this.indexed[path] = this.definition(path)
                }
            }
            StandardWatchEventKinds.ENTRY_DELETE -> {
                this.indexed.remove(path)
            }
        }
    }

    private fun CoroutineScope.continuallyPollWatchService(service: WatchService) {
        while (isActive) {
            val key = service.poll(100, TimeUnit.MILLISECONDS) ?: continue
            for (event in key.pollEvents()) {
                @Suppress("UNCHECKED_CAST")
                updateIndexedScriptDefinitions(event as WatchEvent<Path>)
            }
            key.reset()
        }
    }
}