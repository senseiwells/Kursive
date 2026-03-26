package me.senseiwells.scripting.common.script.configuration

import me.senseiwells.scripting.annotation.Environment
import me.senseiwells.scripting.common.utils.EnvironmentUtils
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.experimental.jvm.util.isError

open class BaseScript

object ScriptWithClasspathCompilationConfiguration: ScriptCompilationConfiguration({
    defaultImports(Environment::class, DependsOn::class)
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
