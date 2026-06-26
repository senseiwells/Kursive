package me.senseiwells.kursive.common

import com.mojang.brigadier.Command
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import me.senseiwells.kursive.common.script.definition.FileScriptDefinition
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.instance.ScriptInstances
import me.senseiwells.kursive.common.utils.compileAndExecute
import net.casual.arcade.commands.argument
import net.casual.arcade.commands.literal
import net.casual.arcade.utils.component.bold
import net.casual.arcade.utils.component.crimson
import net.casual.arcade.utils.component.lime
import net.casual.arcade.utils.component.white
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.name
import kotlin.io.path.walk
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.jvm.util.isError

object Kursive: ModInitializer {
    const val MOD_ID = "kursive"

    val logger: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {

    }

    fun configDirectory(): Path {
        return FabricLoader.getInstance().configDir.resolve(MOD_ID).createDirectories()
    }

    fun logDiagnostics(result: ResultWithDiagnostics<*>) {
        for (report in result.reports) {
            when (report.severity) {
                ScriptDiagnostic.Severity.WARNING -> this.logger.warn(report.render(withSeverity = false))
                ScriptDiagnostic.Severity.ERROR -> this.logger.error(report.render(withSeverity = false))
                else -> { }
            }
        }
    }

    fun <M: Any> findScriptDefinitions(origin: Path): Collection<ScriptDefinition<M>> {
        try {
            val compiled = origin.resolve(".compiled").createDirectories()
            return origin.createDirectories().walk().filter { it.name.endsWith(".main.kts") }.map {
                FileScriptDefinition.of<M>(it, origin, compiled)
            }.toList()
        } catch (e: Exception) {
            logger.error("Failed to find scripts", e)
            return emptyList()
        }
    }

    fun <M: Any, S> registerCommonCommands(
        node: LiteralArgumentBuilder<S>,
        handler: CommonCommandHandler<M, S>
    ) {
        val suggester = SuggestionProvider { context, builder ->
            handler.scripts.suggestions(handler.minecraft(context.source), builder)
        }
        node.literal("start") {
            argument("name", StringArgumentType.string()) {
                suggests(suggester)
                executes { startScript(it, handler, listOf()) }
                argument("args", StringArgumentType.greedyString()) {
                    executes { startScript(it, handler) }
                }
            }
        }
        node.literal("stop") {
            argument("name", StringArgumentType.string()) {
                suggests(suggester)
                executes { stopScript(it, handler) }
            }
        }
    }

    private fun <M: Any, S> startScript(
        context: CommandContext<S>,
        handler: CommonCommandHandler<M, S>,
        args: List<String> = this.parseArgs(StringArgumentType.getString(context, "args"))
    ): Int {
        val name = StringArgumentType.getString(context, "name")
        val instance = handler.scripts.find(name)
        val source = context.source
        if (instance == null) {
            handler.failure(source, Component.translatable("kursive.command.noScriptWithThatName"))
            return 0
        }
        if (instance.isRunning()) {
            handler.failure(source, Component.translatable("kursive.command.scriptAlreadyStarted"))
            return 0
        }
        val environment = handler.environment(source, args)
        environment.launch {
            val result = instance.compileAndExecute(environment)
            onScriptResult(source, handler, name, result)
        }
        return Command.SINGLE_SUCCESS
    }

    private fun <M: Any, S> stopScript(context: CommandContext<S>, handler: CommonCommandHandler<M, S>): Int {
        val name = StringArgumentType.getString(context, "name")
        val instance = handler.scripts.find(name)
        val source = context.source
        if (instance == null) {
            handler.failure(source, Component.translatable("kursive.command.noScriptWithThatName"))
            return 0
        }
        if (!instance.cancel()) {
            handler.failure(source, Component.translatable("kursive.command.scriptAlreadyStopped"))
            return 0
        }
        val formatted = Component.literal(name).white().bold()
        val message = Component.translatable("kursive.command.successfullyStoppedScript", formatted)
        handler.success(source, message.crimson())
        return Command.SINGLE_SUCCESS
    }

    private fun <M: Any, S> onScriptResult(
        source: S,
        handler: CommonCommandHandler<M, S>,
        name: String,
        result: ResultWithDiagnostics<Unit>
    ) {
        logDiagnostics(result)
        if (result.isError()) {
            handler.failure(source, Component.translatable("kursive.command.failedToRunScript"))
            return
        }
        val formatted = Component.literal(name).white().bold()
        val message = Component.translatable("kursive.command.successfullyStartedScript", formatted)
        handler.success(source, message.lime())
    }

    private fun parseArgs(raw: String): List<String> {
        val reader = StringReader(raw)
        val args = ArrayList<String>()

        while (reader.canRead()) {
            reader.skipWhitespace()
            if (reader.canRead()) {
                args.add(reader.readString())
            }
        }

        return args
    }

    interface CommonCommandHandler<M: Any, S> {
        val scripts: ScriptInstances<M>

        fun minecraft(source: S): M

        fun success(source: S, component: Component)

        fun failure(source: S, component: Component)

        fun environment(source: S, args: List<String>): ExecutionEnvironment<M, *>
    }
}