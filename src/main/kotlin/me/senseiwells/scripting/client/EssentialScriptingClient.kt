package me.senseiwells.scripting.client

import me.senseiwells.scripting.client.script.ClientExecutionEnvironment
import me.senseiwells.scripting.common.EssentialScripting
import me.senseiwells.scripting.common.EssentialScripting.CommonCommandHandler
import me.senseiwells.scripting.common.script.definition.ScriptDefinition
import me.senseiwells.scripting.common.script.execution.ExecutionEnvironment
import me.senseiwells.scripting.common.script.instance.ScriptInstances
import net.casual.arcade.commands.registerLiteral
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object EssentialScriptingClient: ClientModInitializer, CommonCommandHandler<Minecraft, FabricClientCommandSource> {
    override val scripts = ScriptInstances<Minecraft> { this.findScriptDefinitions() }

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.registerLiteral("essential-scripting-client") {
                EssentialScripting.registerCommonCommands(this, EssentialScriptingClient)
            }
        }
    }

    override fun environment(source: FabricClientCommandSource): ExecutionEnvironment<Minecraft> {
        return ClientExecutionEnvironment(source.client, listOf())
    }

    override fun failure(source: FabricClientCommandSource, component: Component) {
        source.sendError(component)
    }

    override fun success(source: FabricClientCommandSource, component: Component) {
        source.sendFeedback(component)
    }

    override fun minecraft(source: FabricClientCommandSource): Minecraft {
        return source.client
    }

    private fun findScriptDefinitions(): Collection<ScriptDefinition<Minecraft>> {
        val origin = FabricLoader.getInstance().gameDir.resolve("essential-scripts")
        return EssentialScripting.findScriptDefinitions(origin)
    }
}