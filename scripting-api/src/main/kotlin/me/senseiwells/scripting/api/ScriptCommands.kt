package me.senseiwells.scripting.api

import com.mojang.brigadier.builder.LiteralArgumentBuilder

interface ScriptCommands<S> {
    fun register(command: LiteralArgumentBuilder<S>)
}