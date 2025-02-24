package me.senseiwells.scripting.common.script.configuration

import me.senseiwells.scripting.annotation.Environment
import me.senseiwells.scripting.annotation.Mappings
import me.senseiwells.scripting.common.EssentialScripting
import me.senseiwells.scripting.common.utils.EnvironmentUtils
import me.senseiwells.scripting.common.utils.ScriptRemappingUtils
import net.minecraft.SharedConstants
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
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
    // Really, we should also filter out all mods and have a more robust way of detecting obf jar
    val files = classpathFromClassloader(BaseScript::class.java.classLoader)?.filter { file ->
        !unmapped.contains(file.toPath()) && !isObfuscatedJar(file.toPath())
    }
    return files
}

private fun isObfuscatedJar(path: Path): Boolean {
    val version = SharedConstants.getCurrentVersion().name
    if (!path.nameWithoutExtension.contains("minecraft-${version}")) {
        return false
    }
    var current: Path? = path.parent
    for (packageName in listOf(version, "minecraft", "mojang", "com")) {
        if (current == null || current.name != packageName) {
            return false
        }
        current = current.parent
    }
    return true
}

private fun configureMappings(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    EssentialScripting.logger.info("Configuring mappings")
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
