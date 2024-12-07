package me.senseiwells.scripting.script.configuration

import me.senseiwells.scripting.EssentialScripting
import me.senseiwells.scripting.script.Script
import me.senseiwells.scripting.script.annotation.Environment
import me.senseiwells.scripting.script.annotation.Mappings

import me.senseiwells.scripting.utils.ScriptRemappingUtils
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.util.version.VersionParser
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.util.filterByAnnotationType

class ScriptWithClasspathCompilationConfiguration: ScriptCompilationConfiguration({
    defaultImports(Mappings::class, Environment::class)
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
        updateClasspath(listOf(ScriptRemappingUtils.getMojangClientJar()!!.toFile()))

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
    val mappedJar = when (annotation.type.lowercase()) {
        else -> ScriptRemappingUtils.getMojangClientJar()
    }
    if (mappedJar == null) {
        return ResultWithDiagnostics.Failure(
            "Failed to load mapped jar (${annotation.type}) for script".asErrorDiagnostics()
        )
    }
    return context.compilationConfiguration.with {
        updateClasspath(listOf(mappedJar.toFile()))
    }.asSuccess()
}

private fun configureEnvironment(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val (annotation, _) = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.first {
        it.annotation is Environment
    } ?: return context.compilationConfiguration.asSuccess()
    annotation as Environment
    val version = VersionParser.parse(annotation.version, true)
    val container = FabricLoader.getInstance().getModContainer("minecraft").get()
    val result = version.compareTo(container.metadata.version)
    when {
        result > 0 -> EssentialScripting.logger.warn(
            "Script was made for newer version of Minecraft: ${annotation.version}"
        )
        result < 0 -> EssentialScripting.logger.warn(
            "Script was made for an older version of Minecraft: ${annotation.version}"
        )
    }
    return context.compilationConfiguration.asSuccess()
}
