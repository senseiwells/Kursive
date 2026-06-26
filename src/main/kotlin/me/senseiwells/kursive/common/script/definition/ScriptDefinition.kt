package me.senseiwells.kursive.common.script.definition

import java.nio.file.Path
import java.time.Instant
import kotlin.script.experimental.api.SourceCode

interface ScriptDefinition<M: Any> {
    val name: String

    fun isValid(): Boolean

    fun getSource(): SourceCode

    fun lastSourceUpdate(): Instant

    fun getCompileDirectoryPath(): Path

    fun delete()

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}