package me.senseiwells.scripting.server.script

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.senseiwells.scripting.api.ScriptCommands
import net.casual.arcade.events.ListenerRegistry
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.server.block.CommandBlockExecuteEvent
import net.casual.arcade.events.server.player.PlayerCommandEvent
import net.casual.arcade.events.server.player.PlayerCommandSuggestionsEvent
import net.casual.arcade.events.server.player.PlayerSendCommandsEvent
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.MinecraftServer

class ServerScriptCommands(
    private val server: MinecraftServer
): ScriptCommands<CommandSourceStack> {
    private val dispatcher = CommandDispatcher<CommandSourceStack>()

    fun initialize(events: ListenerRegistry) {
        events.register<PlayerSendCommandsEvent>(::onPlayerSendCommands)
        events.register<PlayerCommandEvent>(::onPlayerCommand)
        events.register<CommandBlockExecuteEvent>(::onCommandBlockExecute)
        events.register<PlayerCommandSuggestionsEvent>(::onPlayerCommandSuggestions)
    }

    fun cleanup() {
        this.resend()
    }

    override fun register(command: LiteralArgumentBuilder<CommandSourceStack>) {
        this.dispatcher.register(command)
        this.resend()
    }

    private fun onPlayerSendCommands(event: PlayerSendCommandsEvent) {
        event.addCustomCommandNode(this.dispatcher.root)
    }

    private fun onPlayerCommand(event: PlayerCommandEvent) {
        val source = event.player.createCommandSourceStack()
        val result = this.dispatcher.parse(event.command, source)
        if (!result.reader.canRead()) {
            source.server.commands.performCommand(result, event.command)
            event.cancel()
        }
    }

    private fun onCommandBlockExecute(event: CommandBlockExecuteEvent) {
        val result = this.dispatcher.parse(event.command, event.source)
        if (!result.reader.canRead()) {
            event.source.server.commands.performCommand(result, event.command)
            event.cancel()
        }
    }

    private fun onPlayerCommandSuggestions(event: PlayerCommandSuggestionsEvent) {
        val result = this.dispatcher.parse(event.createCommandReader(), event.player.createCommandSourceStack())
        event.addSuggestions(this.dispatcher.getCompletionSuggestions(result))
    }

    private fun resend() {
        for (player in this.server.playerList.players) {
            this.server.commands.sendCommands(player)
        }
    }
}