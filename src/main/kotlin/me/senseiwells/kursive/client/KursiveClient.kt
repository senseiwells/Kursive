package me.senseiwells.kursive.client

import me.senseiwells.kursive.client.config.KursiveClientConfig
import me.senseiwells.kursive.client.config.KursiveKeybinds
import me.senseiwells.kursive.client.script.ClientExecutionEnvironment
import me.senseiwells.kursive.client.sync.ClientRemoteScriptsManager
import me.senseiwells.kursive.client.utils.ClientCommandSource
import me.senseiwells.kursive.common.Kursive
import me.senseiwells.kursive.common.Kursive.CommonCommandHandler
import me.senseiwells.kursive.common.script.definition.resolver.PolledFileScriptDefinitionSource
import me.senseiwells.kursive.common.script.execution.ExecutionEnvironment
import me.senseiwells.kursive.common.script.instance.ScriptInstances
import me.senseiwells.kursive.common.utils.ScriptFileUtils
import net.casual.arcade.commands.registerLiteral
import net.casual.arcade.utils.coroutine.launch
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.nio.file.Path
import kotlin.io.path.exists

object KursiveClient: ClientModInitializer, CommonCommandHandler<Minecraft, ClientCommandSource> {
    override val scripts = ScriptInstances<Minecraft>(PolledFileScriptDefinitionSource(this.scriptsDirectory()))

    override fun onInitializeClient() {
        this.checkUserHasYACLInstalled()

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.registerLiteral("kursive-client") {
                Kursive.registerCommonCommands(this, KursiveClient)
            }
        }

        ClientLifecycleEvents.CLIENT_STARTED.register(::onClientStart)
        ClientTickEvents.END_CLIENT_TICK.register(::onClientTick)
        ClientLifecycleEvents.CLIENT_STOPPING.register(::onClientStop)

        ClientRemoteScriptsManager.registerEvents()

        KursiveClientConfig.load()
        KursiveKeybinds.load()

        this.scripts.initialize(true)
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
        return this.directory().resolve(ScriptFileUtils.SCRIPTS_DIRECTORY)
    }

    fun doesScriptExist(name: String): Boolean {
        return this.scriptsDirectory().resolve(name).exists()
    }

    fun remoteScriptsDirectory(): Path {
        return this.directory().resolve("sync").resolve(ScriptFileUtils.SCRIPTS_DIRECTORY)
    }

    fun doesRemoteScriptExist(name: String): Boolean {
        return this.remoteScriptsDirectory().resolve(name).exists()
    }

    private fun onClientStart(minecraft: Minecraft) {
        minecraft.launch {
            scripts.start(environment(minecraft))
        }
    }

    private fun onClientTick(@Suppress("Unused") minecraft: Minecraft) {
        this.scripts.update()
    }

    private fun onClientStop(@Suppress("Unused") minecraft: Minecraft) {
        this.scripts.close()
    }

    private fun checkUserHasYACLInstalled() {
        if (!FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            val message = "You need to install YACL in order to use Kursive"
            Kursive.logger.error(message)
            throw IllegalStateException(message)
        }
    }
}