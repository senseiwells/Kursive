package me.senseiwells.scripting.script.execution

import me.senseiwells.scripting.EssentialScripting
import me.senseiwells.scripting.EssentialScriptingConfig
import me.senseiwells.scripting.script.configuration.ScriptWithClasspathCompilationConfiguration
import me.senseiwells.scripting.script.execution.ScriptExecutor.Entrypoint
import me.senseiwells.scripting.script.remapping.RemappedJvmScriptJarGenerator
import net.minecraft.Util
import net.minecraft.client.Minecraft
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createParentDirectories
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object ScriptExecutor {
    fun runScript(code: String, name: String) {
        CompletableFuture.supplyAsync({
            this.compileAndLoad(code, name)
        }, Util.ioPool()).handleAsync({ entrypoint, throwable ->
            // TODO: Clean this up
            if (entrypoint != null) {
                try {
                    entrypoint.invoke(arrayOf())
                } catch (e: Throwable) {
                    EssentialScripting.logger.error("Exception during execution", e)
                }
            } else {
                EssentialScripting.logger.error("Failed to run script", throwable)
            }
        }, Minecraft.getInstance())
    }

    private fun compileAndLoad(code: String, name: String): Entrypoint? {
        val output = EssentialScriptingConfig.resolve("compiled")
            .resolve("${name}.jar").createParentDirectories()

        val host = BasicJvmScriptingHost(evaluator = RemappedJvmScriptJarGenerator(output))

        val report = host.eval(
            code.toScriptSource("__EssentialScript"),
            ScriptWithClasspathCompilationConfiguration(),
            ScriptEvaluationConfiguration()
        )
        // TODO: propagate errors properly
        if (report.isError()) {
            for (diag in report.reports) {
                EssentialScripting.logger.info(diag.render())
            }
            return null
        }

        val loader = URLClassLoader(arrayOf(output.toUri().toURL()), this::class.java.classLoader)
        val clazz = loader.loadClass("__EssentialScript")
        val entrypoint = findEntrypoint(clazz)
        return entrypoint
    }

    private fun findEntrypoint(clazz: Class<*>): Entrypoint? {
        try {
            val main = clazz.getDeclaredMethod("main", Array<String>::class.java)
            if (Modifier.isStatic(main.modifiers)) {
                return Entrypoint { args -> main.invoke(null, args) }
            }
            val instance = clazz.getDeclaredConstructor().newInstance()
            return Entrypoint { args -> main.invoke(instance, args) }
        } catch (_: NoSuchMethodException) {

        }
        try {
            val main = clazz.getDeclaredMethod("main")
            // This should never be static, but let's account for it anyway
            if (Modifier.isStatic(main.modifiers)) {
                return Entrypoint { _ -> main.invoke(null) }
            }
            val instance = clazz.getDeclaredConstructor().newInstance()
            return Entrypoint { _ -> main.invoke(instance) }
        } catch (_: NoSuchMethodException) {

        }
        return null
    }

    private fun interface Entrypoint {
        fun invoke(args: Array<String>)
    }
}