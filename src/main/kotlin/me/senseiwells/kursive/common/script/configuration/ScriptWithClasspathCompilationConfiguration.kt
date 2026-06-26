package me.senseiwells.kursive.common.script.configuration

import me.senseiwells.kursive.annotation.Environment
import me.senseiwells.kursive.annotation.Script
import me.senseiwells.kursive.common.utils.EnvironmentUtils
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader

open class BaseScript

object ScriptWithClasspathCompilationConfiguration: ScriptCompilationConfiguration({
    defaultImports(Environment::class, DependsOn::class, Script::class)
    jvm {
        updateClasspath(getClasspath())
        jvmTarget("25")
    }
    refineConfiguration {
        onAnnotations(Environment::class, handler = ::configureEnvironment)
    }
    // We need this so that everything is loaded with the KnotClassLoader
    baseClass(BaseScript::class)
}) {
    private fun readResolve(): Any = ScriptWithClasspathCompilationConfiguration
}

private fun getClasspath(): List<File>? {
    return classpathFromClassloader(BaseScript::class.java.classLoader)
}

private fun configureEnvironment(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    var result: ResultWithDiagnostics<ScriptCompilationConfiguration> = context.compilationConfiguration.asSuccess()
    val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.map { it.annotation }
        ?: return result

    val environment = annotations.filterIsInstance<Environment>().firstOrNull()?.let(EnvironmentWithVersion::parse)
        ?.onFailure { return ResultWithDiagnostics.Failure(it.reports) }
        ?.valueOrThrow()
    val scriptId = annotations.filterIsInstance<Script>().firstOrNull()?.let(ScriptMetadata::parse)
        ?.onFailure { return ResultWithDiagnostics.Failure(it.reports) }
        ?.valueOrThrow()

    if (environment != null) {
        result = result.onSuccess { configuration ->
            configuration.with {
                environment(environment)
            }.asSuccess(EnvironmentUtils.getDiagnosticsForTarget(environment.version))
        }
    }
    if (scriptId != null) {
        result = result.onSuccess { configuration ->
            configuration.with {
                scriptMetadata(scriptId)
            }.asSuccess()
        }
    }

    return result
}
