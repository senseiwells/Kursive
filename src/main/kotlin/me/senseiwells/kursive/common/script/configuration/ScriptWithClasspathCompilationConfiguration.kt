package me.senseiwells.kursive.common.script.configuration

import me.senseiwells.kursive.annotation.Environment
import me.senseiwells.kursive.annotation.Script
import me.senseiwells.kursive.common.utils.EnvironmentUtils
import me.senseiwells.kursive.common.utils.ScriptConfigurationUtils
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
import kotlin.script.experimental.util.filterByAnnotationType

open class BaseScript

object ScriptWithClasspathCompilationConfiguration: ScriptCompilationConfiguration({
    defaultImports(Environment::class, DependsOn::class, Script::class)
    jvm {
        jvmTarget("25")
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
    refineConfiguration {
        onAnnotations(Environment::class, handler = ::configureEnvironment)
        onAnnotations(Script::class, handler = ::configureScript)
    }
    // We need this so that everything is loaded with the KnotClassLoader
    hostConfiguration(ScriptConfigurationUtils.HOST_CONFIGURATION)
    baseClass(BaseScript::class)
}) {
    private fun readResolve(): Any = ScriptWithClasspathCompilationConfiguration
}

private fun configureScript(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val (annotation) = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
        ?.filterByAnnotationType<Script>()
        ?.firstOrNull()
        ?: return context.compilationConfiguration.asSuccess()
    val metadata = ScriptMetadata.parse(annotation)
        .onFailure { return ResultWithDiagnostics.Failure(it.reports) }
        .valueOrThrow()
    return context.compilationConfiguration.with {
        scriptMetadata(metadata)
    }.asSuccess()
}

private fun configureEnvironment(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val (annotation) = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
        ?.filterByAnnotationType<Environment>()
        ?.firstOrNull()
        ?: return context.compilationConfiguration.asSuccess()
    val environment = EnvironmentWithVersion.parse(annotation)
        .onFailure { return ResultWithDiagnostics.Failure(it.reports) }
        .valueOrThrow()
    return context.compilationConfiguration.with {
        environment(environment)
    }.asSuccess(EnvironmentUtils.getDiagnosticsForTarget(environment.version))
}
