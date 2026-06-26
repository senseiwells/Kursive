package me.senseiwells.kursive.server

import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.Kursive.CommonCommandHandler
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.instance.ScriptInstances
import me.senseiwells.kursive.server.script.ServerExecutionEnvironment
import net.casual.arcade.commands.registerLiteral
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Path

object KursiveServer: ModInitializer, CommonCommandHandler<MinecraftServer, CommandSourceStack> {
    override val scripts = ScriptInstances(this::findScriptDefinitions)

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.registerLiteral("kursive-server") {
                Kursive.registerCommonCommands(this, KursiveServer)
            }
        }
    }

    override fun environment(source: CommandSourceStack, args: List<String>): ExecutionEnvironment<MinecraftServer, *> {
        return ServerExecutionEnvironment(source.server, args)
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

    private fun findScriptDefinitions(server: MinecraftServer): Collection<ScriptDefinition<MinecraftServer>> {
        return Kursive.findScriptDefinitions(this.directory(server).resolve("scripts"))
    }
}