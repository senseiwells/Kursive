package me.senseiwells.scripting.script.configuration

import me.senseiwells.scripting.script.Script
import me.senseiwells.scripting.script.annotation.Environment
import me.senseiwells.scripting.script.annotation.Mappings
import me.senseiwells.scripting.utils.ScriptRemappingUtils
import me.senseiwells.scripting.utils.asWarningDiagnostics
import net.fabricmc.loader.api.FabricLoader
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.util.filterByAnnotationType

class ScriptWithClasspathCompilationConfiguration: ScriptCompilationConfiguration({
    defaultImports(Mappings::class, Environment::class)
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
    refineConfiguration {
        onAnnotations(Mappings::class, handler = ::configureMappings)
        onAnnotations(Environment::class, handler = ::configureEnvironment)
    }
    // We need this so that everything is loaded with the KnotClassLoader
    baseClass(Script::class)
})

private fun configureMappings(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val (annotation) = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
        ?.filterByAnnotationType<Mappings>()
        ?.firstOrNull()
        ?: return context.compilationConfiguration.asSuccess()
    val type = MappingType.parse(annotation.type)
    val mappedJar = ScriptRemappingUtils.getMappedJar(type)
    if (mappedJar != null) {
        return context.compilationConfiguration.with {
            mappings(type)
            updateClasspath(listOf(mappedJar.toFile()))
        }.asSuccess()
    }
    return ResultWithDiagnostics.Failure(
        "Failed to load mapped jar (${annotation.type}) for script".asErrorDiagnostics()
    )
}

private fun configureEnvironment(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val (annotation, _) = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.first {
        it.annotation is Environment
    } ?: return context.compilationConfiguration.asSuccess()
    annotation as Environment
    val result = EnvironmentWithVersion.parse(annotation)
    if (result.isError()) {
        return ResultWithDiagnostics.Failure(result.reports)
    }
    val env = result.valueOrThrow()
    val container = FabricLoader.getInstance().getModContainer("minecraft").get()
    val comparison = env.version.compareTo(container.metadata.version)
    val diagnostics = when {
        comparison > 0 -> listOf("Script was made for a newer version of Minecraft".asWarningDiagnostics())
        comparison < 0 -> listOf("Script was made for an older version of Minecraft".asWarningDiagnostics())
        else -> emptyList()
    }
    return context.compilationConfiguration.with {
        environment(result.valueOrThrow())
    }.asSuccess(diagnostics)
}
