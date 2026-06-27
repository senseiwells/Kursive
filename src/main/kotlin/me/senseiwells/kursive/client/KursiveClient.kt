package me.senseiwells.kursive.client

import me.senseiwells.kursive.client.script.ClientExecutionEnvironment
import me.senseiwells.kursive.client.utils.ClientCommandSource
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.Kursive.CommonCommandHandler
import me.senseiwells.kursive.common.script.definition.resolver.FileScriptDefinitionSource
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.instance.ScriptInstances
import net.casual.arcade.commands.registerLiteral
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.nio.file.Path

object KursiveClient: ClientModInitializer, CommonCommandHandler<Minecraft, ClientCommandSource> {
    override val scripts = ScriptInstances<Minecraft>(FileScriptDefinitionSource {
        this.scriptsDirectory()
    })

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.registerLiteral("kursive-client") {
                Kursive.registerCommonCommands(this, KursiveClient)
            }
        }

        ClientLifecycleEvents.CLIENT_STARTED.register(::onClientStart)
        ClientTickEvents.END_CLIENT_TICK.register(::onClientTick)
        ClientLifecycleEvents.CLIENT_STOPPING.register(::onClientStop)
    }

    override fun environment(minecraft: Minecraft, args: List<String>): ExecutionEnvironment<Minecraft, *> {
        return ClientExecutionEnvironment(minecraft, args)
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

    fun directory(): Path {
        return FabricLoader.getInstance().gameDir.resolve(Kursive.MOD_ID)
    }

    fun scriptsDirectory(): Path {
        return this.directory().resolve("scripts")
    }

    private fun onClientStart(minecraft: Minecraft) {
        this.scripts.initialize(minecraft)
    }

    private fun onClientTick(@Suppress("Unused") minecraft: Minecraft) {
        this.scripts.update()
    }

    private fun onClientStop(@Suppress("Unused") minecraft: Minecraft) {
        this.scripts.close()
    }
}