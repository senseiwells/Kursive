package me.senseiwells.scripting.api

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.casual.arcade.commands.CommandTree
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack

interface ScriptCommands<S> {
    fun register(command: LiteralArgumentBuilder<S>)

    fun dispatcher(): CommandDispatcher<S>

    fun context(): CommandBuildContext

    companion object {
        fun ScriptCommands<CommandSourceStack>.register(tree: CommandTree) {
            tree.register(this.dispatcher(), this.context())
        }
    }
}