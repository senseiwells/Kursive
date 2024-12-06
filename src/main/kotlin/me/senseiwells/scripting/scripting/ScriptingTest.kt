package me.senseiwells.scripting.scripting

import me.senseiwells.scripting.EssentialScripting
import me.senseiwells.scripting.EssentialScriptingConfig
import me.senseiwells.scripting.utils.ScriptRemappingUtils
import net.fabricmc.loader.api.FabricLoader
import java.net.URLClassLoader
import kotlin.io.path.createParentDirectories
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ScriptWithClasspathComplicationConfiguration: ScriptCompilationConfiguration({
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
        updateClasspath(listOf(ScriptRemappingUtils.getMojangClientJar()!!.toFile()))
    }
})

class MyEvalConfig: ScriptEvaluationConfiguration({
    hostConfiguration
})

object ScriptingTest {
    fun runScript(script: String) {
        val path = EssentialScriptingConfig.resolve("compiled")
            .resolve("output.jar").createParentDirectories()
        val report = BasicJvmScriptingHost(
            evaluator = RemappedJvmScriptJarGenerator(path)
        ).eval(
            script.toScriptSource("SavedScript.kts"),
            ScriptWithClasspathComplicationConfiguration(),
            null
        )
        for (diag in report.reports) {
            EssentialScripting.logger.info(diag.render())
        }
        try {
            val loader = URLClassLoader(arrayOf(path.toUri().toURL()), this::class.java.classLoader)
            val clazz = loader.loadClass("SavedScript")
            clazz.getDeclaredConstructor().newInstance()
        } catch (e: Throwable) {
            EssentialScripting.logger.error("Error:", e)
        }
    }
}