package me.senseiwells.essential_scripting.script

import me.senseiwells.essential_scripting.EssentialScriptingConfig
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isReadable
import kotlin.io.path.nameWithoutExtension
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.toScriptSource

class FileScriptInstance<M>(
    private val path: Path
): ScriptInstance<M>() {
    override val name: String
        get() = this.path.nameWithoutExtension

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
        return EssentialScriptingConfig.resolve("compiled").createDirectories()
    }
}