package me.senseiwells.essential_scripting.script.configuration

import me.senseiwells.essential_scripting.utils.EnvironmentUtils
import me.senseiwells.essential_scripting.utils.ScriptRemappingUtils
import me.senseiwells.scripting.annotation.Environment
import me.senseiwells.scripting.annotation.Mappings
import net.fabricmc.loader.api.FabricLoader
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
    // We want to keep the kotlin stdlib, it's not really a mod
    val kotlin = FabricLoader.getInstance()
        .getModContainer("fabric-language-kotlin")
        .orElseThrow { IllegalStateException("Expected fabric-language-kotlin to be present!") }
        .origin.paths
    if (!ScriptRemappingUtils.isCurrentIntermediary()) {
        // For dev environment
        val ignored = FabricLoader.getInstance().allMods.flatMap { it.origin.paths }.toHashSet()
        ignored.removeAll(kotlin.toSet())
        return classpathFromClassloader(BaseScript::class.java.classLoader)?.filter { file ->
            !ignored.contains(file.toPath())
        }
    }

    val root = FabricLoader.getInstance().gameDir
    // We don't include mods, they need remapping
    val mods = root.resolve("mods")
    // Theses are nested mods, they may also need remapping
    val includes = root.resolve(".fabric").resolve("processedMods")
    // The unmapped jar, intermediary outside dev
    val unmapped = ScriptRemappingUtils.getUnmappedMinecraftJar()
    return classpathFromClassloader(BaseScript::class.java.classLoader)?.filter { file ->
        val path = file.toPath()
        val parent = path.parent
        kotlin.contains(path) || (parent != mods && parent != includes && path != unmapped)
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
