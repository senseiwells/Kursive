package me.senseiwells.scripting.server

import me.senseiwells.scripting.common.EssentialScripting
import me.senseiwells.scripting.common.EssentialScripting.CommonCommandHandler
import me.senseiwells.scripting.common.script.definition.ScriptDefinition
import me.senseiwells.scripting.common.script.execution.ExecutionEnvironment
import me.senseiwells.scripting.common.script.instance.ScriptInstances
import me.senseiwells.scripting.server.script.ServerExecutionEnvironment
import net.casual.arcade.commands.registerLiteral
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource

object EssentialScriptingServer: ModInitializer, CommonCommandHandler<MinecraftServer, CommandSourceStack> {
    override val scripts = ScriptInstances(this::findScriptDefinitions)

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.registerLiteral("essential-scripting-server") {
                EssentialScripting.registerCommonCommands(this, EssentialScriptingServer)
            }
        }
    }

    override fun environment(source: CommandSourceStack): ExecutionEnvironment<MinecraftServer> {
        return ServerExecutionEnvironment(source.server, listOf())
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

    private fun findScriptDefinitions(server: MinecraftServer): Collection<ScriptDefinition<MinecraftServer>> {
        val origin = server.getWorldPath(LevelResource.ROOT).resolve("essential-scripts")
        return EssentialScripting.findScriptDefinitions(origin)
    }
}