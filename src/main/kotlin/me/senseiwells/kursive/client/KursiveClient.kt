package me.senseiwells.kursive.client

import me.senseiwells.kursive.client.script.ClientExecutionEnvironment
import me.senseiwells.kursive.client.utils.ClientCommandSource
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.Kursive.CommonCommandHandler
import me.senseiwells.kursive.common.script.definition.ScriptDefinition
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.instance.ScriptInstances
import net.casual.arcade.commands.registerLiteral
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object KursiveClient: ClientModInitializer, CommonCommandHandler<Minecraft, ClientCommandSource> {
    override val scripts = ScriptInstances<Minecraft> { this.findScriptDefinitions() }

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.registerLiteral("kursive-client") {
                Kursive.registerCommonCommands(this, KursiveClient)
            }
        }
    }

    override fun environment(source: ClientCommandSource): ExecutionEnvironment<Minecraft> {
        return ClientExecutionEnvironment(source.client, listOf())
    }

    override fun failure(source: ClientCommandSource, component: Component) {
        source.sendError(component)
    }

    override fun success(source: ClientCommandSource, component: Component) {
        source.sendFeedback(component)
    }

    override fun minecraft(source: ClientCommandSource): Minecraft {
        return source.client
    }

    private fun findScriptDefinitions(): Collection<ScriptDefinition<Minecraft>> {
        val origin = FabricLoader.getInstance().gameDir.resolve(Kursive.MOD_ID).resolve("scripts")
        return Kursive.findScriptDefinitions(origin)
    }
}