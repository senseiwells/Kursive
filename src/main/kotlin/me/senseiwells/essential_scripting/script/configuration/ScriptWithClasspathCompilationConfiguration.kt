package me.senseiwells.essential_scripting.script.configuration

import me.senseiwells.essential_scripting.utils.EnvironmentUtils
import me.senseiwells.essential_scripting.utils.ScriptRemappingUtils
import me.senseiwells.scripting.annotation.Environment
import me.senseiwells.scripting.annotation.Mappings
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.util.filterByAnnotationType

open class BaseScript

object ScriptWithClasspathCompilationConfiguration: ScriptCompilationConfiguration({
    defaultImports(Mappings::class, Environment::class)
    jvm {
        updateClasspath(getClasspath())
        jvmTarget("21")
    }
    refineConfiguration {
        onAnnotations(Mappings::class, handler = ::configureMappings)
        onAnnotations(Environment::class, handler = ::configureEnvironment)
    }
    // We need this so that everything is loaded with the KnotClassLoader
    baseClass(BaseScript::class)
}) {
    private fun readResolve(): Any = ScriptWithClasspathCompilationConfiguration
}

private fun getClasspath(): List<File>? {
    val unmapped = mutableSetOf(ScriptRemappingUtils.getUnmappedMinecraftJar())
    unmapped.addAll(ScriptRemappingUtils.getUnmappedModJars())
    return classpathFromClassloader(BaseScript::class.java.classLoader)?.filter { file ->
        !unmapped.contains(file.toPath())
    }
}

private fun configureMappings(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val (annotation) = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
        ?.filterByAnnotationType<Mappings>()
        ?.firstOrNull()
        ?: return context.compilationConfiguration.asSuccess()
    val type = MappingType.parse(annotation.type)
    val mappedJar = ScriptRemappingUtils.getMappedMinecraftJar(type)
    if (mappedJar != null) {
        val modJars = ScriptRemappingUtils.getMappedModJars(type)
        return context.compilationConfiguration.with {
            mappings(type)
            updateClasspath(listOf(mappedJar.toFile()))
            updateClasspath(modJars.map { it.toFile() })
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
    return context.compilationConfiguration.with {
        environment(result.valueOrThrow())
    }.asSuccess(EnvironmentUtils.getDiagnosticsForTarget(env.version))
}
