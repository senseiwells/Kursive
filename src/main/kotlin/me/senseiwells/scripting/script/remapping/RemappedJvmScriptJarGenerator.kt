package me.senseiwells.scripting.script.remapping

import me.senseiwells.scripting.script.configuration.MappingType
import me.senseiwells.scripting.script.configuration.mappings
import me.senseiwells.scripting.utils.ScriptRemappingUtils
import me.senseiwells.scripting.script.remapping.metadata.KotlinMetadataTinyRemapperExtensionImpl
import net.fabricmc.tinyremapper.NonClassCopyMode
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvmhost.saveToJar

class RemappedJvmScriptJarGenerator(private val outputJar: Path): ScriptEvaluator {
    override suspend fun invoke(
        compiledScript: CompiledScript,
        scriptEvaluationConfiguration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<EvaluationResult> {
        if (compiledScript !is KJvmCompiledScript) {
            val message = "Cannot generate jar: unsupported compiled script type $compiledScript"
            return ResultWithDiagnostics.Failure(message.asErrorDiagnostics())
        }
        try {
            this.remapAndSaveScript(compiledScript)
            return ResultWithDiagnostics.Success(EvaluationResult(ResultValue.NotEvaluated, scriptEvaluationConfiguration))
        } catch (e: Throwable) {
            val diagnostics = e.asDiagnostics(
                customMessage = "Cannot generate script jar: ${e.message}",
                path = compiledScript.sourceLocationId
            )
            return ResultWithDiagnostics.Failure(diagnostics)
        }
    }

    private fun remapAndSaveScript(script: KJvmCompiledScript) {
        val type = script.compilationConfiguration[ScriptCompilationConfiguration.mappings]
            ?: MappingType.Mojang

        if (!ScriptRemappingUtils.shouldRemap(type)) {
            script.saveToJar(this.outputJar.toFile())
            return
        }

        val temp = Files.createTempFile(
            this.outputJar.parent,
            this.outputJar.nameWithoutExtension,
            ".${this.outputJar.extension}"
        )

        script.saveToJar(temp.toFile())

        val mappings = ScriptRemappingUtils.getMappings(type)
        val remapper = TinyRemapper.newRemapper()
            .withMappings(mappings)
            .extension(KotlinMetadataTinyRemapperExtensionImpl)
            .build()
        try {
            OutputConsumerPath.Builder(this.outputJar).build().use { consumer ->
                consumer.addNonClassFiles(temp, NonClassCopyMode.UNCHANGED, remapper)
                remapper.readClassPath(ScriptRemappingUtils.getMappedJar(type))
                remapper.readInputs(temp)
                remapper.apply(consumer)
            }
        } finally {
            remapper.finish()
            temp.deleteIfExists()
        }
    }
}