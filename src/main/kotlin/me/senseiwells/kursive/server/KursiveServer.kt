package me.senseiwells.kursive.server

import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.Kursive.CommonCommandHandler
import me.senseiwells.kursive.common.script.definition.resolver.FileScriptDefinitionSource
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.instance.ScriptInstances
import me.senseiwells.kursive.server.script.ServerExecutionEnvironment
import me.senseiwells.kursive.server.sync.ServerRemoteScriptsManager
import net.casual.arcade.commands.registerLiteral
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.server.ServerStartEvent
import net.casual.arcade.events.server.ServerStopEvent
import net.casual.arcade.events.server.ServerTickEvent
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Path

object KursiveServer: ModInitializer, CommonCommandHandler<MinecraftServer, CommandSourceStack> {
    override lateinit var scripts: ScriptInstances<MinecraftServer>

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.registerLiteral("kursive-server") {
                Kursive.registerCommonCommands(this, KursiveServer)
            }
        }

        GlobalEventHandler.Server.register<ServerStartEvent>(::onServerStart)
        GlobalEventHandler.Server.register<ServerTickEvent>(::onServerTick)
        GlobalEventHandler.Server.register<ServerStopEvent>(::onServerStop)

        ServerRemoteScriptsManager.registerEvents()
    }

    override fun environment(minecraft: MinecraftServer, args: List<String>): ExecutionEnvironment<MinecraftServer, *> {
        return ServerExecutionEnvironment(minecraft, args)
    }

    override fun failure(source: CommandSourceStack, component: Component) {
        source.sendFailure(component)
    }

    override fun success(source: CommandSourceStack, component: Component) {
        source.sendSuccess({ component }, false)
    }

    override fun minecraft(source: CommandSourceStack): MinecraftServer {
        return source.server
    }

    fun directory(server: MinecraftServer): Path {
        return server.getWorldPath(LevelResource.ROOT).resolve(Kursive.MOD_ID)
    }

    fun scriptsDirectory(server: MinecraftServer): Path {
        return this.directory(server).resolve("scripts")
    }

    private fun onServerStart(event: ServerStartEvent) {
        val server = event.server
        this.scripts = ScriptInstances(
            FileScriptDefinitionSource(this.scriptsDirectory(server))
        ) { script -> ServerRemoteScriptsManager.synchronize(server, script) }

        this.scripts.initialize()
    }

    private fun onServerTick(@Suppress("Unused") event: ServerTickEvent) {
        this.scripts.update()
    }

    private fun onServerStop(@Suppress("Unused") event: ServerStopEvent) {
        this.scripts.close()
    }
}